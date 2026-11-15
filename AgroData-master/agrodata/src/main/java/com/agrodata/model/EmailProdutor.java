package com.agrodata.model;

import jakarta.persistence.*;

@Entity
@Table(name = "EMAIL_PRODUTOR")
public class EmailProdutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_email")
    private Long idEmail;

    @Column(name = "id_produtor", nullable = false)
    private Long idProdutor;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String tipo;

    public EmailProdutor() {}

    public EmailProdutor(Long idProdutor, String email, String tipo) {
        this.idProdutor = idProdutor;
        this.email = email;
        this.tipo = tipo;
    }

    public Long getIdEmail() { return idEmail; }
    public void setIdEmail(Long idEmail) { this.idEmail = idEmail; }

    public Long getIdProdutor() { return idProdutor; }
    public void setIdProdutor(Long idProdutor) { this.idProdutor = idProdutor; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
