package no.nav.helse.flex

import no.nav.helse.flex.melding.MeldingRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MigrerVariantTilUpperCase(
    private val meldingRepository: MeldingRepository,
    private val leaderElection: LeaderElection
) {

    private val log = logger()

    @Scheduled(initialDelay = 3, fixedDelay = 60 * 24, timeUnit = TimeUnit.MINUTES)
    fun migrer(): Long {
        return if (leaderElection.isLeader()) {
            val antallOppdatert = meldingRepository.variantTilUpperCase()
            log.info("Endret $antallOppdatert meldinger til upper-case Variant.")
            antallOppdatert
        } else {
            log.info("Er ikke leader, returnerer 0.")
            0
        }
    }
}
