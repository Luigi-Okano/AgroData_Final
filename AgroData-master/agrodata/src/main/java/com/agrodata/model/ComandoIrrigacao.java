package com.agrodata.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Ordem de irrigação gerada quando a umidade fica abaixo do mínimo da cultura. */
@Entity
@Table(name = "COMANDO_IRRIGACAO")
public class ComandoIrrigacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comando")
    private Long idComando;

    @Column(name = "id_talhao", nullable = false)
    private Long idTalhao;

    @Column(name = "id_leitura")
    private Long idLeitura;

    @Column(name = "umidade_atual", nullable = false)
    private Double umidadeAtual;

    @Column(name = "umidade_minima", nullable = false)
    private Double umidadeMinima;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusComando status = StatusComando.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    public ComandoIrrigacao() {}

    public ComandoIrrigacao(Long idTalhao, Long idLeitura, Double umidadeAtual, Double umidadeMinima) {
        this.idTalhao = idTalhao;
        this.idLeitura = idLeitura;
        this.umidadeAtual = umidadeAtual;
        this.umidadeMinima = umidadeMinima;
    }

    public Long getIdComando() { return idComando; }
    public Long getIdTalhao() { return idTalhao; }
    public Long getIdLeitura() { return idLeitura; }
    public Double getUmidadeAtual() { return umidadeAtual; }
    public Double getUmidadeMinima() { return umidadeMinima; }
    public StatusComando getStatus() { return status; }
    public void setStatus(StatusComando status) { this.status = status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(LocalDateTime enviadoEm) { this.enviadoEm = enviadoEm; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(LocalDateTime concluidoEm) { this.concluidoEm = concluidoEm; }
}
