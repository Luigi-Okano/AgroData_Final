package com.agrodata.model;

import jakarta.persistence.*;

@Entity
@Table(name = "PRODUTOR")
public class Produtor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produtor")
    private Long idProdutor;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    public Produtor() {}

    public Produtor(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    public Long getIdProdutor() { return idProdutor; }
    public void setIdProdutor(Long idProdutor) { this.idProdutor = idProdutor; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}
