package no.nav.helse.flex.melding

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.melding.domene.MeldingDbRecord
import no.nav.helse.flex.melding.domene.Variant
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class MeldingRepositoryTest : FellesTestOppsett() {
    @Test
    fun testReposistory() {
        val melding =
            MeldingDbRecord(
                fnr = "12345",
                opprettet = Instant.EPOCH,
                lukket = null,
                tekst = "Heyyy",
                lenke = "http://heisann",
                meldingType = "hoi",
                synligFremTil = Instant.now(),
                meldingUuid = UUID.randomUUID().toString(),
                variant = Variant.INFO.toString(),
                lukkbar = true,
            )
        meldingRepository.save(melding)

        meldingRepository.count() shouldBe 1

        meldingRepository.findByFnrIn(listOf("12345")).first().tekst shouldBe "Heyyy"
    }
}
