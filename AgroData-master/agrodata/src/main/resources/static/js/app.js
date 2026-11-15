    const apiBase = "/api/agrodata";
    const state = { sensors: [], talhoes: [], readings: [], selectedSensor: "all", irrigation: [], fazendas: [], culturas: [] };
    let editingSensorId = null;
    let editingFarmId = null;
    let editingTalhaoId = null;

    const elements = {
        connection: document.getElementById("connection-label"),
        error: document.getElementById("error-banner"),
        actionMessage: document.getElementById("action-message"),
        refresh: document.getElementById("refresh-button"),
        sensorCount: document.getElementById("sensor-count"),
        readingCount: document.getElementById("reading-count"),
        averageHumidity: document.getElementById("average-humidity"),
        latestReading: document.getElementById("latest-reading"),
        latestDate: document.getElementById("latest-date"),
        sensorFilter: document.getElementById("sensor-filter"),
        sensorList: document.getElementById("sensor-list"),
        chart: document.getElementById("chart"),
        readingRows: document.getElementById("reading-rows"),
        tableCount: document.getElementById("table-count"),
        updatedAt: document.getElementById("updated-at"),
        talhaoSelect: document.getElementById("sensor-talhao"),
        talhaoHint: document.getElementById("talhao-hint"),
        createSensorButton: document.getElementById("create-sensor-button"),
    };

    // ===== Autenticação =====
    // Token guardado em sessionStorage (apagado ao fechar a aba). Todo conteúdo dinâmico é
    // inserido via textContent/DOM, o que reduz o risco de XSS expor o token.
    const auth = {
        get token() { try { return sessionStorage.getItem("agrodata.token"); } catch { return null; } },
        get role() { try { return sessionStorage.getItem("agrodata.role"); } catch { return null; } },
        get temp() { try { return sessionStorage.getItem("agrodata.temp") === "1"; } catch { return false; } },
        save(token, role, temp = false) {
            sessionStorage.setItem("agrodata.token", token);
            sessionStorage.setItem("agrodata.role", role);
            sessionStorage.setItem("agrodata.temp", temp ? "1" : "0");
        },
        clear() { ["token", "role", "temp"].forEach(k => sessionStorage.removeItem(`agrodata.${k}`)); }
    };

    function authHeaders(extra = {}) {
        return auth.token ? { ...extra, Authorization: `Bearer ${auth.token}` } : extra;
    }

    function showLogin(message = "") {
        auth.clear();
        document.getElementById("dashboard-shell").hidden = true;
        document.getElementById("dashboard-main").hidden = false;
        document.getElementById("profile-screen").hidden = true;
        document.body.removeAttribute("data-role");
        document.getElementById("logout-button").hidden = true;
        document.getElementById("profile-nav").hidden = true;
        usersState.list = [];
        document.getElementById("user-rows").replaceChildren();
        document.getElementById("admin-edit-form").hidden = true;
        document.getElementById("profile-edit-section").hidden = true;
        document.getElementById("producer-list").replaceChildren();
        document.getElementById("login-form").hidden = false;
        document.getElementById("registration-form").hidden = true;
        document.getElementById("force-form").hidden = true;
        document.getElementById("login-error").textContent = message;
        document.getElementById("login-overlay").classList.add("visible");
        document.getElementById("login-user").focus();
    }

    function showForcePasswordChange() {
        document.getElementById("login-form").hidden = true;
        document.getElementById("force-form").hidden = false;
        document.getElementById("force-error").textContent = "";
        document.getElementById("login-overlay").classList.add("visible");
        document.getElementById("force-current").focus();
    }

    function onAuthenticated() {
        if (auth.temp) { showForcePasswordChange(); return; }
        document.getElementById("dashboard-shell").hidden = false;
        document.body.dataset.role = auth.role || "";
        document.getElementById("logout-button").hidden = false;
        document.getElementById("profile-nav").hidden = false;
        document.getElementById("dashboard-main").hidden = false;
        document.getElementById("profile-screen").hidden = true;
        document.getElementById("login-overlay").classList.remove("visible");
        refreshDashboard();
        loadProfile();
        loadUsers();
        loadProducers();
    }

    function handleUnauthorized(response) {
        if (response.status === 401) {
            showLogin("Sessão expirada. Entre novamente.");
            throw new Error("Sessão expirada.");
        }
        if (response.status === 403) {
            throw new Error("Você não tem permissão para esta ação.");
        }
    }

    async function getJson(path) {
        const response = await fetch(path, { headers: authHeaders({ Accept: "application/json" }) });
        handleUnauthorized(response);
        if (!response.ok) {
            throw new Error(await responseError(response, `Erro HTTP ${response.status} ao consultar ${path}.`));
        }
        return response.json();
    }

    async function responseError(response, fallback) {
        const text = await response.text();
        if (!text) return fallback;
        try {
            const body = JSON.parse(text);
            return body.message || body.detail || body.error || fallback;
        } catch {
            return text;
        }
    }

    async function sendJson(path, method, data) {
        const response = await fetch(path, {
            method,
            headers: authHeaders({ "Content-Type": "application/json", Accept: "application/json" }),
            body: data === undefined ? undefined : JSON.stringify(data)
        });
        handleUnauthorized(response);
        if (!response.ok) {
            throw new Error(await responseError(response, `A API respondeu HTTP ${response.status}.`));
        }
        if (response.status === 204) return null;
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    }

    // ===== Perfil (dados mascarados) e administração de usuários =====
    const ROLE_LABEL = { ADMIN: "Administrador", PRODUTOR: "Produtor", IRRIGACAO: "Sistema de irrigação" };
    const usersState = { list: [], editingId: null };

    async function loadProfile() {
        try {
            const p = await getJson("/api/auth/me");
            const dl = document.getElementById("profile-data");
            dl.replaceChildren();
            const rows = [["Usuário", p.username], ["Perfil", ROLE_LABEL[p.role] || p.role],
                          ["Nome", p.nomeCompleto], ["CPF", p.cpf], ["E-mail", p.email], ["Telefone", p.telefone]];
            for (const [label, value] of rows) {
                const dt = document.createElement("dt");
                dt.textContent = label;
                const dd = document.createElement("dd");
                dd.textContent = value || "não informado";
                dl.append(dt, dd);
            }
            return true;
        } catch (error) {
            if (auth.token) showActionMessage(`Não foi possível carregar o perfil: ${error.message}`, true);
            return false;
        }
    }

    function openProfile() {
        document.getElementById("dashboard-main").hidden = true;
        document.getElementById("profile-screen").hidden = false;
        document.getElementById("profile-nav").hidden = true;
        loadProfile();
    }

    function closeProfile() {
        document.getElementById("profile-screen").hidden = true;
        document.getElementById("profile-edit-section").hidden = true;
        document.getElementById("profile-form").reset();
        document.getElementById("password-form").reset();
        document.getElementById("dashboard-main").hidden = false;
        document.getElementById("profile-nav").hidden = false;
    }

    function startProfileEdit() {
        document.getElementById("profile-form").reset();
        document.getElementById("password-form").reset();
        document.getElementById("profile-edit-section").hidden = false;
        document.getElementById("profile-nome").focus();
    }

    async function loadUsers() {
        if (auth.role !== "ADMIN") return;
        try {
            const list = await getJson("/api/auth/usuarios");
            usersState.list = Array.isArray(list) ? list : [];
            renderUsers();
        } catch (error) {
            if (auth.token) showActionMessage(`Não foi possível carregar os usuários: ${error.message}`, true);
        }
    }

    function renderUsers() {
        const body = document.getElementById("user-rows");
        body.replaceChildren();
        document.getElementById("user-count").textContent = `${usersState.list.length} usuário(s)`;
        for (const user of usersState.list) {
            const row = document.createElement("tr");
            const cells = [user.username, ROLE_LABEL[user.role] || user.role, user.ativo ? "Ativo" : "Desativado",
                           user.nomeCompleto, user.cpf, user.email, user.telefone];
            for (const value of cells) {
                const td = document.createElement("td");
                td.textContent = value || "—";
                row.append(td);
            }
            const actionCell = document.createElement("td");
            const edit = document.createElement("button");
            edit.type = "button";
            edit.className = "edit-button";
            edit.dataset.editUser = String(user.idUsuario);
            edit.textContent = "Editar";
            actionCell.append(edit);
            row.append(actionCell);
            body.append(row);
        }
    }

    const SITUACAO_CURTA = { REGAR: "regar", AGUARDAR_CHUVA: "aguardando chuva", OK: "ok", EXCESSO_UMIDADE: "excesso de água", SEM_DADOS: "sem leitura", SEM_CULTURA: "sem cultura" };

    async function loadProducers() {
        if (auth.role !== "ADMIN") return;
        const list = document.getElementById("producer-list");
        try {
            const data = await getJson("/api/admin/produtores");
            list.replaceChildren();
            const producers = Array.isArray(data) ? data : [];
            document.getElementById("producer-count").textContent = `${producers.length} produtor(es)`;
            if (producers.length === 0) {
                const empty = document.createElement("div");
                empty.className = "sensor-empty";
                empty.textContent = "Nenhum produtor cadastrado.";
                list.append(empty);
                return;
            }
            const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`;
            for (const p of producers) {
                const box = document.createElement("div");
                box.className = "producer";

                const head = document.createElement("div");
                head.className = "producer-head";
                const left = document.createElement("div");
                const name = document.createElement("div");
                name.className = "producer-name";
                name.textContent = p.nome;
                const meta = document.createElement("div");
                meta.className = "producer-meta";
                meta.textContent = [`CPF ${p.cpf}`, ...p.telefones, ...p.emails].join(" · ");
                left.append(name, meta);
                const totals = document.createElement("span");
                totals.className = `badge ${p.talhoesParaRegar > 0 ? "water" : "ok"}`;
                totals.textContent = `${plural(p.totalFazendas, "fazenda", "fazendas")} · ${plural(p.totalTalhoes, "talhão", "talhões")}`
                    + (p.talhoesParaRegar > 0 ? ` · ${p.talhoesParaRegar} para regar` : "");
                head.append(left, totals);
                box.append(head);

                for (const f of p.fazendas) {
                    const farm = document.createElement("div");
                    farm.className = "farm";
                    const title = document.createElement("div");
                    title.className = "farm-title";
                    title.textContent = f.nome;
                    const sub = document.createElement("div");
                    sub.className = "farm-sub";
                    const local = [f.cidade, f.estado].filter(Boolean).join("/");
                    sub.textContent = `${local ? local + " · " : ""}${plural(f.qtdTalhoes, "talhão", "talhões")}`;
                    const chips = document.createElement("div");
                    chips.className = "chips";
                    for (const t of f.talhoes) {
                        const chip = document.createElement("span");
                        chip.className = `chip${t.situacaoIrrigacao === "REGAR" ? " water" : t.situacaoIrrigacao === "EXCESSO_UMIDADE" ? " excess" : ""}`;
                        chip.textContent = `${t.nome}${t.cultura ? " · " + t.cultura : ""} · ${SITUACAO_CURTA[t.situacaoIrrigacao] || t.situacaoIrrigacao}`;
                        chips.append(chip);
                    }
                    farm.append(title, sub, chips);
                    box.append(farm);
                }
                list.append(box);
            }
        } catch (error) {
            if (auth.token) showActionMessage(`Não foi possível carregar os produtores: ${error.message}`, true);
        }
    }

    function openAdminEdit(id) {
        const user = usersState.list.find(u => u.idUsuario === id);
        if (!user) return;
        usersState.editingId = id;
        const form = document.getElementById("admin-edit-form");
        document.getElementById("admin-edit-title").textContent = `Editando: ${user.username}`;
        document.getElementById("adm-nome").value = user.nomeCompleto || "";
        document.getElementById("adm-cpf").value = user.cpf || "";
        document.getElementById("adm-telefone").value = user.telefone || "";
        document.getElementById("adm-email").value = user.email || "";
        document.getElementById("adm-role").value = user.role;
        document.getElementById("adm-ativo").value = String(user.ativo);
        document.getElementById("adm-senha").value = "";
        form.hidden = false;
        form.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function closeAdminEdit() {
        usersState.editingId = null;
        document.getElementById("admin-edit-form").hidden = true;
    }

    function showActionMessage(message, isError = false) {
        elements.actionMessage.textContent = message;
        elements.actionMessage.classList.toggle("error", isError);
        elements.actionMessage.classList.add("visible");
    }

    function formatDate(value, options) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "Data inválida";
        return new Intl.DateTimeFormat("pt-BR", options).format(date);
    }

    function formatNumber(value, digits = 1) {
        if (value === null || value === undefined || !Number.isFinite(Number(value))) return "—";
        return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: digits }).format(Number(value));
    }

    function readingsForSelection() {
        const selected = state.selectedSensor;
        return state.readings
            .filter(reading => selected === "all" || String(reading.idSensor) === selected)
            .sort((a, b) => new Date(b.dataHora) - new Date(a.dataHora));
    }

    function renderSensorOptions() {
        const selected = state.selectedSensor;
        elements.sensorFilter.replaceChildren();
        const allOption = document.createElement("option");
        allOption.value = "all";
        allOption.textContent = "Todos os sensores";
        elements.sensorFilter.append(allOption);
        for (const sensor of state.sensors) {
            const option = document.createElement("option");
            option.value = String(sensor.idSensor);
            option.textContent = `Sensor ${sensor.idSensor} · ${sensor.tipoSensor || "Sem tipo"}`;
            elements.sensorFilter.append(option);
        }
        elements.sensorFilter.value = state.sensors.some(sensor => String(sensor.idSensor) === selected) || selected === "all"
            ? selected : "all";
        state.selectedSensor = elements.sensorFilter.value;

    }

    function renderTalhaoOptions() {
        const selected = elements.talhaoSelect.value;
        elements.talhaoSelect.replaceChildren();
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = state.talhoes.length ? "Selecione um talhão" : "Nenhum talhão cadastrado";
        elements.talhaoSelect.append(placeholder);
        for (const talhao of state.talhoes) {
            const option = document.createElement("option");
            option.value = String(talhao.idTalhao);
            option.textContent = `Talhão ${talhao.idTalhao} · ${talhao.nome || "Sem nome"}`;
            elements.talhaoSelect.append(option);
        }
        elements.talhaoSelect.value = state.talhoes.some(talhao => String(talhao.idTalhao) === selected)
            ? selected : "";
        elements.createSensorButton.disabled = state.talhoes.length === 0;
        elements.talhaoHint.textContent = state.talhoes.length
            ? `${state.talhoes.length} talhão(ões) disponível(is).`
            : "Cadastre primeiro uma fazenda e um talhão: o banco exige que todo sensor pertença a um talhão existente.";
    }

    function fillSelect(select, placeholderText, items, valueOf, labelOf) {
        const selected = select.value;
        select.replaceChildren();
        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = placeholderText;
        select.append(placeholder);
        for (const item of items) {
            const option = document.createElement("option");
            option.value = String(valueOf(item));
            option.textContent = labelOf(item);
            select.append(option);
        }
        select.value = items.some(item => String(valueOf(item)) === selected) ? selected : "";
    }

    function renderTalhaoForm() {
        fillSelect(document.getElementById("talhao-fazenda"),
            state.fazendas.length ? "Selecione a fazenda" : "Nenhuma fazenda cadastrada",
            state.fazendas, f => f.idFazenda,
            f => `${f.nome} · ${f.qtdTalhoes} ${f.qtdTalhoes === 1 ? "talhão" : "talhões"}`);
        fillSelect(document.getElementById("talhao-cultura"), "Sem cultura definida",
            state.culturas, c => c.idCultura,
            c => `${c.nome} (ideal ${formatNumber(c.umidadeMinima, 0)}–${formatNumber(c.umidadeMaxima, 0)}%)`);
    }

    function renderFarmList() {
        const list = document.getElementById("fazenda-list");
        list.replaceChildren();
        if (state.fazendas.length === 0) {
            const empty = document.createElement("div");
            empty.className = "record-empty";
            empty.textContent = "Nenhuma fazenda cadastrada.";
            list.append(empty);
            return;
        }

        for (const farm of state.fazendas) {
            const item = document.createElement("div");
            item.className = "record-item";
            const details = document.createElement("div");
            details.className = "record-details";
            const name = document.createElement("div");
            name.className = "record-name";
            name.textContent = farm.nome;
            const subtitle = document.createElement("div");
            subtitle.className = "record-sub";
            subtitle.textContent = `${formatNumber(farm.areaDisponivel)} ha disponíveis de ${formatNumber(farm.areaTotal)} ha · ${farm.qtdTalhoes} ${farm.qtdTalhoes === 1 ? "talhão" : "talhões"}`;
            details.append(name, subtitle);

            const actions = document.createElement("div");
            actions.className = "record-actions";
            const edit = document.createElement("button");
            edit.type = "button";
            edit.className = "edit-button";
            edit.textContent = "Editar";
            edit.setAttribute("aria-label", `Editar fazenda ${farm.nome}`);
            edit.addEventListener("click", () => startFarmEdit(farm.idFazenda));
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "delete-button admin-only";
            remove.textContent = "Apagar";
            remove.setAttribute("aria-label", `Apagar fazenda ${farm.nome}`);
            remove.addEventListener("click", () => deleteFarm(farm));
            actions.append(edit, remove);
            item.append(details, actions);
            list.append(item);
        }
    }

    function renderTalhaoList() {
        const list = document.getElementById("talhao-list");
        list.replaceChildren();
        if (state.talhoes.length === 0) {
            const empty = document.createElement("div");
            empty.className = "record-empty";
            empty.textContent = "Nenhum talhão cadastrado.";
            list.append(empty);
            return;
        }

        for (const plot of state.talhoes) {
            const item = document.createElement("div");
            item.className = "record-item";
            const farm = state.fazendas.find(candidate => candidate.idFazenda === plot.idFazenda);
            const details = document.createElement("div");
            details.className = "record-details";
            const name = document.createElement("div");
            name.className = "record-name";
            name.textContent = plot.nome;
            const subtitle = document.createElement("div");
            subtitle.className = "record-sub";
            subtitle.textContent = `${formatNumber(plot.area)} ha · ${farm ? farm.nome : `Fazenda ${plot.idFazenda}`}${plot.tipoSolo ? ` · Solo ${plot.tipoSolo}` : ""}`;
            details.append(name, subtitle);

            const actions = document.createElement("div");
            actions.className = "record-actions";
            const edit = document.createElement("button");
            edit.type = "button";
            edit.className = "edit-button";
            edit.textContent = "Editar";
            edit.setAttribute("aria-label", `Editar talhão ${plot.nome}`);
            edit.addEventListener("click", () => startTalhaoEdit(plot));
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "delete-button admin-only";
            remove.textContent = "Apagar";
            remove.setAttribute("aria-label", `Apagar talhão ${plot.nome}`);
            remove.addEventListener("click", () => deleteTalhao(plot));
            actions.append(edit, remove);
            item.append(details, actions);
            list.append(item);
        }
    }

    async function startFarmEdit(idFazenda) {
        try {
            const farm = await getJson(`${apiBase}/fazendas/${encodeURIComponent(idFazenda)}`);
            editingFarmId = farm.idFazenda;
            document.getElementById("fazenda-nome").value = farm.nome || "";
            document.getElementById("fazenda-area").value = farm.areaTotal;
            document.getElementById("fazenda-rua").value = farm.rua || "";
            document.getElementById("fazenda-numero").value = farm.numero || "";
            document.getElementById("fazenda-bairro").value = farm.bairro || "";
            const cepDigits = String(farm.cep || "").replace(/\D/g, "");
            document.getElementById("fazenda-cep").value = cepDigits.length > 5
                ? `${cepDigits.slice(0, 5)}-${cepDigits.slice(5)}`
                : cepDigits;
            document.getElementById("fazenda-cidade").value = farm.cidade || "";
            document.getElementById("fazenda-estado").value = farm.estado || "";
            document.getElementById("fazenda-latitude").value = farm.latitude ?? "";
            document.getElementById("fazenda-longitude").value = farm.longitude ?? "";
            document.getElementById("fazenda-form-title").textContent = "Editar fazenda";
            document.getElementById("fazenda-form-description").textContent = "Atualize os dados da fazenda e sua localização.";
            document.getElementById("create-fazenda-button").textContent = "Salvar alterações";
            document.getElementById("cancel-fazenda-edit-button").hidden = false;
        } catch (error) {
            showActionMessage(`Não foi possível carregar a fazenda para edição: ${error.message}`, true);
        }
    }

    function resetFarmForm() {
        editingFarmId = null;
        document.getElementById("fazenda-form").reset();
        document.getElementById("fazenda-form-title").textContent = "Cadastrar fazenda";
        document.getElementById("fazenda-form-description").textContent = "A fazenda será vinculada automaticamente ao seu perfil.";
        document.getElementById("create-fazenda-button").textContent = "Cadastrar fazenda";
        document.getElementById("cancel-fazenda-edit-button").hidden = true;
    }

    function startTalhaoEdit(plot) {
        editingTalhaoId = plot.idTalhao;
        const farmSelect = document.getElementById("talhao-fazenda");
        farmSelect.value = String(plot.idFazenda);
        farmSelect.disabled = true;
        document.getElementById("talhao-nome").value = plot.nome || "";
        document.getElementById("talhao-area").value = plot.area;
        document.getElementById("talhao-solo").value = plot.tipoSolo || "";
        document.getElementById("talhao-cultura").value = plot.idCultura == null ? "" : String(plot.idCultura);
        document.getElementById("talhao-form-title").textContent = "Editar talhão";
        document.getElementById("talhao-form-description").textContent = "A fazenda não pode ser alterada para preservar os vínculos e históricos do talhão.";
        document.getElementById("create-talhao-button").textContent = "Salvar alterações";
        document.getElementById("cancel-talhao-edit-button").hidden = false;
    }

    function resetTalhaoForm() {
        editingTalhaoId = null;
        document.getElementById("talhao-form").reset();
        document.getElementById("talhao-fazenda").disabled = false;
        document.getElementById("talhao-form-title").textContent = "Cadastrar talhão";
        document.getElementById("talhao-form-description").textContent = "Ao salvar, um sensor é criado e começa a gerar leituras aleatórias automaticamente.";
        document.getElementById("create-talhao-button").textContent = "Cadastrar talhão";
        document.getElementById("cancel-talhao-edit-button").hidden = true;
    }

    async function deleteFarm(farm) {
        if (!window.confirm(`Apagar a fazenda "${farm.nome}"? Todos os talhões precisam ser excluídos antes.`)) return;
        try {
            await sendJson(`${apiBase}/fazendas/${encodeURIComponent(farm.idFazenda)}`, "DELETE");
            showActionMessage(`Fazenda "${farm.nome}" apagada.`);
            await refreshDashboard();
        } catch (error) {
            showActionMessage(`Não foi possível apagar a fazenda: ${error.message}`, true);
        }
    }

    async function deleteTalhao(plot) {
        if (!window.confirm(`Apagar o talhão "${plot.nome}"? Todos os registros vinculados precisam ser excluídos antes.`)) return;
        try {
            await sendJson(`${apiBase}/talhoes/${encodeURIComponent(plot.idTalhao)}`, "DELETE");
            showActionMessage(`Talhão "${plot.nome}" apagado.`);
            await refreshDashboard();
        } catch (error) {
            showActionMessage(`Não foi possível apagar o talhão: ${error.message}`, true);
        }
    }

    function renderSensors() {
        elements.sensorList.replaceChildren();
        if (state.sensors.length === 0) {
            const empty = document.createElement("div");
            empty.className = "sensor-empty";
            empty.textContent = "Nenhum sensor cadastrado. Use o formulário acima para cadastrar um sensor.";
            elements.sensorList.append(empty);
            return;
        }

        for (const sensor of state.sensors) {
            const item = document.createElement("div");
            item.className = "sensor-item";
            const symbol = document.createElement("span");
            symbol.className = "sensor-symbol";
            symbol.setAttribute("aria-hidden", "true");
            symbol.innerHTML = '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v3m0 12v3M5.6 5.6l2.1 2.1m8.6 8.6 2.1 2.1M3 12h3m12 0h3M5.6 18.4l2.1-2.1m8.6-8.6 2.1-2.1" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><circle cx="12" cy="12" r="4" stroke="currentColor" stroke-width="1.7"/></svg>';
            const details = document.createElement("div");
            details.className = "sensor-details";
            const name = document.createElement("div");
            name.className = "sensor-name";
            name.textContent = sensor.tipoSensor || `Sensor ${sensor.idSensor}`;
            const sub = document.createElement("div");
            sub.className = "sensor-sub";
            sub.textContent = `ID ${sensor.idSensor} · Talhão ${sensor.idTalhao ?? "—"}`;
            details.append(name, sub);
            const status = document.createElement("span");
            status.className = `status${sensor.status && sensor.status.toUpperCase() !== "ATIVO" ? " inactive" : ""}`;
            status.textContent = sensor.status || "Cadastrado";
            const actions = document.createElement("div");
            actions.className = "sensor-actions";
            actions.append(status);
            const editButton = document.createElement("button");
            editButton.type = "button";
            editButton.className = "edit-button";
            editButton.dataset.editSensor = String(sensor.idSensor);
            editButton.textContent = "Editar";
            editButton.setAttribute("aria-label", `Editar sensor ${sensor.idSensor}`);
            actions.append(editButton);
            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "delete-button admin-only";
            deleteButton.dataset.deleteSensor = String(sensor.idSensor);
            deleteButton.textContent = "Apagar";
            deleteButton.setAttribute("aria-label", `Apagar sensor ${sensor.idSensor}`);
            actions.append(deleteButton);
            item.append(symbol, details, actions);
            elements.sensorList.append(item);
        }
    }

    function resetSensorForm() {
        document.getElementById("sensor-form").reset();
        for (const select of [document.getElementById("sensor-type"), document.getElementById("sensor-status")]) {
            select.querySelectorAll("option[data-legacy-option]").forEach(option => option.remove());
        }
        elements.talhaoSelect.disabled = state.talhoes.length === 0;
        elements.createSensorButton.textContent = "Cadastrar sensor";
        document.getElementById("sensor-form-title").textContent = "Cadastrar sensor";
        document.getElementById("sensor-form-description").textContent =
            "Escolha um talhão já cadastrado para vincular o sensor.";
        document.getElementById("cancel-sensor-edit-button").hidden = true;
        editingSensorId = null;
    }

    function setSensorEditSelect(select, value) {
        if (value && !Array.from(select.options).some(option => option.value === value)) {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value;
            option.dataset.legacyOption = "true";
            select.add(option);
        }
        select.value = value || "";
    }

    function beginSensorEdit(sensor) {
        resetSensorForm();
        editingSensorId = sensor.idSensor;
        elements.talhaoSelect.value = String(sensor.idTalhao);
        elements.talhaoSelect.disabled = true;
        setSensorEditSelect(document.getElementById("sensor-type"), sensor.tipoSensor);
        setSensorEditSelect(document.getElementById("sensor-status"), sensor.status || "ATIVO");
        document.getElementById("sensor-date").value = sensor.dataInstalacao || "";
        document.getElementById("sensor-form-title").textContent = `Editar sensor ${sensor.idSensor}`;
        document.getElementById("sensor-form-description").textContent =
            "Altere o tipo, a data ou o status. O talhão não pode ser trocado para preservar o histórico de leituras.";
        document.getElementById("cancel-sensor-edit-button").hidden = false;
        elements.createSensorButton.disabled = false;
        elements.createSensorButton.textContent = "Salvar alterações";
        document.getElementById("sensor-type").focus();
        document.getElementById("sensor-form").scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function renderChart(readings) {
        const points = readings
            .filter(item => Number.isFinite(Number(item.umidadeSolo)))
            .slice(0, 12)
            .reverse();

        if (points.length === 0) {
            elements.chart.innerHTML = '<div class="chart-empty">Ainda não há leituras de umidade para exibir.</div>';
            return;
        }

        const width = 720;
        const height = 218;
        const left = 38;
        const right = 12;
        const top = 14;
        const bottom = 28;
        const min = 0;
        const max = 100;
        const xStep = points.length > 1 ? (width - left - right) / (points.length - 1) : 0;
        const coords = points.map((point, index) => {
            const value = Math.max(min, Math.min(max, Number(point.umidadeSolo)));
            return {
                x: points.length > 1 ? left + index * xStep : width / 2,
                y: top + (max - value) / (max - min) * (height - top - bottom),
                value,
                date: point.dataHora
            };
        });
        const line = coords.map(point => `${point.x},${point.y}`).join(" ");
        const area = `${coords[0].x},${height - bottom} ${line} ${coords[coords.length - 1].x},${height - bottom}`;
        const gridValues = [0, 25, 50, 75, 100];
        const grid = gridValues.map(value => {
            const y = top + (max - value) / (max - min) * (height - top - bottom);
            return `<line x1="${left}" y1="${y}" x2="${width - right}" y2="${y}" stroke="#edf0ec" stroke-width="1"/><text x="0" y="${y + 4}" fill="#59695e" font-size="10">${value}%</text>`;
        }).join("");
        const circles = coords.map(point => `<circle cx="${point.x}" cy="${point.y}" r="3.5" fill="#fff" stroke="#43865a" stroke-width="2"><title>${formatNumber(point.value)}% · ${formatDate(point.date, { dateStyle: "short", timeStyle: "short" })}</title></circle>`).join("");
        const firstDate = formatDate(coords[0].date, { day: "2-digit", month: "2-digit" });
        const lastDate = formatDate(coords[coords.length - 1].date, { day: "2-digit", month: "2-digit" });

        elements.chart.innerHTML = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Gráfico de umidade do solo nas últimas ${points.length} leituras" preserveAspectRatio="none">
            <defs><linearGradient id="humidity-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stop-color="#77ad7e" stop-opacity=".24"/><stop offset="100%" stop-color="#77ad7e" stop-opacity=".01"/></linearGradient></defs>
            ${grid}
            <polygon points="${area}" fill="url(#humidity-fill)"/>
            <polyline points="${line}" fill="none" stroke="#43865a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            ${circles}
            <text x="${left}" y="${height - 5}" fill="#59695e" font-size="10">${firstDate}</text>
            <text x="${width - right}" y="${height - 5}" fill="#59695e" font-size="10" text-anchor="end">${lastDate}</text>
        </svg>`;
    }

    function renderReadings(readings) {
        const recent = readings.slice(0, 10);
        elements.readingRows.replaceChildren();
        elements.tableCount.textContent = `${Math.min(readings.length, 10)} de ${readings.length} leitura(s)`;
        if (recent.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 5;
            cell.className = "empty-row";
            cell.textContent = "Aguardando leituras enviadas pelos sensores para este filtro.";
            row.append(cell);
            elements.readingRows.append(row);
            return;
        }

        for (const reading of recent) {
            const row = document.createElement("tr");
            const sensor = document.createElement("td");
            sensor.textContent = `Sensor ${reading.idSensor ?? "—"}`;
            const date = document.createElement("td");
            date.textContent = formatDate(reading.dataHora, { dateStyle: "medium", timeStyle: "short" });
            const humidityCell = document.createElement("td");
            const humidityWrap = document.createElement("div");
            humidityWrap.className = "humidity-cell";
            const humidityValue = Number(reading.umidadeSolo);
            const humidityText = document.createElement("span");
            humidityText.textContent = Number.isFinite(humidityValue) ? `${formatNumber(humidityValue)}%` : "—";
            const track = document.createElement("span");
            track.className = "humidity-track";
            const fill = document.createElement("span");
            fill.className = `humidity-fill${humidityValue < 30 ? " low" : ""}`;
            fill.style.display = "block";
            fill.style.width = `${Number.isFinite(humidityValue) ? Math.max(0, Math.min(100, humidityValue)) : 0}%`;
            track.append(fill);
            humidityWrap.append(humidityText, track);
            humidityCell.append(humidityWrap);
            const temperature = document.createElement("td");
            temperature.textContent = Number.isFinite(Number(reading.temperatura))
                ? `${formatNumber(reading.temperatura)} °C` : "—";
            const actions = document.createElement("td");
            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "delete-button admin-only";
            deleteButton.dataset.deleteReading = String(reading.idLeitura);
            deleteButton.textContent = "Apagar";
            deleteButton.setAttribute("aria-label", `Apagar leitura ${reading.idLeitura}`);
            actions.append(deleteButton);
            row.append(sensor, date, humidityCell, temperature, actions);
            elements.readingRows.append(row);
        }
    }

    const SITUACAO = {
        REGAR: { label: "Precisa regar", css: "water" },
        AGUARDAR_CHUVA: { label: "Aguardar chuva", css: "rain" },
        OK: { label: "Umidade adequada", css: "ok" },
        EXCESSO_UMIDADE: { label: "Excesso de água", css: "excess" },
        SEM_DADOS: { label: "Sem leitura de sensor ativo", css: "" },
        SEM_CULTURA: { label: "Cultura não definida", css: "" }
    };

    function renderIrrigation() {
        const list = document.getElementById("irrigation-list");
        const summary = document.getElementById("irrigation-summary");
        list.replaceChildren();
        const toWater = state.irrigation.filter(item => item.situacao === "REGAR").length;
        const waitingForRain = state.irrigation.filter(item => item.situacao === "AGUARDAR_CHUVA").length;
        const rainMessage = waitingForRain
            ? `${waitingForRain} ${waitingForRain === 1 ? "talhão aguarda" : "talhões aguardam"} chuva`
            : "";
        const irrigationMessage = toWater
            ? `${toWater} ${toWater === 1 ? "talhão precisa" : "talhões precisam"} de irrigação`
            : "";
        summary.textContent = [rainMessage, irrigationMessage].filter(Boolean).join(" · ")
            || "Nenhum talhão precisa de irrigação";
        summary.classList.toggle("alert", toWater > 0);
        summary.classList.toggle("rain", waitingForRain > 0 && toWater === 0);

        if (state.irrigation.length === 0) {
            const empty = document.createElement("div");
            empty.className = "sensor-empty";
            empty.textContent = "Nenhum talhão cadastrado.";
            list.append(empty);
            return;
        }

        state.irrigation.forEach(item => {
            const info = SITUACAO[item.situacao] || { label: item.situacao, css: "" };
            const row = document.createElement("div");
            row.className = `irrigation-item${item.situacao === "REGAR" ? " water" : item.situacao === "AGUARDAR_CHUVA" ? " rain" : ""}`;

            const left = document.createElement("div");
            const name = document.createElement("div");
            name.className = "irrigation-name";
            name.textContent = item.cultura ? `${item.talhao} · ${item.cultura}` : item.talhao;
            const sub = document.createElement("div");
            sub.className = "irrigation-sub";
            if (item.umidadeAtual !== null && item.umidadeAtual !== undefined && item.umidadeMinima !== null) {
                let text = `Umidade ${formatNumber(item.umidadeAtual)}% · ideal ${formatNumber(item.umidadeMinima, 0)}–${formatNumber(item.umidadeMaxima, 0)}%`;
                if (item.dataLeitura) text += ` · leitura de ${formatDate(item.dataLeitura, { dateStyle: "short", timeStyle: "short" })}`;
                if (item.situacao === "AGUARDAR_CHUVA") {
                    text = `Solo seco; irrigação suspensa para aguardar chuva (${formatNumber(item.probabilidadeChuva, 0)}% nas próximas 24 h).`;
                    if (item.comandoStatus === "ENVIADO") text += " Um comando anterior já foi enviado ao sistema.";
                } else {
                    if (item.probabilidadeChuva !== null && item.probabilidadeChuva !== undefined) {
                        text += ` · chuva ${formatNumber(item.probabilidadeChuva, 0)}% nas próximas 24 h`;
                    } else if (item.situacao === "REGAR") {
                        text += " · previsão de chuva indisponível no momento";
                    }
                    if (item.comandoStatus === "ENVIADO") text += " · sistema de irrigação acionado";
                    else if (item.comandoStatus === "PENDENTE") text += " · aguardando o sistema de irrigação";
                }
                sub.textContent = text;
            } else {
                sub.textContent = item.situacao === "SEM_CULTURA"
                    ? "Defina a cultura do talhão para monitorar a irrigação."
                    : "Nenhuma leitura recente de sensor ativo.";
            }
            left.append(name, sub);

            const badge = document.createElement("span");
            badge.className = `badge ${info.css}`.trim();
            badge.textContent = info.label;
            row.append(left, badge);
            list.append(row);
        });
    }

    function renderDashboard() {
        const readings = readingsForSelection();
        const humidities = readings
            .map(reading => Number(reading.umidadeSolo))
            .filter(value => Number.isFinite(value));
        const average = humidities.length
            ? humidities.reduce((sum, value) => sum + value, 0) / humidities.length
            : null;
        const latest = readings[0];

        elements.sensorCount.textContent = state.sensors.length;
        elements.readingCount.textContent = readings.length;
        elements.averageHumidity.textContent = average === null ? "—" : `${formatNumber(average)}%`;
        elements.latestReading.textContent = latest ? formatNumber(latest.umidadeSolo) + "%" : "—";
        elements.latestDate.textContent = latest
            ? `Sensor ${latest.idSensor} · ${formatDate(latest.dataHora, { dateStyle: "medium", timeStyle: "short" })}`
            : "Aguardando leituras";

        renderChart(readings);
        renderReadings(readings);
        renderSensors();
    }

    async function refreshDashboard() {
        elements.refresh.disabled = true;
        elements.error.classList.remove("visible");
        elements.connection.textContent = "Atualizando dados";
        try {
            const [sensors, talhoes, irrigation, fazendas, culturas] = await Promise.all([
                getJson(`${apiBase}/sensores`),
                getJson(`${apiBase}/talhoes`),
                getJson(`${apiBase}/irrigacao/status`),
                getJson(`${apiBase}/fazendas`),
                getJson(`${apiBase}/culturas`)
            ]);
            if (!Array.isArray(irrigation)) throw new Error("A API retornou um formato inesperado para a irrigação.");
            if (!Array.isArray(sensors)) throw new Error("A API retornou um formato inesperado para a lista de sensores.");
            if (!Array.isArray(talhoes)) throw new Error("A API retornou um formato inesperado para a lista de talhões.");
            const sensorReadings = await Promise.all(sensors.map(async sensor => {
                const readings = await getJson(`${apiBase}/leituras/${encodeURIComponent(sensor.idSensor)}`);
                if (!Array.isArray(readings)) throw new Error(`A API retornou um formato inesperado para as leituras do sensor ${sensor.idSensor}.`);
                return readings;
            }));
            state.sensors = sensors;
            state.talhoes = talhoes;
            state.irrigation = irrigation;
            state.fazendas = Array.isArray(fazendas) ? fazendas : [];
            state.culturas = Array.isArray(culturas) ? culturas : [];
            state.readings = sensorReadings.flat();
            renderSensorOptions();
            renderTalhaoOptions();
            renderDashboard();
            renderIrrigation();
            renderTalhaoForm();
            renderFarmList();
            renderTalhaoList();
            elements.connection.textContent = "Dados atualizados";
            elements.updatedAt.textContent = `Atualizado em ${formatDate(new Date(), { dateStyle: "medium", timeStyle: "short" })} · Dados carregados da API AgroData.`;
        } catch (error) {
            elements.connection.textContent = "Falha ao carregar dados";
            elements.error.textContent = `${error.message} Confirme se a aplicação e o banco de dados estão disponíveis e atualize a página.`;
            elements.error.classList.add("visible");
        } finally {
            elements.refresh.disabled = false;
        }
    }

    elements.refresh.addEventListener("click", refreshDashboard);
    document.getElementById("profile-nav").addEventListener("click", openProfile);
    document.getElementById("profile-back").addEventListener("click", closeProfile);
    document.getElementById("profile-refresh").addEventListener("click", async event => {
        const button = event.currentTarget;
        button.disabled = true;
        try {
            if (await loadProfile()) showActionMessage("Dados do perfil atualizados.");
        } finally {
            button.disabled = false;
        }
    });
    document.getElementById("profile-edit-toggle").addEventListener("click", startProfileEdit);
    document.getElementById("profile-cancel").addEventListener("click", () => {
        document.getElementById("profile-edit-section").hidden = true;
        document.getElementById("profile-form").reset();
        document.getElementById("password-form").reset();
    });
    elements.sensorFilter.addEventListener("change", event => {
        state.selectedSensor = event.target.value;
        renderDashboard();
    });

    const fazendaCep = document.getElementById("fazenda-cep");
    fazendaCep.addEventListener("input", () => {
        const digits = fazendaCep.value.replace(/\D/g, "").slice(0, 8);
        fazendaCep.value = digits.length > 5
            ? `${digits.slice(0, 5)}-${digits.slice(5)}`
            : digits;
    });

    document.getElementById("fazenda-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formEl = event.currentTarget;
        const form = new FormData(formEl);
        const button = document.getElementById("create-fazenda-button");
        const farmId = editingFarmId;
        const editing = farmId !== null;
        const normalizeText = value => String(value || "").trim().replace(/\s+/g, " ");
        button.disabled = true;
        try {
            const payload = {
                nome: normalizeText(form.get("nome")),
                areaTotal: Number(form.get("areaTotal")),
                rua: normalizeText(form.get("rua")),
                numero: normalizeText(form.get("numero")),
                bairro: normalizeText(form.get("bairro")),
                cep: String(form.get("cep")).replace(/\D/g, ""),
                cidade: normalizeText(form.get("cidade")),
                estado: normalizeText(form.get("estado")).toUpperCase(),
                latitude: String(form.get("latitude") || "").trim() ? Number(form.get("latitude")) : null,
                longitude: String(form.get("longitude") || "").trim() ? Number(form.get("longitude")) : null
            };
            if (editing) {
                await sendJson(`${apiBase}/fazendas/${encodeURIComponent(farmId)}`, "PUT", payload);
                resetFarmForm();
                showActionMessage("Fazenda atualizada.");
            } else {
                await sendJson(`${apiBase}/fazendas`, "POST", payload);
                resetFarmForm();
                showActionMessage("Fazenda cadastrada e vinculada ao seu perfil.");
            }
            await refreshDashboard();
            loadProducers();
        } catch (error) {
            showActionMessage(`Não foi possível ${editing ? "atualizar" : "cadastrar"} a fazenda: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });
    document.getElementById("cancel-fazenda-edit-button").addEventListener("click", resetFarmForm);

    document.getElementById("sensor-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const button = elements.createSensorButton;
        const sensorId = editingSensorId;
        const editing = sensorId !== null;
        button.disabled = true;
        try {
            const payload = {
                tipoSensor: String(form.get("tipoSensor")).trim(),
                dataInstalacao: form.get("dataInstalacao") || null,
                status: form.get("status")
            };
            if (editing) {
                await sendJson(`${apiBase}/sensores/${encodeURIComponent(sensorId)}`, "PUT", payload);
            } else {
                payload.idTalhao = Number(form.get("idTalhao"));
                await sendJson(`${apiBase}/sensores`, "POST", payload);
            }
            resetSensorForm();
            showActionMessage(editing
                ? `Sensor ${sensorId} atualizado.`
                : "Sensor cadastrado; a geração automática de leituras foi iniciada.");
            await refreshDashboard();
        } catch (error) {
            showActionMessage(`Não foi possível ${editing ? "atualizar" : "cadastrar"} o sensor: ${error.message}`, true);
        } finally {
            button.disabled = state.talhoes.length === 0;
        }
    });

    document.getElementById("cancel-sensor-edit-button").addEventListener("click", resetSensorForm);

    document.getElementById("talhao-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formEl = event.currentTarget;
        const form = new FormData(formEl);
        const button = document.getElementById("create-talhao-button");
        const plotId = editingTalhaoId;
        const editing = plotId !== null;
        button.disabled = true;
        try {
            const payload = {
                nome: String(form.get("nome")).trim(),
                area: Number(form.get("area")),
                tipoSolo: String(form.get("tipoSolo") || "").trim() || null,
                idCultura: form.get("idCultura") ? Number(form.get("idCultura")) : null
            };
            if (editing) {
                await sendJson(`${apiBase}/talhoes/${encodeURIComponent(plotId)}`, "PUT", payload);
                resetTalhaoForm();
                showActionMessage("Talhão atualizado. O histórico e o sensor foram preservados.");
            } else {
                const result = await sendJson(`${apiBase}/talhoes`, "POST", {
                    ...payload,
                    idFazenda: Number(form.get("idFazenda"))
                });
                resetTalhaoForm();
                showActionMessage(`Talhão cadastrado com sensor e ${result.leiturasGeradas} leituras iniciais. A geração automática continuará a cada minuto.`);
            }
            await refreshDashboard();
            loadProducers();
        } catch (error) {
            showActionMessage(`Não foi possível ${editing ? "atualizar" : "cadastrar"} o talhão: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });
    document.getElementById("cancel-talhao-edit-button").addEventListener("click", resetTalhaoForm);

    document.getElementById("user-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formEl = event.currentTarget;
        const form = new FormData(formEl);
        const button = document.getElementById("create-user-button");
        button.disabled = true;
        try {
            const result = await sendJson("/api/auth/usuarios", "POST", {
                username: String(form.get("username")).trim(),
                password: String(form.get("password")),
                role: String(form.get("role")),
                nomeCompleto: String(form.get("nomeCompleto") || "").trim() || null
            });
            formEl.reset();
            showActionMessage(`Usuário ${result.username} (${result.role}) cadastrado.`);
            loadUsers();
        } catch (error) {
            showActionMessage(`Não foi possível cadastrar o usuário: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });

    document.getElementById("profile-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formEl = event.currentTarget;
        const form = new FormData(formEl);
        const payload = {};
        for (const key of ["nomeCompleto", "telefone", "email"]) {
            const value = String(form.get(key) || "").trim();
            if (value) payload[key] = value;
        }
        if (Object.keys(payload).length === 0) {
            showActionMessage("Preencha ao menos um campo para alterar.", true);
            return;
        }
        const button = document.getElementById("profile-button");
        button.disabled = true;
        try {
            await sendJson("/api/auth/me", "PUT", payload);
            formEl.reset();
            document.getElementById("profile-edit-section").hidden = true;
            showActionMessage("Dados atualizados.");
            await loadProfile();
            loadUsers();
        } catch (error) {
            showActionMessage(`Não foi possível atualizar o perfil: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });

    document.getElementById("password-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formEl = event.currentTarget;
        const form = new FormData(formEl);
        const button = document.getElementById("password-button");
        button.disabled = true;
        try {
            await sendJson("/api/auth/me/senha", "PUT", {
                senhaAtual: String(form.get("senhaAtual")),
                novaSenha: String(form.get("novaSenha"))
            });
            formEl.reset();
            showActionMessage("Senha alterada com sucesso.");
        } catch (error) {
            showActionMessage(`Não foi possível alterar a senha: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });

    document.getElementById("user-rows").addEventListener("click", event => {
        const button = event.target.closest("button[data-edit-user]");
        if (button) openAdminEdit(Number(button.dataset.editUser));
    });
    document.getElementById("producer-refresh").addEventListener("click", loadProducers);
    document.getElementById("admin-edit-cancel").addEventListener("click", closeAdminEdit);

    document.getElementById("admin-edit-form").addEventListener("submit", async event => {
        event.preventDefault();
        if (usersState.editingId === null) return;
        const form = new FormData(event.currentTarget);
        const payload = { role: String(form.get("role")), ativo: form.get("ativo") === "true" };
        for (const key of ["nomeCompleto", "cpf", "telefone", "email", "novaSenha"]) {
            const value = String(form.get(key) || "").trim();
            if (value) payload[key] = value;
        }
        const button = document.getElementById("admin-edit-save");
        button.disabled = true;
        try {
            const updated = await sendJson(`/api/auth/usuarios/${encodeURIComponent(usersState.editingId)}`, "PUT", payload);
            showActionMessage(`Usuário ${updated.username} atualizado.`);
            closeAdminEdit();
            await loadUsers();
            loadProfile();
        } catch (error) {
            showActionMessage(`Não foi possível salvar o usuário: ${error.message}`, true);
        } finally {
            button.disabled = false;
        }
    });

    elements.sensorList.addEventListener("click", async event => {
        const editButton = event.target.closest("[data-edit-sensor]");
        if (editButton) {
            const sensor = state.sensors.find(item => String(item.idSensor) === editButton.dataset.editSensor);
            if (sensor) beginSensorEdit(sensor);
            return;
        }
        const button = event.target.closest("[data-delete-sensor]");
        if (!button) return;
        const idSensor = button.dataset.deleteSensor;
        if (!confirm(`Apagar o sensor ${idSensor}? Sensores com leituras não podem ser apagados.`)) return;
        button.disabled = true;
        try {
            await sendJson(`${apiBase}/sensores/${encodeURIComponent(idSensor)}`, "DELETE");
            showActionMessage(`Sensor ${idSensor} apagado.`);
            await refreshDashboard();
        } catch (error) {
            const message = error.message.includes("409")
                ? `O sensor ${idSensor} não foi apagado porque possui leituras. Apague as leituras primeiro.`
                : `Não foi possível apagar o sensor ${idSensor}: ${error.message}`;
            showActionMessage(message, true);
            button.disabled = false;
        }
    });

    elements.readingRows.addEventListener("click", async event => {
        const button = event.target.closest("[data-delete-reading]");
        if (!button) return;
        const idLeitura = button.dataset.deleteReading;
        if (!confirm(`Apagar a leitura ${idLeitura}? Esta ação não pode ser desfeita.`)) return;
        button.disabled = true;
        try {
            await sendJson(`${apiBase}/leituras/${encodeURIComponent(idLeitura)}`, "DELETE");
            showActionMessage(`Leitura ${idLeitura} apagada.`);
            await refreshDashboard();
        } catch (error) {
            showActionMessage(`Não foi possível apagar a leitura ${idLeitura}: ${error.message}`, true);
            button.disabled = false;
        }
    });

    document.getElementById("login-form").addEventListener("submit", async event => {
        event.preventDefault();
        const button = document.getElementById("login-button");
        const errorBox = document.getElementById("login-error");
        const passField = document.getElementById("login-pass");
        errorBox.textContent = "";
        button.disabled = true;
        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({
                    username: document.getElementById("login-user").value,
                    password: passField.value
                })
            });
            if (!response.ok) {
                errorBox.textContent = await responseError(response, "Não foi possível entrar.");
                return;
            }
            const body = await response.json();
            auth.save(body.accessToken, body.role, body.senhaTemporaria === true);
            passField.value = "";
            onAuthenticated();
        } catch {
            errorBox.textContent = "Falha de conexão com o servidor.";
        } finally {
            button.disabled = false;
        }
    });

    function formatCpf(value) {
        const digits = value.replace(/\D/g, "").slice(0, 11);
        if (digits.length > 9) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
        if (digits.length > 6) return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6)}`;
        if (digits.length > 3) return `${digits.slice(0, 3)}.${digits.slice(3)}`;
        return digits;
    }

    function formatPhone(value) {
        const digits = value.replace(/\D/g, "").slice(0, 11);
        if (digits.length === 0) return "";
        if (digits.length === 1) return `(${digits}`;

        const areaCode = digits.slice(0, 2);
        const number = digits.slice(2);
        if (number.length === 0) return `(${areaCode}) `;

        const prefixLength = digits.length > 10 ? 5 : 4;
        if (number.length <= prefixLength) return `(${areaCode}) ${number}`;
        return `(${areaCode}) ${number.slice(0, prefixLength)}-${number.slice(prefixLength)}`;
    }

    function applyInputMask(input, formatter) {
        const previousValue = input.value;
        const caret = input.selectionStart === null ? previousValue.length : input.selectionStart;
        const digitsBeforeCaret = previousValue.slice(0, caret).replace(/\D/g, "").length;
        const formattedValue = formatter(previousValue);
        input.value = formattedValue;

        let nextCaret = 0;
        let digitsBeforeNextCaret = 0;
        while (nextCaret < formattedValue.length && digitsBeforeNextCaret < digitsBeforeCaret) {
            if (/\d/.test(formattedValue.charAt(nextCaret))) digitsBeforeNextCaret++;
            nextCaret++;
        }
        while (nextCaret < formattedValue.length && /\D/.test(formattedValue.charAt(nextCaret))) nextCaret++;
        input.setSelectionRange(nextCaret, nextCaret);
    }

    document.getElementById("registration-cpf").addEventListener("input", event => {
        applyInputMask(event.currentTarget, formatCpf);
    });

    document.getElementById("registration-phone").addEventListener("input", event => {
        applyInputMask(event.currentTarget, formatPhone);
    });

    const registrationEmail = document.getElementById("registration-email");
    const registrationPassword = document.getElementById("registration-pass");
    const registrationError = document.getElementById("registration-error");
    const registrationPasswordHint = document.getElementById("registration-password-hint");
    const registrationPasswordRequirements = "Use de 8 a 72 caracteres, uma letra maiúscula, um número e um caractere especial.";

    function getRegistrationEmailError(value) {
        const missingAt = !value.includes("@");
        const missingCom = !/\.com(?:\.br)?$/i.test(value);
        if (missingAt && missingCom) return "E-mail inválido: faltam @ e .com.";
        if (missingAt) return "E-mail inválido: falta @.";
        if (missingCom) return "E-mail inválido: falta .com.";
        return "";
    }

    function updateRegistrationEmailValidity() {
        registrationEmail.setCustomValidity(getRegistrationEmailError(registrationEmail.value.trim()));
    }

    registrationEmail.addEventListener("input", () => {
        updateRegistrationEmailValidity();
        if (registrationError.textContent.startsWith("E-mail inválido:")) registrationError.textContent = "";
    });
    registrationEmail.addEventListener("blur", () => {
        registrationEmail.value = registrationEmail.value.trim().toLowerCase();
        updateRegistrationEmailValidity();
    });
    registrationEmail.addEventListener("invalid", () => {
        updateRegistrationEmailValidity();
        registrationError.textContent = getRegistrationEmailError(registrationEmail.value.trim());
    });

    function getRegistrationPasswordError(value) {
        if (!value) return "";
        const valid = value.length >= 8 && value.length <= 72
            && /\p{Lu}/u.test(value)
            && /\p{N}/u.test(value)
            && /[^\p{L}\p{N}\s]/u.test(value);
        return valid ? "" : "A senha deve ter de 8 a 72 caracteres, uma letra maiúscula, um número e um caractere especial.";
    }

    function updateRegistrationPasswordValidity() {
        const error = getRegistrationPasswordError(registrationPassword.value);
        registrationPassword.setCustomValidity(error);
        registrationPasswordHint.textContent = error || registrationPasswordRequirements;
        registrationPasswordHint.classList.toggle("password-hint-error", Boolean(error));
        return error;
    }

    registrationPassword.addEventListener("input", () => {
        updateRegistrationPasswordValidity();
    });
    registrationPassword.addEventListener("invalid", () => {
        updateRegistrationPasswordValidity();
        const emailError = getRegistrationEmailError(registrationEmail.value.trim());
        if (emailError) {
            registrationError.textContent = emailError;
            return;
        }
    });

    document.querySelectorAll("[data-password-toggle]").forEach(toggle => {
        toggle.addEventListener("click", () => {
            const input = document.getElementById(toggle.dataset.passwordToggle);
            const showPassword = input.type === "password";
            input.type = showPassword ? "text" : "password";
            toggle.textContent = showPassword ? "Ocultar" : "Mostrar";
            toggle.setAttribute("aria-label", showPassword ? "Ocultar senha" : "Mostrar senha");
            toggle.setAttribute("aria-pressed", String(showPassword));
        });
    });

    document.getElementById("show-registration").addEventListener("click", () => {
        document.getElementById("login-form").hidden = true;
        document.getElementById("registration-form").hidden = false;
        document.getElementById("registration-error").textContent = "";
        document.getElementById("registration-user").focus();
    });

    document.getElementById("back-to-login").addEventListener("click", () => {
        document.getElementById("registration-form").hidden = true;
        document.getElementById("login-form").hidden = false;
        document.getElementById("login-user").focus();
    });

    document.getElementById("registration-form").addEventListener("submit", async event => {
        event.preventDefault();
        const button = document.getElementById("registration-button");
        const errorBox = document.getElementById("registration-error");
        const nomeCompleto = document.getElementById("registration-name").value.trim();
        const cpf = document.getElementById("registration-cpf").value.trim();
        const email = document.getElementById("registration-email").value.trim().toLowerCase();
        const telefone = document.getElementById("registration-phone").value.trim();
        const username = document.getElementById("registration-user").value.trim();
        const password = document.getElementById("registration-pass").value;
        const confirmation = document.getElementById("registration-pass-confirm").value;
        errorBox.textContent = "";

        const emailError = getRegistrationEmailError(email);
        if (emailError) {
            errorBox.textContent = emailError;
            registrationEmail.focus();
            return;
        }

        const passwordError = getRegistrationPasswordError(password);
        if (passwordError) {
            updateRegistrationPasswordValidity();
            registrationPassword.focus();
            return;
        }

        if (password !== confirmation) {
            errorBox.textContent = "As senhas não coincidem.";
            return;
        }

        button.disabled = true;
        try {
            const response = await fetch("/api/auth/cadastro", {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({ username, password, nomeCompleto, cpf, email, telefone })
            });
            if (!response.ok) {
                errorBox.textContent = await responseError(response, "Não foi possível criar a conta.");
                return;
            }

            document.getElementById("registration-form").reset();
            document.getElementById("login-user").value = username;
            showLogin("Conta de produtor criada. Entre com sua senha.");
        } catch {
            errorBox.textContent = "Falha de conexão com o servidor.";
        } finally {
            button.disabled = false;
        }
    });

    document.getElementById("logout-button").addEventListener("click", () => showLogin());

    document.getElementById("force-form").addEventListener("submit", async event => {
        event.preventDefault();
        const button = document.getElementById("force-button");
        const errorBox = document.getElementById("force-error");
        errorBox.textContent = "";
        button.disabled = true;
        try {
            const response = await fetch("/api/auth/me/senha", {
                method: "PUT",
                headers: authHeaders({ "Content-Type": "application/json", Accept: "application/json" }),
                body: JSON.stringify({
                    senhaAtual: document.getElementById("force-current").value,
                    novaSenha: document.getElementById("force-new").value
                })
            });
            if (!response.ok) {
                errorBox.textContent = await responseError(response, "Não foi possível trocar a senha.");
                return;
            }
            document.getElementById("force-current").value = "";
            document.getElementById("force-new").value = "";
            showLogin("Senha alterada. Entre novamente com a nova senha.");
        } catch {
            errorBox.textContent = "Falha de conexão com o servidor.";
        } finally {
            button.disabled = false;
        }
    });

    if (auth.token) {
        onAuthenticated();
    } else {
        showLogin();
    }
    window.setInterval(() => {
        if (!document.hidden && auth.token && !auth.temp) refreshDashboard();
    }, 15000);
