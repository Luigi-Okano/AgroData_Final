package com.agrodata.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TalhaoRequestDTO {

    @NotNull
    private Long idFazenda;

    @NotBlank @Size(max = 50)
    private String nome;

    @NotNull @DecimalMin(value = "0.01", message = "a área deve ser maior que zero")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal area;

    @Size(max = 50)
    private String tipoSolo;

    private Long idCultura;

    public Long getIdFazenda() { return idFazenda; }
    public void setIdFazenda(Long idFazenda) { this.idFazenda = idFazenda; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getTipoSolo() { return tipoSolo; }
    public void setTipoSolo(String tipoSolo) { this.tipoSolo = tipoSolo; }
    public Long getIdCultura() { return idCultura; }
    public void setIdCultura(Long idCultura) { this.idCultura = idCultura; }
}
