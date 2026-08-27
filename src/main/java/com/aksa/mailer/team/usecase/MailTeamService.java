package com.aksa.mailer.team.usecase;

import com.aksa.mailer.common.domain.NotFoundException;
import com.aksa.mailer.team.domain.MailTeam;
import com.aksa.mailer.team.port.in.GetTeamsUseCase;
import com.aksa.mailer.team.port.out.MailTeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Is mantigi. port/in'i gerceklestirir, ihtiyaclarini port/out uzerinden alir.
 * JPA veya HTTP sinifi import ETMEZ - tek Spring dokunusu @Service.
 */
@Service
public class MailTeamService implements GetTeamsUseCase {

    private final MailTeamRepository mailTeamRepository;

    public MailTeamService(MailTeamRepository mailTeamRepository) {
        this.mailTeamRepository = mailTeamRepository;
    }

    @Override
    public List<MailTeam> erisilebilirTakimlar(String sicil) {
        // Sprint 0: herkes tum aktif takimlari gorur. Takim bazli yetki
        // Sprint 2'de gelecek (yol haritasi) - token'daki teamIds ile
        // mail_teams eslestirilecek.
        return mailTeamRepository.aktifTakimlar();
    }

    @Override
    public MailTeam takim(Long id) {
        return mailTeamRepository.bul(id)
                .orElseThrow(() -> new NotFoundException("Takım bulunamadı: " + id));
    }
}
