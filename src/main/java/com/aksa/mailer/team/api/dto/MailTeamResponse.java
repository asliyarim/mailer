package com.aksa.mailer.team.api.dto;

import com.aksa.mailer.team.domain.MailTeam;

/**
 * HTTP yaniti. Domain nesnesini DOGRUDAN dondurmuyoruz: domain degisince
 * API sozlesmesi (docs/api.md) sessizce degismesin diye.
 */
public record MailTeamResponse(Long id, String code, String name, String themeKey) {

    public static MailTeamResponse of(MailTeam team) {
        return new MailTeamResponse(team.id(), team.code(), team.name(), team.themeKey());
    }
}
