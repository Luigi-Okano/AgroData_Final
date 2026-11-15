package com.agrodata.dto;

import com.agrodata.model.Usuario;
import com.agrodata.util.Cpf;

/** Visão do ADMINISTRADOR: dados completos, sem máscara (nunca inclui o hash da senha). */
public record UsuarioDetalheDTO(Long idUsuario, String username, String role, boolean ativo,
                                String nomeCompleto, String cpf, String email, String telefone) {

    public static UsuarioDetalheDTO de(Usuario u) {
        return new UsuarioDetalheDTO(u.getIdUsuario(), u.getUsername(), u.getRole().name(), u.isAtivo(),
                u.getNomeCompleto(), u.getCpf() == null ? null : Cpf.formatar(u.getCpf()),
                u.getEmail(), u.getTelefone());
    }
}
