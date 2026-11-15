package com.agrodata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos pessoais editáveis. Campo nulo ou em branco = manter o valor atual. CPF não pode ser alterado. */
public class PerfilUpdateDTO {

    @Size(max = 100)
    private String nomeCompleto;

    @Email @Size(max = 100)
    private String email;

    @Pattern(regexp = "^[0-9()+\\-\\s]{0,20}$", message = "telefone inválido")
    private String telefone;

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
