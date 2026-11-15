package com.agrodata.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "FAZENDA")
public class Fazenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fazenda")
    private Long idFazenda;

    @Column(name = "id_produtor", nullable = false)
    private Long idProdutor;

    @Column(name = "id_localizacao", nullable = false)
    private Long idLocalizacao;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "area_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaTotal;

    public Fazenda() {}

    public Fazenda(Long idProdutor, Long idLocalizacao, String nome, BigDecimal areaTotal) {
        this.idProdutor = idProdutor;
        this.idLocalizacao = idLocalizacao;
        this.nome = nome;
        this.areaTotal = areaTotal;
    }

    public Long getIdFazenda() { return idFazenda; }
    public void setIdFazenda(Long idFazenda) { this.idFazenda = idFazenda; }

    public Long getIdProdutor() { return idProdutor; }
    public void setIdProdutor(Long idProdutor) { this.idProdutor = idProdutor; }

    public Long getIdLocalizacao() { return idLocalizacao; }
    public void setIdLocalizacao(Long idLocalizacao) { this.idLocalizacao = idLocalizacao; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getAreaTotal() { return areaTotal; }
    public void setAreaTotal(BigDecimal areaTotal) { this.areaTotal = areaTotal; }
}
