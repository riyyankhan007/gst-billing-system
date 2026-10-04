package com.gstbilling.gst_billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(7);
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role.toUpperCase() : "ADMIN")));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, null, authorities));
            } catch (Exception ignored) { }
        }
        chain.doFilter(request, response);
    }
}
