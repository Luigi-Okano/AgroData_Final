CREATE OR REPLACE FUNCTION agrodata_normalizar_produtor()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.nome := btrim(regexp_replace(coalesce(NEW.nome, ''), '[[:space:]]+', ' ', 'g'));
    NEW.cpf := regexp_replace(coalesce(NEW.cpf, ''), '[^0-9]', '', 'g');
    IF NEW.nome = '' THEN
        RAISE EXCEPTION 'O nome do produtor e obrigatorio.';
    END IF;
    IF char_length(NEW.cpf) <> 11 THEN
        RAISE EXCEPTION 'O CPF deve conter 11 digitos.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_produtor ON produtor@@
CREATE TRIGGER agrodata_normalizar_produtor
BEFORE INSERT OR UPDATE ON produtor
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_produtor()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_localizacao()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.rua := btrim(regexp_replace(coalesce(NEW.rua, ''), '[[:space:]]+', ' ', 'g'));
    NEW.numero := btrim(regexp_replace(coalesce(NEW.numero, ''), '[[:space:]]+', ' ', 'g'));
    NEW.bairro := btrim(regexp_replace(coalesce(NEW.bairro, ''), '[[:space:]]+', ' ', 'g'));
    NEW.cidade := btrim(regexp_replace(coalesce(NEW.cidade, ''), '[[:space:]]+', ' ', 'g'));
    NEW.cep := regexp_replace(coalesce(NEW.cep, ''), '[^0-9]', '', 'g');
    NEW.estado := upper(btrim(coalesce(NEW.estado, '')));
    IF NEW.rua = '' OR NEW.numero = '' OR NEW.bairro = '' OR NEW.cidade = '' THEN
        RAISE EXCEPTION 'Rua, numero, bairro e cidade sao obrigatorios.';
    END IF;
    IF NEW.cep !~ '^[0-9]{8}$' THEN
        RAISE EXCEPTION 'O CEP deve conter 8 digitos.';
    END IF;
    IF NEW.estado NOT IN ('AC','AL','AM','AP','BA','CE','DF','ES','GO','MA','MG','MS','MT',
                          'PA','PB','PE','PI','PR','RJ','RN','RO','RR','RS','SC','SE','SP','TO') THEN
        RAISE EXCEPTION 'Informe uma sigla de estado brasileira valida.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_localizacao ON localizacao@@
CREATE TRIGGER agrodata_normalizar_localizacao
BEFORE INSERT OR UPDATE ON localizacao
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_localizacao()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_fazenda()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.nome := btrim(regexp_replace(coalesce(NEW.nome, ''), '[[:space:]]+', ' ', 'g'));
    IF NEW.nome = '' THEN
        RAISE EXCEPTION 'O nome da fazenda e obrigatorio.';
    END IF;
    IF NEW.area_total IS NULL OR NEW.area_total <= 0 THEN
        RAISE EXCEPTION 'A area da fazenda deve ser maior que zero.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_fazenda ON fazenda@@
CREATE TRIGGER agrodata_normalizar_fazenda
BEFORE INSERT OR UPDATE ON fazenda
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_fazenda()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_talhao()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.nome := btrim(regexp_replace(coalesce(NEW.nome, ''), '[[:space:]]+', ' ', 'g'));
    IF NEW.tipo_solo IS NOT NULL THEN
        NEW.tipo_solo := nullif(btrim(regexp_replace(NEW.tipo_solo, '[[:space:]]+', ' ', 'g')), '');
    END IF;
    IF NEW.nome = '' THEN
        RAISE EXCEPTION 'O nome do talhao e obrigatorio.';
    END IF;
    IF NEW.area IS NULL OR NEW.area <= 0 THEN
        RAISE EXCEPTION 'A area do talhao deve ser maior que zero.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_talhao ON talhao@@
CREATE TRIGGER agrodata_normalizar_talhao
BEFORE INSERT OR UPDATE ON talhao
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_talhao()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_telefone_produtor()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.telefone := regexp_replace(coalesce(NEW.telefone, ''), '[^0-9]', '', 'g');
    NEW.tipo := coalesce(
        nullif(btrim(regexp_replace(coalesce(NEW.tipo, ''), '[[:space:]]+', ' ', 'g')), ''),
        'Celular'
    );
    IF char_length(NEW.telefone) NOT BETWEEN 10 AND 13 THEN
        RAISE EXCEPTION 'O telefone deve conter de 10 a 13 digitos.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_telefone_produtor ON telefone_produtor@@
