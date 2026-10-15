package br.com.zaluratech.contact.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfig {
    @Bean @Order(2)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contacts/**").hasAuthority("SCOPE_contact:read")
                        .requestMatchers(HttpMethod.POST, "/api/contacts/**").hasAuthority("SCOPE_contact:write")
                        .requestMatchers(HttpMethod.PUT, "/api/contacts/**").hasAuthority("SCOPE_contact:write")
                        .requestMatchers(HttpMethod.PATCH, "/api/contacts/**").hasAuthority("SCOPE_contact:write")
                        .requestMatchers(HttpMethod.DELETE, "/api/contacts/**").hasAuthority("SCOPE_contact:write")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .addFilterAfter(new AuthenticatedCompanyFilter(), BearerTokenAuthenticationFilter.class)
                .build();
    }
    @Bean public UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Credenciais inválidas."); };
    }
}
