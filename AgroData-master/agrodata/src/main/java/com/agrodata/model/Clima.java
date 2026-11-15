package com.agrodata.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLIMA")
public class Clima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clima")
    private Long idClima;

    @Column(name = "id_talhao")
    private Long idTalhao;

    @Column(name = "probabilidade_chuva")
    private Double probabilidadeChuva;

    @Column(name = "temperatura_ambiente")
    private Double temperaturaAmbiente;

    @Column(name = "umidade_ar")
    private Double umidadeAr;

    @Column(name = "data_registro")
    private LocalDateTime dataRegistro;

    public Clima() {}

    public Clima(Long idTalhao, Double probabilidadeChuva, Double temperaturaAmbiente, Double umidadeAr, LocalDateTime dataRegistro) {
        this.idTalhao = idTalhao;
        this.probabilidadeChuva = probabilidadeChuva;
        this.temperaturaAmbiente = temperaturaAmbiente;
        this.umidadeAr = umidadeAr;
        this.dataRegistro = dataRegistro;
    }

    public Long getIdClima() { return idClima; }
    public void setIdClima(Long idClima) { this.idClima = idClima; }

    public Long getIdTalhao() { return idTalhao; }
    public void setIdTalhao(Long idTalhao) { this.idTalhao = idTalhao; }

    public Double getProbabilidadeChuva() { return probabilidadeChuva; }
    public void setProbabilidadeChuva(Double probabilidadeChuva) { this.probabilidadeChuva = probabilidadeChuva; }

    public Double getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public void setTemperaturaAmbiente(Double temperaturaAmbiente) { this.temperaturaAmbiente = temperaturaAmbiente; }

    public Double getUmidadeAr() { return umidadeAr; }
    public void setUmidadeAr(Double umidadeAr) { this.umidadeAr = umidadeAr; }

    public LocalDateTime getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDateTime dataRegistro) { this.dataRegistro = dataRegistro; }
}
