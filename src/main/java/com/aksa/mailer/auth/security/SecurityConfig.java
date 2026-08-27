package com.aksa.mailer.auth.security;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
                // Spring'in yerlesik CSRF'i yerine CsrfCookieFilter (double-submit) kullaniliyor
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // ERROR dispatch'i serbest birak. Gerekcesi:
                        // Spring'in kendi urettigi hata yanitlari (eksik
                        // parametre, bozuk JSON = 400) govdesiz doner ve
                        // container bunlari /error'a FORWARD eder. Guvenlik
                        // zinciri o forward'da ikinci kez calisir, ama
                        // JwtCookieAuthFilter bir OncePerRequestFilter oldugu
                        // icin ERROR dispatch'te ATLANIR - kimlik olmadigindan
                        // anyRequest().authenticated() devreye girer ve 400
                        // istemciye 401 olarak ulasir.
                        //
                        // Sonucu agir: dogrulama hatasi alan frontend "oturum
                        // dustu" sanip refresh dener, sonra giris ekranina atar.
                        // Gercek hata hicbir zaman gorunmez.
                        //
                        // Yetki kaybi yok: ERROR dispatch'e ancak ASIL istek
                        // guvenlik zincirinden gectikten sonra gelinir.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // /api/auth/** bu serviste yok: Odyssey o yolu odyssey-auth'a
                        // proxy'liyor (bkz. Odyssey nginx.conf).
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtCookieAuthFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), JwtCookieAuthFilter.class);

        return http.build();
    }
}
