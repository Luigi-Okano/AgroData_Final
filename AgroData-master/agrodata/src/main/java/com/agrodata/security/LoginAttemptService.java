package com.agrodata.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção básica contra força bruta: após MAX_TENTATIVAS falhas seguidas
 * para a combinação IP + usuário, bloqueia novas tentativas por BLOQUEIO.
 * (Em memória; em ambiente com várias instâncias use Redis/Bucket4j.)
 */
@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Tentativa(int falhas, Instant bloqueadoAte) {}

    private final ConcurrentHashMap<String, Tentativa> tentativas = new ConcurrentHashMap<>();

    private String chave(String ip, String username) {
        return ip + "|" + (username == null ? "" : username.toLowerCase());
    }

    public boolean estaBloqueado(String ip, String username) {
        Tentativa t = tentativas.get(chave(ip, username));
        if (t == null || t.bloqueadoAte() == null) return false;
        if (Instant.now().isAfter(t.bloqueadoAte())) {
            tentativas.remove(chave(ip, username));
            return false;
        }
        return true;
    }

    public void registrarFalha(String ip, String username) {
        tentativas.merge(chave(ip, username), new Tentativa(1, null), (atual, novo) -> {
            int falhas = atual.falhas() + 1;
            return falhas >= MAX_TENTATIVAS
                    ? new Tentativa(falhas, Instant.now().plus(BLOQUEIO))
                    : new Tentativa(falhas, null);
        });
    }

    public void registrarSucesso(String ip, String username) {
        tentativas.remove(chave(ip, username));
    }
}
