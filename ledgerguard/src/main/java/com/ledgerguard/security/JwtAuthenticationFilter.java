package com.ledgerguard.security;

import com.ledgerguard.context.TenantContext;
import com.ledgerguard.dto.AuthUserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.validateToken(token)) {
                    Claims claims = jwtUtil.extractAllClaims(token);

                    String userId = claims.get("userId", String.class);
                    String orgId = claims.get("orgId", String.class);
                    String role = claims.get("role", String.class);

                    if (orgId != null && userId != null && role != null) {
                        TenantContext.setTenantId(orgId);

                        String formattedRole = "ROLE_" + role.toUpperCase();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(formattedRole);

                        AuthUserPrincipal principal = new AuthUserPrincipal(userId, orgId, role);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}