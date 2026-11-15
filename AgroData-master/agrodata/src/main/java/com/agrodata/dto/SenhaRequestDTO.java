package com.agrodata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SenhaRequestDTO {

    @NotBlank @Size(max = 72)
    private String senhaAtual;

    @NotBlank
    @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
    private String novaSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
