package com.aksa.mailer.team.api;

import com.aksa.mailer.team.api.dto.MailTeamResponse;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/mailer/teams
 *
 * Tum uclar /api/mailer altinda: Odyssey'in nginx'i en uzun oneki sectigi
 * icin bu blok mevcut /api/ blogunu (Capacity Planner) yener - onun
 * bozulma riski yok (bkz. docs/BRIEF.md §1).
 *
 * Cerezsiz istek buraya HIC ULASMAZ; SecurityConfig 401 doner.
 */
@RestController
@RequestMapping("/api/mailer/teams")
public class MailTeamController {

    private final GetTeamsUseCase getTeamsUseCase;

    public MailTeamController(GetTeamsUseCase getTeamsUseCase) {
        this.getTeamsUseCase = getTeamsUseCase;
    }

    @GetMapping
    public List<MailTeamResponse> takimlar(Authentication authentication) {
        // JwtCookieAuthFilter principal olarak sicil'i yaziyor.
        String sicil = authentication.getName();
        return getTeamsUseCase.erisilebilirTakimlar(sicil).stream()
                .map(MailTeamResponse::of)
                .toList();
    }
}
