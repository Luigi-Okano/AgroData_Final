package com.agrodata.model;

import jakarta.persistence.*;

@Entity
@Table(name = "TELEFONE_PRODUTOR")
public class TelefoneProdutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_telefone")
    private Long idTelefone;

    @Column(name = "id_produtor", nullable = false)
    private Long idProdutor;

    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 20)
    private String tipo;

    public TelefoneProdutor() {}

    public TelefoneProdutor(Long idProdutor, String telefone, String tipo) {
        this.idProdutor = idProdutor;
        this.telefone = telefone;
        this.tipo = tipo;
    }

    public Long getIdTelefone() { return idTelefone; }
    public void setIdTelefone(Long idTelefone) { this.idTelefone = idTelefone; }

    public Long getIdProdutor() { return idProdutor; }
    public void setIdProdutor(Long idProdutor) { this.idProdutor = idProdutor; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
