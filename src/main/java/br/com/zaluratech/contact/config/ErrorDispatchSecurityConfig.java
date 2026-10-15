package br.com.zaluratech.contact.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ErrorDispatchSecurityConfig {
    @Bean
    @Order(0)
    public SecurityFilterChain errorDispatchSecurity(HttpSecurity http) throws Exception {
        // Only the container's error rendering dispatch, never a direct HTTP request.
        // Do not decode the worker credential as a user JWT while rendering an error.
        return http.securityMatcher(request -> request.getDispatcherType() == DispatcherType.ERROR
                        && "/error".equals(request.getServletPath()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
