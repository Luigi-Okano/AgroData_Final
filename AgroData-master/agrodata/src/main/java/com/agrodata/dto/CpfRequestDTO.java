package com.agrodata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CpfRequestDTO {

    @NotBlank(message = "CPF obrigatório")
    @Pattern(regexp = "^(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})$",
            message = "Informe 11 dígitos para o CPF.")
    private String cpf;

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
}
