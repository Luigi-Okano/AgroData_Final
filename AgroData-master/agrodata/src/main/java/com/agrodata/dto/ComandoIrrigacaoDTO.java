package com.agrodata.dto;

import java.time.LocalDateTime;

public record ComandoIrrigacaoDTO(
        Long idComando, Long idTalhao, String talhao, String cultura,
        Double umidadeAtual, Double umidadeMinima, String status, LocalDateTime criadoEm) {}
