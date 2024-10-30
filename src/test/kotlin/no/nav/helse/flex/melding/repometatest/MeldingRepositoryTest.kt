package no.nav.helse.flex.melding.repometatest

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.melding.domene.Variant
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

class MeldingRepositoryTest : FellesTestOppsett() {
    @Autowired
    lateinit var meldingRepositoryMeta: MeldingRepositoryMeta

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
                metadata = mapOf("hei" to 34).tilPgJson(),
            )
        meldingRepositoryMeta.save(melding)

        meldingRepositoryMeta.count() `should be equal to` 1

        val first = meldingRepositoryMeta.findByFnrIn(listOf("12345")).first()
        first.tekst `should be equal to` "Heyyy"
        first.metadata!!.value `should be equal to` """{"hei": 34}"""
    }
}

private fun Any.tilPgJson(): PGobject {
    val pg = PGobject()
    pg.type = "json"
    pg.value = this.serialisertTilString()
    return pg
}
