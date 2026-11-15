package com.agrodata.dto;

import com.agrodata.model.Role;
import jakarta.validation.constraints.Size;

public class AdminUsuarioUpdateDTO extends PerfilUpdateDTO {

    private Role role;
    private Boolean ativo;

    /** Redefinição de senha pelo administrador (opcional, 10 a 72 caracteres). */
    @Size(max = 72)
    private String novaSenha;

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
}
