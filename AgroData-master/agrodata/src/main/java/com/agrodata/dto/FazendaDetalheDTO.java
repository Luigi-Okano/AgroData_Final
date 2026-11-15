package com.agrodata.dto;

import java.math.BigDecimal;

public record FazendaDetalheDTO(Long idFazenda, String nome, BigDecimal areaTotal,
                                String rua, String numero, String bairro, String cep,
                                String cidade, String estado, Double latitude, Double longitude) {}
