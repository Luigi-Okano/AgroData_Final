package com.agrodata.service;

import com.agrodata.model.ComandoIrrigacao;
import com.agrodata.model.StatusComando;
import com.agrodata.repository.ComandoIrrigacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Avisa o sistema de irrigação. Se IRRIGACAO_WEBHOOK_URL estiver configurada, envia um POST JSON;
 * caso contrário (ou se falhar) o comando continua PENDENTE e o sistema pode buscá-lo em
 * GET /api/irrigacao/comandos/pendentes.
 */
@Component
public class IrrigacaoNotifier {

    private static final Logger log = LoggerFactory.getLogger(IrrigacaoNotifier.class);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ComandoIrrigacaoRepository comandos;
    private final String webhookUrl;
    private final String webhookToken;

    public IrrigacaoNotifier(ComandoIrrigacaoRepository comandos,
                             @Value("${agrodata.irrigacao.webhook-url:}") String webhookUrl,
                             @Value("${agrodata.irrigacao.webhook-token:}") String webhookToken) {
        this.comandos = comandos;
        this.webhookUrl = webhookUrl.trim();
        this.webhookToken = webhookToken.trim();
    }

    public void notificar(ComandoIrrigacao cmd, String talhao, String cultura) {
        if (webhookUrl.isEmpty()) return;
        try {
            String json = String.format(Locale.ROOT,
                "{\"idComando\":%d,\"acao\":\"IRRIGAR\",\"idTalhao\":%d,\"talhao\":\"%s\",\"cultura\":\"%s\","
                + "\"umidadeAtual\":%.2f,\"umidadeMinima\":%.2f}",
                cmd.getIdComando(), cmd.getIdTalhao(), escapar(talhao), escapar(cultura),
                cmd.getUmidadeAtual(), cmd.getUmidadeMinima());

            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (!webhookToken.isEmpty()) req.header("Authorization", "Bearer " + webhookToken);

            client.sendAsync(req.build(), HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    if (resp.statusCode() / 100 == 2) {
                        comandos.findById(cmd.getIdComando()).ifPresent(c -> {
                            if (c.getStatus() == StatusComando.PENDENTE) {
                                c.setStatus(StatusComando.ENVIADO);
                                c.setEnviadoEm(LocalDateTime.now());
                                comandos.save(c);
                            }
                        });
                    } else {
                        log.warn("Sistema de irrigação respondeu HTTP {} ao comando {}", resp.statusCode(), cmd.getIdComando());
                    }
                })
                .exceptionally(ex -> {
                    log.warn("Não foi possível avisar o sistema de irrigação (comando {}): {}", cmd.getIdComando(), ex.getMessage());
                    return null;
                });
        } catch (Exception e) {
            log.warn("Webhook de irrigação inválido ou erro ao montar requisição: {}", e.getMessage());
        }
    }

    private static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\p{Cntrl}]", " ");
    }
}
