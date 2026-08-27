package com.aksa.mailer.team.port.out;

import com.aksa.mailer.team.domain.MailTeam;

import java.util.List;
import java.util.Optional;

/**
 * CIKIS portu: modulun DISARIDAN ihtiyac duydugu sey.
 *
 * Arayuz burada (modulun icinde) tanimlanir, gerceklemesi adapter/out/
 * altindadir - bagimlilik yonu ICERI dogru olsun diye. usecase/ bu arayuzu
 * bilir, JPA'yi bilmez.
 */
public interface MailTeamRepository {

    List<MailTeam> aktifTakimlar();

    Optional<MailTeam> bul(Long id);
}
