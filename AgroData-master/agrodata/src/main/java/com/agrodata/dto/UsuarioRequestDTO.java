package com.agrodata.dto;

import com.agrodata.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioRequestDTO extends PerfilUpdateDTO {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "use apenas letras, números, ponto, hífen e underscore")
    private String username;

    // BCrypt só considera os primeiros 72 bytes
    @NotBlank
    @Size(min = 10, max = 72, message = "a senha deve ter entre 10 e 72 caracteres")
    private String password;

    @NotNull
    private Role role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
