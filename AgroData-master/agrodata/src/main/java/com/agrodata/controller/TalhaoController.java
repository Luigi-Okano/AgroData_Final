package com.agrodata.controller;

import com.agrodata.dto.FazendaDTO;
import com.agrodata.dto.FazendaDetalheDTO;
import com.agrodata.dto.FazendaRequestDTO;
import com.agrodata.dto.TalhaoRequestDTO;
import com.agrodata.dto.TalhaoResponseDTO;
import com.agrodata.dto.TalhaoUpdateDTO;
import com.agrodata.service.FazendaService;
import com.agrodata.service.ProdutorAccessService;
import com.agrodata.service.TalhaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agrodata")
public class TalhaoController {

    private final TalhaoService service;
    private final FazendaService fazendaService;
    private final ProdutorAccessService produtorAccess;

    public TalhaoController(TalhaoService service, FazendaService fazendaService,
                            ProdutorAccessService produtorAccess) {
        this.service = service;
        this.fazendaService = fazendaService;
        this.produtorAccess = produtorAccess;
    }

    @GetMapping("/fazendas")
    public List<FazendaDTO> fazendas(Authentication authentication) {
        return service.listarFazendas(authentication.getName(), produtorAccess.isAdmin(authentication));
    }

    @PostMapping("/fazendas")
    public ResponseEntity<FazendaDTO> criarFazenda(@Valid @RequestBody FazendaRequestDTO dto,
                                                    Authentication authentication) {
        return ResponseEntity.status(201).body(fazendaService.cadastrar(authentication.getName(), dto));
    }

    @GetMapping("/fazendas/{idFazenda}")
    public FazendaDetalheDTO detalharFazenda(@PathVariable Long idFazenda, Authentication authentication) {
        produtorAccess.exigirAcessoFazenda(idFazenda, authentication.getName(),
                produtorAccess.isAdmin(authentication));
        return fazendaService.detalhar(idFazenda);
    }

    @PutMapping("/fazendas/{idFazenda}")
    public ResponseEntity<Void> atualizarFazenda(@PathVariable Long idFazenda,
                                                  @Valid @RequestBody FazendaRequestDTO dto,
                                                  Authentication authentication) {
        produtorAccess.exigirAcessoFazenda(idFazenda, authentication.getName(),
                produtorAccess.isAdmin(authentication));
        fazendaService.atualizar(idFazenda, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/fazendas/{idFazenda}")
    public ResponseEntity<Void> excluirFazenda(@PathVariable Long idFazenda) {
        fazendaService.excluir(idFazenda);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/talhoes")
    public ResponseEntity<TalhaoResponseDTO> criar(@Valid @RequestBody TalhaoRequestDTO dto,
                                                    Authentication authentication) {
        return ResponseEntity.status(201).body(service.cadastrar(
                dto, authentication.getName(), produtorAccess.isAdmin(authentication)));
    }

    @PutMapping("/talhoes/{idTalhao}")
    public ResponseEntity<Void> atualizarTalhao(@PathVariable Long idTalhao,
                                                 @Valid @RequestBody TalhaoUpdateDTO dto,
                                                 Authentication authentication) {
        service.atualizar(idTalhao, dto, authentication.getName(), produtorAccess.isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/talhoes/{idTalhao}")
    public ResponseEntity<Void> excluirTalhao(@PathVariable Long idTalhao) {
        service.excluir(idTalhao);
        return ResponseEntity.noContent().build();
    }
}
