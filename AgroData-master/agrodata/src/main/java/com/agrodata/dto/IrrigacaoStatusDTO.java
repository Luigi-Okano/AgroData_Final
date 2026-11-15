package com.agrodata.dto;

import java.time.LocalDateTime;

/**
 * situacao: REGAR | OK | EXCESSO_UMIDADE | SEM_DADOS | SEM_CULTURA
 * comandoStatus: status do comando aberto enviado ao sistema de irrigação (ou null)
 */
public record IrrigacaoStatusDTO(
        Long idTalhao, String talhao, String cultura,
        Double umidadeAtual, Double umidadeMinima, Double umidadeMaxima,
        LocalDateTime dataLeitura, String situacao, String comandoStatus,
        Double probabilidadeChuva) {}
