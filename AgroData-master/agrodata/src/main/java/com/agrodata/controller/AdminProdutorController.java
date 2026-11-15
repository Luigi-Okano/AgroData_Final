package com.agrodata.controller;

import com.agrodata.dto.AdminProdutorDTO;
import com.agrodata.service.AdminProdutorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Somente ADMIN (regra /api/admin/** em SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminProdutorController {

    private final AdminProdutorService service;

    public AdminProdutorController(AdminProdutorService service) {
        this.service = service;
    }

    @GetMapping("/produtores")
    public List<AdminProdutorDTO> produtores(Authentication auth) {
        return service.listar(auth.getName());
    }
}
