package com.agrodata.dto;

import java.math.BigDecimal;

public record TalhaoResponseDTO(Long idTalhao, Long idFazenda, String nome, BigDecimal area,
                                String tipoSolo, Long idCultura, Long idSensor, int leiturasGeradas) {}
