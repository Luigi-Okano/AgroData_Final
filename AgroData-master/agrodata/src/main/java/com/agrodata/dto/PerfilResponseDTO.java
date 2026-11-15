package com.agrodata.dto;

import com.agrodata.model.Usuario;
import com.agrodata.util.Mascara;

/** Visão do PRÓPRIO usuário: dados pessoais sempre mascarados. */
public record PerfilResponseDTO(Long idUsuario, String username, String role,
                                String nomeCompleto, String cpf, String email, String telefone) {

    public static PerfilResponseDTO de(Usuario u) {
        return new PerfilResponseDTO(u.getIdUsuario(), u.getUsername(), u.getRole().name(),
                Mascara.nome(u.getNomeCompleto()), Mascara.cpf(u.getCpf()),
                Mascara.email(u.getEmail()), Mascara.telefone(u.getTelefone()));
    }
}
