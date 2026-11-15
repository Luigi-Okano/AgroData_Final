package com.agrodata.dto;

public class ClimaResponseDTO {

    private Double probabilidadeChuva;
    private Double temperaturaAmbiente;
    private Double umidadeAr;
    private String statusClima;

    public ClimaResponseDTO() {}

    public ClimaResponseDTO(Double probabilidadeChuva, Double temperaturaAmbiente, Double umidadeAr, String statusClima) {
        this.probabilidadeChuva = probabilidadeChuva;
        this.temperaturaAmbiente = temperaturaAmbiente;
        this.umidadeAr = umidadeAr;
        this.statusClima = statusClima;
    }

    public Double getProbabilidadeChuva() { return probabilidadeChuva; }
    public void setProbabilidadeChuva(Double probabilidadeChuva) { this.probabilidadeChuva = probabilidadeChuva; }

    public Double getTemperaturaAmbiente() { return temperaturaAmbiente; }
    public void setTemperaturaAmbiente(Double temperaturaAmbiente) { this.temperaturaAmbiente = temperaturaAmbiente; }

    public Double getUmidadeAr() { return umidadeAr; }
    public void setUmidadeAr(Double umidadeAr) { this.umidadeAr = umidadeAr; }

    public String getStatusClima() { return statusClima; }
    public void setStatusClima(String statusClima) { this.statusClima = statusClima; }
}
