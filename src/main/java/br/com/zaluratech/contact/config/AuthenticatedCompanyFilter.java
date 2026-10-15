package br.com.zaluratech.contact.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticatedCompanyFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token && authentication.isAuthenticated()) {
            UUID company;
            try { company = UUID.fromString(token.getToken().getClaimAsString("company_id")); }
            catch (RuntimeException ex) { response.sendError(401); return; }
            String requested = request.getHeader("X-Company-Id");
            if (requested != null) {
                try {
                    UUID header = UUID.fromString(requested);
                    if (!header.toString().equals(requested)) { response.sendError(400); return; }
                    if (!company.equals(header)) { response.sendError(403); return; }
                } catch (IllegalArgumentException ex) { response.sendError(400); return; }
            }
            request.setAttribute("authenticatedCompanyId", company);
        }
        chain.doFilter(request, response);
    }
}
