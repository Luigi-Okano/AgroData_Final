package com.agrodata.controller;

import com.agrodata.dto.CpfRequestDTO;
import com.agrodata.dto.PerfilResponseDTO;
import com.agrodata.dto.PerfilUpdateDTO;
import com.agrodata.dto.SenhaRequestDTO;
import com.agrodata.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** O usuário só enxerga/edita a PRÓPRIA conta (identificada pelo token), sempre com dados mascarados. */
@RestController
@RequestMapping("/api/auth/me")
public class PerfilController {

    private final UsuarioService service;

    public PerfilController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public PerfilResponseDTO meuPerfil(Authentication auth) {
        return PerfilResponseDTO.de(service.porUsername(auth.getName()));
    }

    @PutMapping
    public PerfilResponseDTO atualizar(Authentication auth, @Valid @RequestBody PerfilUpdateDTO dto) {
        return PerfilResponseDTO.de(service.atualizarMeusDados(auth.getName(), dto));
    }

    @PutMapping("/cpf")
    public PerfilResponseDTO cadastrarCpf(Authentication auth, @Valid @RequestBody CpfRequestDTO dto) {
        return PerfilResponseDTO.de(service.cadastrarCpf(auth.getName(), dto.getCpf()));
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> trocarSenha(Authentication auth, @Valid @RequestBody SenhaRequestDTO dto) {
        service.trocarSenha(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }
}
