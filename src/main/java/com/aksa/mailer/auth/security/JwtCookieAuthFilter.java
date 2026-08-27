package com.aksa.mailer.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * access_token httpOnly cookie'sini okuyup gecerliyse SecurityContext'e
 * (rol dahil) authentication yazar. Authorization header kullanilmiyor -
 * tasima tamamen cookie ile yapiliyor.
 */
public class JwtCookieAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtCookieAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        findCookie(request, CookieNames.ACCESS_TOKEN)
                .flatMap(jwtTokenProvider::parse)
                .ifPresent(claims -> {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(claims.sicil(), null, authorities);
                    authentication.setDetails(claims);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        filterChain.doFilter(request, response);
    }

    private Optional<String> findCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return List.of(request.getCookies()).stream()
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
