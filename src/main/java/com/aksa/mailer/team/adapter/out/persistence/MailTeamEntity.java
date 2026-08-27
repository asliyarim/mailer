package com.aksa.mailer.team.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * mail_teams tablosunun JPA karsiligi. Bu sinif ADAPTER katmaninda kalir -
 * domain'e sizmaz; disari cikarken MailTeam'e cevrilir.
 *
 * Sema Flyway ile yonetiliyor (V1__init.sql), Hibernate sadece dogrular
 * (ddl-auto: validate).
 */
@Entity
@Table(name = "mail_teams")
public class MailTeamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "theme_key", nullable = false)
    private String themeKey;

    @Column(nullable = false)
    private boolean active;

    protected MailTeamEntity() {
        // JPA icin
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getThemeKey() {
        return themeKey;
    }

    public boolean isActive() {
        return active;
    }
}
