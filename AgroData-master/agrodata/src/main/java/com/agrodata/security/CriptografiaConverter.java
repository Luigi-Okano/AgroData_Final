package com.agrodata.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografa colunas sensíveis (AES-256-GCM) antes de gravar no banco.
 * A chave vem de DATA_ENCRYPTION_KEY (ver CriptografiaConfig). Sem a chave, os dados não são legíveis.
 */
@Converter
public class CriptografiaConverter implements AttributeConverter<String, String> {

    private static final String PREFIXO = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKey chave;

    static void configurar(SecretKey k) {
        chave = k;
    }

    private static SecretKey chaveOuErro() {
        SecretKey k = chave;
        if (k == null) throw new IllegalStateException("Chave de criptografia de dados não configurada.");
        return k;
    }

    @Override
    public String convertToDatabaseColumn(String valor) {
        if (valor == null) return null;
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, chaveOuErro(), new GCMParameterSpec(128, iv));
            byte[] cifrado = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado);
            return PREFIXO + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar dado.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String armazenado) {
        if (armazenado == null) return null;
        if (!armazenado.startsWith(PREFIXO)) return armazenado; // legado em texto puro
        try {
            byte[] tudo = Base64.getDecoder().decode(armazenado.substring(PREFIXO.length()));
            ByteBuffer buf = ByteBuffer.wrap(tudo);
            byte[] iv = new byte[12];
            buf.get(iv);
            byte[] cifrado = new byte[buf.remaining()];
            buf.get(cifrado);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, chaveOuErro(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao descriptografar dado (chave incorreta?).", e);
        }
    }
}
