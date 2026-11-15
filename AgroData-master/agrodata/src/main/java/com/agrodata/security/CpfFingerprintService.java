package com.agrodata.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class CpfFingerprintService {

    private static final String CONTEXTO_CHAVE = "AgroData CPF fingerprint key v1";
    private final SecretKeySpec chave;

    public CpfFingerprintService(@Value("${agrodata.security.data-key}") String chaveBase64) {
        byte[] chaveMestre;
        try {
            chaveMestre = Base64.getDecoder().decode(chaveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY deve estar em Base64.", e);
        }
        if (chaveMestre.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY deve ter exatamente 32 bytes.");
        }

        try {
            Mac derivador = Mac.getInstance("HmacSHA256");
            derivador.init(new SecretKeySpec(chaveMestre, "HmacSHA256"));
            byte[] chaveDerivada = derivador.doFinal(CONTEXTO_CHAVE.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(chaveDerivada, "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível inicializar a proteção do CPF.", e);
        }
    }

    public String gerar(String cpfNumerico) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(chave);
            return HexFormat.of().formatHex(hmac.doFinal(cpfNumerico.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível gerar a impressão digital do CPF.", e);
        }
    }
}
