package br.com.zaluratech.contact.integration;

import br.com.zaluratech.contact.ContactDtos;
import br.com.zaluratech.contact.ContactService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "worker.auth.token=test-only-worker-token-01234567890123456789",
        "worker.auth.companies=11111111-1111-1111-1111-111111111111"})
class WorkerHttpSecurityIntegrationTest {
    private static final String TOKEN = "test-only-worker-token-01234567890123456789";
    private static final UUID COMPANY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String SCHEMA = "worker_http_" + UUID.randomUUID().toString().replace("-", "");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/contacts");
        registry.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.flyway.schemas", () -> SCHEMA);
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }
    @LocalServerPort int port;
    @Autowired ContactService contacts;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private String path(UUID company, UUID contact) {
        return "/internal/companies/" + company + "/contacts/" + contact + "/eligibility";
    }
    private HttpResponse<String> request(String method, String path, String credential) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10)).method(method, HttpRequest.BodyPublishers.noBody());
        if (credential != null) builder.header("Authorization", "Bearer " + credential);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void preservesForbiddenStatusAfterContainerErrorDispatch() throws Exception {
        var response = request("GET", path(UUID.randomUUID(), UUID.randomUUID()), TOKEN);
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).doesNotContain(TOKEN);
        assertThat(request("POST", path(COMPANY, UUID.randomUUID()), TOKEN).statusCode()).isEqualTo(403);
    }
    @Test void missingContactIsNotMistakenForAuthenticationFailure() throws Exception {
        assertThat(request("GET", path(COMPANY, UUID.randomUUID()), TOKEN).statusCode()).isEqualTo(404);
    }
    @Test void validAccessStillWorksAndErrorPathIsNotPublic() throws Exception {
        var contact = contacts.create(COMPANY, new ContactDtos.Request("Fixture", "11999998888", true));
        assertThat(request("GET", path(COMPANY, contact.id()), TOKEN).statusCode()).isEqualTo(200);
        assertThat(request("GET", path(COMPANY, contact.id()), null).statusCode()).isEqualTo(401);
        assertThat(request("GET", path(COMPANY, contact.id()), "wrong").statusCode()).isEqualTo(401);
        assertThat(request("GET", "/error", null).statusCode()).isEqualTo(401);
        assertThat(request("GET", "/api/contacts", TOKEN).statusCode()).isEqualTo(401);
    }
}
