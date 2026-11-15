package com.agrodata.model;

import jakarta.persistence.*;

/** Faixa de umidade do solo (%) considerada adequada para cada tipo de planta. */
@Entity
@Table(name = "CULTURA")
public class Cultura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cultura")
    private Long idCultura;

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    /** Abaixo deste valor o talhão precisa ser irrigado. */
    @Column(name = "umidade_minima", nullable = false)
    private Double umidadeMinima;

    /** Acima deste valor há excesso de água. */
    @Column(name = "umidade_maxima", nullable = false)
    private Double umidadeMaxima;

    public Cultura() {}

    public Long getIdCultura() { return idCultura; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Double getUmidadeMinima() { return umidadeMinima; }
    public void setUmidadeMinima(Double umidadeMinima) { this.umidadeMinima = umidadeMinima; }
    public Double getUmidadeMaxima() { return umidadeMaxima; }
    public void setUmidadeMaxima(Double umidadeMaxima) { this.umidadeMaxima = umidadeMaxima; }
}
