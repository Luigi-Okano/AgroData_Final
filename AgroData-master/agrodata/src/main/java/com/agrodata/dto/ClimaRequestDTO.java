package com.agrodata.dto;

import jakarta.validation.constraints.*;

public class ClimaRequestDTO {

    @DecimalMin("0.0") @DecimalMax("100.0")
    private Double probabilidadeChuva;
    @DecimalMin("-60.0") @DecimalMax("80.0")
    private Double temperaturaAmbiente;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private Double umidadeAr;

    public ClimaRequestDTO() {}

    public ClimaRequestDTO(Double probabilidadeChuva, Double temperaturaAmbiente, Double umidadeAr) {
        this.probabilidadeChuva = probabilidadeChuva;
        this.temperaturaAmbiente = temperaturaAmbiente;
        this.umidadeAr = umidadeAr;
    }

    public Double getProbabilidadeChuva() { return probabilidadeChuva; }
    public void setProbabilidadeChuva(Double probabilidadeChuva) { this.probabilidadeChuva = probabilidadeChuva; }

    public Double getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public void setTemperaturaAmbiente(Double temperaturaAmbiente) { this.temperaturaAmbiente = temperaturaAmbiente; }

    public Double getUmidadeAr() { return umidadeAr; }
    public void setUmidadeAr(Double umidadeAr) { this.umidadeAr = umidadeAr; }
}
