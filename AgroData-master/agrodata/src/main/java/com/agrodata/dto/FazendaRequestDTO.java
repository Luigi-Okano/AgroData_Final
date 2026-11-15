package com.agrodata.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FazendaRequestDTO {

    @NotBlank @Size(max = 100)
    private String nome;

    @NotNull @DecimalMin(value = "0.01", message = "a área deve ser maior que zero")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal areaTotal;

    @NotBlank @Size(max = 150)
    private String rua;

    @NotBlank @Size(max = 20)
    private String numero;

    @NotBlank @Size(max = 100)
    private String bairro;

    @NotBlank @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "o CEP deve conter 8 dígitos, com ou sem hífen")
    private String cep;

    @NotBlank @Size(max = 100)
    private String cidade;

    @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "informe a sigla de 2 letras do estado")
    private String estado;

    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private Double longitude;

    @AssertTrue(message = "Informe latitude e longitude juntas para ativar a previsão de chuva.")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getAreaTotal() { return areaTotal; }
    public void setAreaTotal(BigDecimal areaTotal) { this.areaTotal = areaTotal; }
    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
