package com.aksa.mailer.team.port.in;

import com.aksa.mailer.team.domain.MailTeam;

import java.util.List;

/**
 * GIRIS portu: modulun disaridan cagrilabilecek islemleri.
 * Gerceklemesi usecase/ altinda, cagirani api/ altindadir.
 *
 * document/ modulu de bunu kullanir (tema anahtarini cozmek icin) - baska
 * bir modulun port/out'una veya JPA repository'sine ASLA dogrudan gidilmez.
 */
public interface GetTeamsUseCase {

    /** Verilen kullanicinin erisebildigi aktif takimlar. */
    List<MailTeam> erisilebilirTakimlar(String sicil);

    /** Tek takim. Yoksa NotFoundException. */
    MailTeam takim(Long id);
}
