package com.aksa.mailer.team.domain;

/**
 * Mail uretebilen bir takim.
 *
 * SAF is nesnesi: burada JPA, HTTP veya Spring anotasyonu YOKTUR. Bu kural
 * hexagonal mimarinin tasidigi tek gercek soz - bozulursa katmanlar birbirine
 * yapisir (bkz. docs/BRIEF.md §5).
 *
 * themeKey kodda kayitli bir temayi isaret eder ('rpa', 'is-zekasi');
 * karsiligi veritabaninda degil, render/domain/ThemeRegistry icindedir.
 */
public record MailTeam(Long id, String code, String name, String themeKey, boolean active) {
}
