package br.com.zaluratech.contact.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
@Configuration
public class WorkerSecurityConfig {
    @Bean @Order(1)
    public SecurityFilterChain workerSecurity(HttpSecurity http, @Value("${worker.auth.token:}") String token,
            @Value("${worker.auth.companies:}") String companies) throws Exception {
        return http.securityMatcher("/internal/**").csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable).logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("CONTACT_ELIGIBILITY"))
                .addFilterBefore(new WorkerAuthenticationFilter(token, companies), AnonymousAuthenticationFilter.class).build();
    }
}
