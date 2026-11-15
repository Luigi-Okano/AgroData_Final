package com.agrodata.dto;

import java.math.BigDecimal;

public record FazendaDTO(Long idFazenda, String nome, BigDecimal areaTotal, BigDecimal areaDisponivel, int qtdTalhoes) {}
