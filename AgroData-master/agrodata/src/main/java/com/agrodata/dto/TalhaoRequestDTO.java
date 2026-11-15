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

    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private Double umidadeMinimaPersonalizada;

    @DecimalMin(value = "0.0") @DecimalMax(value = "100.0")
    private Double umidadeMaximaPersonalizada;

    @AssertTrue(message = "Informe os limites mínimo e máximo entre 0 e 100, com mínimo menor que máximo, somente para a opção Outro.")
    public boolean isFaixaUmidadePersonalizadaValida() {
        if (umidadeMinimaPersonalizada == null && umidadeMaximaPersonalizada == null) return true;
        return idCultura == null
                && umidadeMinimaPersonalizada != null
                && umidadeMaximaPersonalizada != null
                && Double.isFinite(umidadeMinimaPersonalizada)
                && Double.isFinite(umidadeMaximaPersonalizada)
                && umidadeMinimaPersonalizada < umidadeMaximaPersonalizada;
    }

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
    public Double getUmidadeMinimaPersonalizada() { return umidadeMinimaPersonalizada; }
    public void setUmidadeMinimaPersonalizada(Double umidadeMinimaPersonalizada) {
        this.umidadeMinimaPersonalizada = umidadeMinimaPersonalizada;
    }
    public Double getUmidadeMaximaPersonalizada() { return umidadeMaximaPersonalizada; }
    public void setUmidadeMaximaPersonalizada(Double umidadeMaximaPersonalizada) {
        this.umidadeMaximaPersonalizada = umidadeMaximaPersonalizada;
    }
}
