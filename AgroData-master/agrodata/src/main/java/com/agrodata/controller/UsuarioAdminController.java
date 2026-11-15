package com.agrodata.controller;

import com.agrodata.dto.AdminUsuarioUpdateDTO;
import com.agrodata.dto.UsuarioDetalheDTO;
import com.agrodata.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Somente ADMIN (regra em SecurityConfig): acesso completo, sem máscara, aos dados dos usuários. */
@RestController
@RequestMapping("/api/auth/usuarios")
public class UsuarioAdminController {

    private final UsuarioService service;

    public UsuarioAdminController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDetalheDTO> listar(Authentication auth) {
        return service.listarTodos(auth.getName());
    }

    @GetMapping("/{id}")
    public UsuarioDetalheDTO detalhar(@PathVariable Long id, Authentication auth) {
        return service.detalhar(id, auth.getName());
    }

    @PutMapping("/{id}")
    public UsuarioDetalheDTO atualizar(@PathVariable Long id,
                                       @Valid @RequestBody AdminUsuarioUpdateDTO dto, Authentication auth) {
        return service.atualizarComoAdmin(id, dto, auth.getName());
    }
}
