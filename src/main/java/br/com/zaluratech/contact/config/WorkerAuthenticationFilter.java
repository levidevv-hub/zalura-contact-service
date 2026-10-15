package br.com.zaluratech.contact.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class WorkerAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern ROUTE = Pattern.compile("^/internal/companies/([0-9a-f-]{36})/contacts/([0-9a-f-]{36})/eligibility$");
    private final byte[] tokenHash;
    private final Set<UUID> companies;
    public WorkerAuthenticationFilter(String token, String allowedCompanies) {
        if (!token.isEmpty() && token.length() < 32) throw new IllegalArgumentException("Credencial do worker exige pelo menos 32 caracteres.");
        companies = allowedCompanies.isBlank() ? Set.of() : Arrays.stream(allowedCompanies.split(","))
                .map(String::strip).map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
        if (!token.isEmpty() && companies.isEmpty()) throw new IllegalArgumentException("Configure empresas permitidas para o worker.");
        tokenHash = token.isEmpty() ? null : hash(token);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (tokenHash == null || header == null || !header.startsWith("Bearer ")
                || !MessageDigest.isEqual(tokenHash, hash(header.substring(7)))) { response.sendError(401); return; }
        var match = ROUTE.matcher(request.getRequestURI());
        if (!"GET".equals(request.getMethod()) || !match.matches()) { response.sendError(403); return; }
        UUID company;
        try { company = UUID.fromString(match.group(1)); }
        catch (IllegalArgumentException ex) { response.sendError(403); return; }
        if (!companies.contains(company)) { response.sendError(403); return; }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("dispatch-worker", null,
                List.of(new SimpleGrantedAuthority("CONTACT_ELIGIBILITY"))));
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
    private static byte[] hash(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível."); }
    }
}
