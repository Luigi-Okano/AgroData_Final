package com.agrodata.controller;

import com.agrodata.dto.*;
import com.agrodata.model.LeituraSensor;
import com.agrodata.model.Talhao;
import com.agrodata.service.AgroDataService;
import com.agrodata.service.ProdutorAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agrodata")
public class AgroDataController {

    private final AgroDataService agroDataService;
    private final ProdutorAccessService produtorAccess;

    public AgroDataController(AgroDataService agroDataService, ProdutorAccessService produtorAccess) {
        this.agroDataService = agroDataService;
        this.produtorAccess = produtorAccess;
    }

    @PostMapping("/sensores")
    public ResponseEntity<SensorResponseDTO> criarSensor(@Valid @RequestBody SensorRequestDTO dto,
                                                         Authentication authentication) {
        SensorResponseDTO novoSensor = agroDataService.salvarSensor(dto, authentication.getName(),
                produtorAccess.isAdmin(authentication));
        return ResponseEntity.status(201).body(novoSensor);
    }

    @GetMapping("/sensores")
    public ResponseEntity<List<SensorResponseDTO>> listarSensores(Authentication authentication) {
        return ResponseEntity.ok(agroDataService.listarSensores(authentication.getName(),
                produtorAccess.isAdmin(authentication)));
    }

    @PutMapping("/sensores/{idSensor}")
    public ResponseEntity<SensorResponseDTO> atualizarSensor(@PathVariable Long idSensor,
                                                              @Valid @RequestBody SensorUpdateDTO dto,
                                                              Authentication authentication) {
        return ResponseEntity.ok(agroDataService.atualizarSensor(idSensor, dto, authentication.getName(),
                produtorAccess.isAdmin(authentication)));
    }

    @GetMapping("/talhoes")
    public ResponseEntity<List<Talhao>> listarTalhoes(Authentication authentication) {
        return ResponseEntity.ok(agroDataService.listarTalhoes(authentication.getName(),
                produtorAccess.isAdmin(authentication)));
    }

    @DeleteMapping("/sensores/{idSensor}")
    public ResponseEntity<Void> excluirSensor(@PathVariable Long idSensor) {
        return switch (agroDataService.excluirSensor(idSensor)) {
            case EXCLUIDO -> ResponseEntity.noContent().build();
            case NAO_ENCONTRADO -> ResponseEntity.notFound().build();
            case POSSUI_LEITURAS -> ResponseEntity.status(409).build();
        };
    }

    @PostMapping("/leituras")
    public ResponseEntity<LeituraSensor> registrarLeitura(@Valid @RequestBody LeituraRequestDTO dto,
                                                           Authentication authentication) {
        LeituraSensor novaLeitura = agroDataService.registrarLeitura(dto, authentication.getName(),
                produtorAccess.isAdmin(authentication));
        return ResponseEntity.status(201).body(novaLeitura);
    }

    @GetMapping("/leituras/{idSensor}")
    public ResponseEntity<List<LeituraSensor>> listarLeituras(@PathVariable Long idSensor,
                                                              Authentication authentication) {
        return ResponseEntity.ok(agroDataService.obterHistoricoLeituras(idSensor, authentication.getName(),
                produtorAccess.isAdmin(authentication)));
    }

    @DeleteMapping("/leituras/{idLeitura}")
    public ResponseEntity<Void> excluirLeitura(@PathVariable Long idLeitura) {
        if (!agroDataService.excluirLeitura(idLeitura)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/analise/clima/{idSensor}")
    public ResponseEntity<Map<String, Object>> analisarIrrigacaoAvancada(
            @PathVariable Long idSensor,
            @Valid @RequestBody ClimaRequestDTO climaDTO,
            Authentication authentication) {
        Map<String, Object> resultado = agroDataService.analisarIrrigacaoComClima(idSensor, climaDTO,
                authentication.getName(), produtorAccess.isAdmin(authentication));
        return ResponseEntity.ok(resultado);
    }
}