CREATE TRIGGER agrodata_normalizar_telefone_produtor
BEFORE INSERT OR UPDATE ON telefone_produtor
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_telefone_produtor()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_email_produtor()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.email := lower(btrim(coalesce(NEW.email, '')));
    NEW.tipo := coalesce(
        nullif(btrim(regexp_replace(coalesce(NEW.tipo, ''), '[[:space:]]+', ' ', 'g')), ''),
        'Pessoal'
    );
    IF NEW.email !~ '^[^[:space:]@]+@[^[:space:]@]+[.]com([.]br)?$' THEN
        RAISE EXCEPTION 'O e-mail deve conter @ e terminar em .com ou .com.br.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_email_produtor ON email_produtor@@
CREATE TRIGGER agrodata_normalizar_email_produtor
BEFORE INSERT OR UPDATE ON email_produtor
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_email_produtor()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_cultura()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.nome := btrim(regexp_replace(coalesce(NEW.nome, ''), '[[:space:]]+', ' ', 'g'));
    IF NEW.nome = '' THEN
        RAISE EXCEPTION 'O nome da cultura e obrigatorio.';
    END IF;
    IF NEW.umidade_minima IS NULL OR NEW.umidade_maxima IS NULL
       OR NEW.umidade_minima < 0 OR NEW.umidade_maxima > 100
       OR NEW.umidade_minima >= NEW.umidade_maxima THEN
        RAISE EXCEPTION 'A faixa de umidade da cultura e invalida.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_cultura ON cultura@@
CREATE TRIGGER agrodata_normalizar_cultura
BEFORE INSERT OR UPDATE ON cultura
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_cultura()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_sensor()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.tipo_sensor := btrim(regexp_replace(coalesce(NEW.tipo_sensor, ''), '[[:space:]]+', ' ', 'g'));
    NEW.status := upper(btrim(coalesce(NEW.status, 'ATIVO')));
    IF NEW.status = '' THEN
        NEW.status := 'ATIVO';
    END IF;
    IF NEW.tipo_sensor = '' THEN
        RAISE EXCEPTION 'O tipo do sensor e obrigatorio.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_sensor ON sensor@@
CREATE TRIGGER agrodata_normalizar_sensor
BEFORE INSERT OR UPDATE ON sensor
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_sensor()@@

CREATE OR REPLACE FUNCTION agrodata_normalizar_usuario()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.username := btrim(coalesce(NEW.username, ''));
    NEW.role := upper(btrim(coalesce(NEW.role, '')));
    IF NEW.username !~ '^[A-Za-z0-9._-]{3,50}$' THEN
        RAISE EXCEPTION 'O nome de usuario e obrigatorio e deve ter de 3 a 50 caracteres validos.';
    END IF;
    IF NEW.senha_hash IS NULL OR btrim(NEW.senha_hash) = '' THEN
        RAISE EXCEPTION 'A senha criptografada e obrigatoria.';
    END IF;
    IF NEW.role NOT IN ('ADMIN', 'PRODUTOR', 'IRRIGACAO') THEN
        RAISE EXCEPTION 'O perfil do usuario e invalido.';
    END IF;
    RETURN NEW;
END;
$$;@@
DROP TRIGGER IF EXISTS agrodata_normalizar_usuario ON usuario@@
CREATE TRIGGER agrodata_normalizar_usuario
BEFORE INSERT OR UPDATE ON usuario
FOR EACH ROW EXECUTE FUNCTION agrodata_normalizar_usuario()@@

INSERT INTO cultura (nome, umidade_minima, umidade_maxima)
VALUES
    ('Soja', 40.00, 65.00),
    ('Milho', 45.00, 70.00),
    ('Cana-de-açúcar', 40.00, 65.00),
    ('Café', 35.00, 60.00),
    ('Feijão', 45.00, 70.00),
    ('Pastagem', 30.00, 60.00)
ON CONFLICT (nome) DO NOTHING@@
