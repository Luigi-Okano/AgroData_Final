package com.agrodata.model;

public enum StatusComando {
    PENDENTE,   // criado, aguardando o sistema de irrigação buscar (ou webhook ainda não entregue)
    ENVIADO,    // entregue ao sistema de irrigação via webhook
    CONCLUIDO,  // irrigação confirmada / umidade recuperada
    CANCELADO   // umidade voltou ao normal antes de o comando ser entregue
}
