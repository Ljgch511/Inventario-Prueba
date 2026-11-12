package com.globalr.inventory.infrastructure.security;

import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.Country;
import com.globalr.inventory.domain.repository.CountryRepository;
import com.globalr.inventory.infrastructure.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CountryRepository countryRepository;

    public JwtAuthenticationFilter(JwtService jwtService, CountryRepository countryRepository) {
        this.jwtService = jwtService;
        this.countryRepository = countryRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Claims claims = jwtService.parseClaims(token);

                UUID userId = jwtService.extractUuid(claims, "uid");
                String username = claims.getSubject();
                String fullName = claims.get("fullName", String.class);
                Role role = jwtService.extractRole(claims);
                UUID countryId = jwtService.extractUuid(claims, "cid");
                String countryCode = claims.get("ccode", String.class);
                boolean multiCountry = Boolean.TRUE.equals(claims.get("mca", Boolean.class));

                // Allow switching country context ONLY for explicitly authorized multi-country admins
                String requestedCountry = request.getHeader("X-Country-Code");
                if (multiCountry && requestedCountry != null && !requestedCountry.isBlank()) {
                    Country override = countryRepository.findByCodeIgnoreCase(requestedCountry.trim()).orElse(null);
                    if (override != null) {
                        countryId = override.getId();
                        countryCode = override.getCode();
                    }
                }

                String channel = request.getHeader("X-Client-Channel");
                if (channel == null || channel.isBlank()) {
                    channel = "WEB";
                }
                String clientIp = request.getHeader("X-Real-IP");
                if (clientIp == null || clientIp.isBlank()) {
                    clientIp = request.getRemoteAddr();
                }

                TenantContext.set(new TenantContext.PrincipalInfo(
                        userId, username, fullName, role, countryId, countryCode, multiCountry, channel, clientIp
                ));

                var auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            chain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
