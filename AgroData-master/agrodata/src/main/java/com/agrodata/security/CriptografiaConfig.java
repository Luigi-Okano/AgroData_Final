package com.agrodata.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CriptografiaConfig {

    public CriptografiaConfig(@Value("${agrodata.security.data-key}") String chaveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(chaveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY deve estar em Base64. Gere com: openssl rand -base64 32");
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY deve ter exatamente 32 bytes. Gere com: openssl rand -base64 32");
        }
        CriptografiaConverter.configurar(new SecretKeySpec(bytes, "AES"));
    }
}
