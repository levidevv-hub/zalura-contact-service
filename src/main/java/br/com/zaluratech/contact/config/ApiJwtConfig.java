package br.com.zaluratech.contact.config;

import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class ApiJwtConfig {
    @Bean public JwtDecoder apiJwtDecoder(@Value("${api.auth.issuer:http://localhost:8085}") String issuer, @Value("${api.auth.jwks-uri:http://localhost:8085/.well-known/jwks.json}") String jwks,
            @Value("${api.auth.audience:zalura-contact}") String audience) {
        if (!validUri(issuer) || !validUri(jwks) || audience.isBlank()) {
            throw new IllegalStateException("Configure issuer, JWKS e audience válidos.");
        }
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwks).jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), tenantValidator(audience)));
        return decoder;
    }
    public static OAuth2TokenValidator<Jwt> tenantValidator(String audience) {
        return jwt -> {
            try {
                Object company = jwt.getClaims().get("company_id");
                String subject = jwt.getSubject();
                if (!(company instanceof String id) || !UUID.fromString(id).toString().equals(id)
                        || subject == null || !UUID.fromString(subject).toString().equals(subject)
                        || jwt.getExpiresAt() == null || jwt.getIssuedAt() == null
                        || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                        || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
                    return invalid();
                }
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) { return invalid(); }
        };
    }
    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Token inválido.", null));
    }
    private boolean validUri(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && uri.getUserInfo() == null && uri.getFragment() == null && uri.getQuery() == null
                    && ("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme())
                    && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))));
        } catch (IllegalArgumentException ex) { return false; }
    }
}

