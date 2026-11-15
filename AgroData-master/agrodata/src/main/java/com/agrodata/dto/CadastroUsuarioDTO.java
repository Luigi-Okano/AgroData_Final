package com.agrodata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastroUsuarioDTO extends PerfilUpdateDTO {

    private String cpf;

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "use apenas letras, números, ponto, hífen e underscore")
    private String username;

    @NotBlank
    @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
    @Pattern(
            regexp = "^(?=.*\\p{Lu})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}\\s]).+$",
            message = "a senha deve conter ao menos uma letra maiúscula, um número e um caractere especial")
    private String password;

    @NotBlank
    @Size(min = 3, max = 100)
    @Override
    public String getNomeCompleto() { return super.getNomeCompleto(); }

    @NotBlank(message = "CPF obrigatório")
    @Pattern(regexp = "^(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})$",
            message = "Informe 11 dígitos para o CPF.")
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    @NotBlank
    @Size(max = 100)
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.com(?:\\.br)?$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "o e-mail deve terminar em .com ou .com.br")
    @Override
    public String getEmail() { return super.getEmail(); }

    @NotBlank(message = "telefone obrigatório")
    @Override
    public String getTelefone() { return super.getTelefone(); }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
