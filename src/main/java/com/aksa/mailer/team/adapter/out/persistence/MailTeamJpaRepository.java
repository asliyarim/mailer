package com.aksa.mailer.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data arayuzu. Disariya SIZMAZ - sadece adapter kullanir. */
interface MailTeamJpaRepository extends JpaRepository<MailTeamEntity, Long> {

    List<MailTeamEntity> findByActiveTrueOrderByNameAsc();
}
