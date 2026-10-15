package br.com.zaluratech.contact.integration;
import br.com.zaluratech.contact.config.WorkerAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class WorkerAuthenticationTest {
    @Test void disabledWorkerAlwaysRejectsAndConfiguredWorkerRequiresCompanyAllowlist() throws Exception {
        var request = new MockHttpServletRequest("GET", "/internal/companies/11111111-1111-1111-1111-111111111111/contacts/11111111-1111-1111-1111-111111111111/eligibility");
        request.addHeader("Authorization", "Bearer ");
        var response = new MockHttpServletResponse();
        new WorkerAuthenticationFilter("", "").doFilter(request, response, (a,b) -> { throw new AssertionError("must not authenticate"); });
        assertThat(response.getStatus()).isEqualTo(401);
        assertThatThrownBy(() -> new WorkerAuthenticationFilter("test-only-token-with-at-least-32-characters", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
