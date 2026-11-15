package com.agrodata.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http
            .cors(c -> c.configurationSource(cors))
            // API stateless com token Bearer no header (não usa cookies) => CSRF não se aplica
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .contentTypeOptions(c -> {})
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .authorizeHttpRequests(a -> a
                // Painel estático (a tela de login fica nele; os dados só vêm da API autenticada)
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/css/**", "/js/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/cadastro").permitAll()

                // Administração de usuários
                .requestMatchers("/api/auth/usuarios/**").hasRole("ADMIN")
                // Perfil próprio (qualquer usuário autenticado; sempre sobre a própria conta)
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/auth/me", "/api/auth/me/senha").authenticated()

                // Visão administrativa de produtores, fazendas e talhões
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Integração com o sistema de irrigação (conta de serviço)
                .requestMatchers("/api/irrigacao/**").hasAnyRole("ADMIN", "IRRIGACAO")

                // Exclusões seguem administrativas; produtores podem cadastrar dados nas próprias áreas.
                .requestMatchers(HttpMethod.DELETE, "/api/agrodata/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/agrodata/fazendas", "/api/agrodata/sensores",
                        "/api/agrodata/talhoes").hasAnyRole("ADMIN", "PRODUTOR")

                // Demais operações da API: ADMIN ou PRODUTOR
                .requestMatchers("/api/agrodata/**").hasAnyRole("ADMIN", "PRODUTOR")

                // Qualquer outra rota é negada por padrão
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            // Quem está com senha provisória só pode ver o próprio perfil e trocar a senha
            .addFilterAfter(new SenhaTemporariaFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** Origens permitidas via CORS_ALLOWED_ORIGINS (separadas por vírgula). Vazio = só mesma origem. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${agrodata.security.cors.allowed-origins:}") String origens) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> lista = Arrays.stream(origens.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOrigins(lista);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
