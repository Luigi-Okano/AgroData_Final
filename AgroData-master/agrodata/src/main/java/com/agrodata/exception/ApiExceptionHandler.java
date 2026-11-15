package com.agrodata.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

/** Respostas de erro padronizadas, sem vazar detalhes internos (SQL, stack trace, nomes de classes). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacao(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> corpoInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Requisição inválida."));
    }

    /** Erros de regra de negócio lançados pelos serviços (mensagem escrita por nós, segura para exibir). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> regraDeNegocio(ResponseStatusException ex) {
        String msg = ex.getReason() != null ? ex.getReason() : "Requisição inválida.";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", msg));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409)
                .body(Map.of("message", "Operação viola a integridade dos dados (registro relacionado inexistente ou duplicado)."));
    }
}
