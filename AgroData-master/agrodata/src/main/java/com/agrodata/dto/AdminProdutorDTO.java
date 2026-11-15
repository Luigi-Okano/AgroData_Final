package com.agrodata.dto;

import java.util.List;

/**
 * Visão administrativa: produtor -> fazendas -> talhões.
 * Para pequenos e médios produtores a unidade da lavoura é o TALHÃO (não se usa hectare nesta visão).
 */
public record AdminProdutorDTO(Long idProdutor, String nome, String cpf,
                               List<String> telefones, List<String> emails,
                               int totalFazendas, int totalTalhoes, int talhoesParaRegar,
                               List<Fazenda> fazendas) {

    public record Fazenda(Long idFazenda, String nome, String cidade, String estado,
                          int qtdTalhoes, List<Talhao> talhoes) {}

    /** situacaoIrrigacao: REGAR | OK | EXCESSO_UMIDADE | SEM_DADOS | SEM_CULTURA */
    public record Talhao(Long idTalhao, String nome, String cultura, String tipoSolo, String situacaoIrrigacao) {}
}
