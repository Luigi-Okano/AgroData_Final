package com.agrodata.security;

import com.agrodata.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration expiracao;

    public JwtService(JwtEncoder encoder,
                      @Value("${agrodata.security.jwt.expiration-minutes:60}") long expiracaoMinutos) {
        this.encoder = encoder;
        this.expiracao = Duration.ofMinutes(expiracaoMinutos);
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .subject(usuario.getUsername())
                .claim("roles", List.of(usuario.getRole().name()))
                .claim("temp", usuario.isSenhaTemporaria())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiracaoSegundos() {
        return expiracao.toSeconds();
    }
}
