package com.aksa.mailer.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HMAC imza anahtari. En az 256 bit (32 karakter) olmali ve odyssey-auth
     * servisindeki degerin AYNISI olmali - token'i o uretiyor, biz sadece
     * dogruluyoruz. Prod'da APP_JWT_SECRET env degiskeni ile ezilir.
     */
    private String secret = "dev-only-secret-change-me-please-32-bytes-min";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
