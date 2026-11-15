package com.agrodata.controller;

import com.agrodata.dto.ComandoIrrigacaoDTO;
import com.agrodata.dto.IrrigacaoStatusDTO;
import com.agrodata.model.Cultura;
import com.agrodata.service.IrrigacaoService;
import com.agrodata.service.ProdutorAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class IrrigacaoController {

    private final IrrigacaoService service;
    private final ProdutorAccessService produtorAccess;

    public IrrigacaoController(IrrigacaoService service, ProdutorAccessService produtorAccess) {
        this.service = service;
        this.produtorAccess = produtorAccess;
    }

    // ---- Painel (ADMIN / PRODUTOR) ----

    @GetMapping("/api/agrodata/irrigacao/status")
    public List<IrrigacaoStatusDTO> status(Authentication authentication) {
        boolean admin = produtorAccess.isAdmin(authentication);
        List<Long> idsTalhoes = produtorAccess.idsTalhoesAcessiveis(authentication.getName(), admin);
        return service.listarStatus().stream()
                .filter(status -> idsTalhoes.contains(status.idTalhao()))
                .toList();
    }

    @GetMapping("/api/agrodata/culturas")
    public List<Cultura> culturas() {
        return service.listarCulturas();
    }

    // ---- Sistema de irrigação (ADMIN / IRRIGACAO) ----

    @GetMapping("/api/irrigacao/comandos/pendentes")
    public List<ComandoIrrigacaoDTO> pendentes() {
        return service.listarComandosPendentes();
    }

    @PostMapping("/api/irrigacao/comandos/{idComando}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long idComando) {
        return service.confirmarComando(idComando)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
