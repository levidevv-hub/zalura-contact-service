package br.com.zaluratech.contact.integration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(properties = {"worker.auth.token=test-only-worker-token-01234567890123456789", "worker.auth.companies=11111111-1111-1111-1111-111111111111"})
class ApiAuthenticationIntegrationTest {
    private static final String SCHEMA = "security_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID CAMPAIGN = UUID.randomUUID();
    private static final String URL = "/api/contacts";
    private static final RSAKey KEY, WRONG;
    private static final HttpServer JWKS;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            WRONG = new RSAKeyGenerator(2048).keyID("test-key").generate();
            JWKS = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            JWKS.createContext("/jwks", exchange -> {
                byte[] bytes = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            });
            JWKS.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> JWKS.stop(0)));
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/contacts");
        registry.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        registry.add("api.auth.jwks-uri", () -> "http://127.0.0.1:" + JWKS.getAddress().getPort() + "/jwks");
    }
    @Autowired WebApplicationContext context;
    private MockMvc mvc() { return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    private String token(RSAKey key, String issuer, String audience, String company, String scope, long expiration) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject(UUID.randomUUID().toString()).issueTime(Date.from(Instant.now().minusSeconds(1200)))
                .expirationTime(Date.from(Instant.now().plusSeconds(expiration))).claim("scope", scope);
        if (company != null) claims.claim("company_id", company);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims.build());
        jwt.sign(new RSASSASigner(key)); return jwt.serialize();
    }
    private String valid() throws Exception { return token(KEY,"http://localhost:8085","zalura-contact",COMPANY.toString(),"contact:read",300); }
    @Test void headerAloneCannotAuthenticateAndValidTokenNeedsNoCompanyHeader() throws Exception {
        mvc().perform(get(URL).header("X-Company-Id", COMPANY)).andExpect(status().isUnauthorized());
        mvc().perform(get(URL).header("Authorization", "Bearer " + valid())).andExpect(status().isOk())
                ;
    }
    @Test void signedCompanyCannotBeOverriddenAndScopeIsMandatory() throws Exception {
        mvc().perform(get(URL).header("Authorization", "Bearer " + valid()).header("X-Company-Id", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        mvc().perform(get(URL).header("Authorization", "Bearer " + token(KEY,"http://localhost:8085","zalura-contact",COMPANY.toString(),"",300)))
                .andExpect(status().isForbidden());
    }
    @Test void invalidSignatureIssuerAudienceExpirationAndTenantAreRejected() throws Exception {
        for (String bad : new String[]{
                token(WRONG,"http://localhost:8085","zalura-contact",COMPANY.toString(),"contact:read",300),
                token(KEY,"https://wrong.example","zalura-contact",COMPANY.toString(),"contact:read",300),
                token(KEY,"http://localhost:8085","other-api",COMPANY.toString(),"contact:read",300),
                token(KEY,"http://localhost:8085","zalura-contact",COMPANY.toString(),"contact:read",-600),
                token(KEY,"http://localhost:8085","zalura-contact",null,"contact:read",300),
                token(KEY,"http://localhost:8085","zalura-contact","bad-uuid","contact:read",300), "malformed"}) {
            mvc().perform(get(URL).header("Authorization", "Bearer " + bad)).andExpect(status().isUnauthorized());
        }
    }
    @Test void healthRemainsPublicAndUnsupportedRoutesStayClosed() throws Exception {
        mvc().perform(get("/actuator/env").header("Authorization", "Bearer " + valid())).andExpect(status().isForbidden());
    }
    @Test void readOnlyTokenCannotWriteAnyBusinessRoute() throws Exception {
        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                    org.springframework.http.HttpMethod.valueOf(method), URL)
                    .header("Authorization", "Bearer " + valid()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
    @Autowired br.com.zaluratech.contact.ContactService contacts;
    private static final UUID WORKER_COMPANY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String WORKER_TOKEN = "test-only-worker-token-01234567890123456789";
    @Test void tenantIsolationAlsoAppliesToWritesAndInternalEligibility() throws Exception {
        var contact = contacts.create(WORKER_COMPANY, new br.com.zaluratech.contact.ContactDtos.Request(
                "Teste", "119" + String.format("%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000)), true));
        String path = "/internal/companies/" + WORKER_COMPANY + "/contacts/" + contact.id() + "/eligibility";
        mvc().perform(get(path)).andExpect(status().isUnauthorized());
        mvc().perform(get(path).header("Authorization", "Bearer " + valid())).andExpect(status().isUnauthorized());
        mvc().perform(get(path).header("Authorization", "Bearer wrong")).andExpect(status().isUnauthorized());
        mvc().perform(get(path).header("Authorization", "Bearer " + WORKER_TOKEN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contact.id().toString()))
                .andExpect(jsonPath("$.name").doesNotExist());
        mvc().perform(get(path.replace(WORKER_COMPANY.toString(), COMPANY.toString()))
                .header("Authorization", "Bearer " + WORKER_TOKEN)).andExpect(status().isForbidden());
        mvc().perform(get(URL).header("Authorization", "Bearer " + WORKER_TOKEN)).andExpect(status().isUnauthorized());
        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .header("Authorization", "Bearer " + WORKER_TOKEN)).andExpect(status().isForbidden());
        mvc().perform(get(URL + "/" + contact.id()).header("Authorization", "Bearer " + valid())).andExpect(status().isNotFound());
        String owner = token(KEY,"http://localhost:8085","zalura-contact",COMPANY.toString(),"contact:write",300);
        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(URL + "/" + contact.id())
                .header("Authorization", "Bearer " + owner)).andExpect(status().isNotFound());
    }
    @Test void ownerCanCreateWithoutCompanyHeader() throws Exception {
        String owner = token(KEY,"http://localhost:8085","zalura-contact",COMPANY.toString(),"contact:write",300);
        mvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(URL)
                .header("Authorization", "Bearer " + owner).contentType("application/json")
                .content("{\"name\":\"Teste\",\"phone\":\"11999998888\",\"marketingConsent\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.companyId").value(COMPANY.toString()));
    }
}

