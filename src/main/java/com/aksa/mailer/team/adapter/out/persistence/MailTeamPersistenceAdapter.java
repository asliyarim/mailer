package com.aksa.mailer.team.adapter.out.persistence;

import com.aksa.mailer.team.domain.MailTeam;
import com.aksa.mailer.team.port.out.MailTeamRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * port/out'un JPA gerceklemesi. Entity <-> domain cevrimi TAM BURADA olur;
 * bu sinifin disina MailTeamEntity cikmaz.
 */
@Component
class MailTeamPersistenceAdapter implements MailTeamRepository {

    private final MailTeamJpaRepository jpaRepository;

    MailTeamPersistenceAdapter(MailTeamJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<MailTeam> aktifTakimlar() {
        return jpaRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(MailTeamPersistenceAdapter::domaine)
                .toList();
    }

    @Override
    public Optional<MailTeam> bul(Long id) {
        return jpaRepository.findById(id).map(MailTeamPersistenceAdapter::domaine);
    }

    private static MailTeam domaine(MailTeamEntity entity) {
        return new MailTeam(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getThemeKey(),
                entity.isActive());
    }
}
