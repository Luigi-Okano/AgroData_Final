package com.agrodata.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "TALHAO")
public class Talhao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_talhao")
    private Long idTalhao;

    @Column(name = "id_fazenda", nullable = false)
    private Long idFazenda;

    @Column(nullable = false, length = 50)
    private String nome;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal area;

    @Column(name = "tipo_solo", length = 50)
    private String tipoSolo;

    @Column(name = "id_cultura")
    private Long idCultura;

    public Talhao() {}

    public Talhao(Long idFazenda, String nome, BigDecimal area, String tipoSolo) {
        this.idFazenda = idFazenda;
        this.nome = nome;
        this.area = area;
        this.tipoSolo = tipoSolo;
    }

    public Long getIdTalhao() { return idTalhao; }
    public void setIdTalhao(Long idTalhao) { this.idTalhao = idTalhao; }

    public Long getIdFazenda() { return idFazenda; }
    public void setIdFazenda(Long idFazenda) { this.idFazenda = idFazenda; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public Long getIdCultura() { return idCultura; }
    public void setIdCultura(Long idCultura) { this.idCultura = idCultura; }

    public String getTipoSolo() { return tipoSolo; }
    public void setTipoSolo(String tipoSolo) { this.tipoSolo = tipoSolo; }
}
