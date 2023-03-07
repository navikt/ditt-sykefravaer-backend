package no.nav.helse.flex

import no.nav.helse.flex.melding.domene.MeldingDbRecord
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

private const val FNR_1 = "fnr-1"
private const val FNR_2 = "fnr-2"

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class VariantIntegrationTest : FellesTestOppsett() {

    @Autowired
    private lateinit var migrerVariantTilUpperCase: MigrerVariantTilUpperCase

    @BeforeAll
    fun `Lagre melding med både upper- og lower-case Variant i databasen`() {
        meldingRepository.deleteAll()
        meldingRepository.save(
            MeldingDbRecord(
                meldingUuid = "uuid-1",
                fnr = FNR_1,
                tekst = "",
                synligFremTil = Instant.now().plusSeconds(3600),
                opprettet = Instant.now(),
                lukket = null,
                meldingType = "",
                lenke = "",
                variant = "info",
                lukkbar = true
            )
        )
        meldingRepository.save(
            MeldingDbRecord(
                meldingUuid = "uuid-2",
                fnr = FNR_2,
                tekst = "",
                synligFremTil = Instant.now().plusSeconds(3600),
                opprettet = Instant.now(),
                lukket = null,
                meldingType = "",
                lenke = "",
                variant = "INFO",
                lukkbar = true
            )
        )
    }

    @Test
    @Order(1)
    fun `Sjekk at Variant alltid returneres som lower-case fra REST API`() {
        val meldingerFraDatabase = meldingRepository.findByFnrIn(listOf(FNR_1, FNR_2))
        meldingerFraDatabase.find { it.meldingUuid == "uuid-1" }?.variant `should be equal to` "info"
        meldingerFraDatabase.find { it.meldingUuid == "uuid-2" }?.variant `should be equal to` "INFO"

        val lagretMedLowerCase = hentMeldinger(FNR_1)
        lagretMedLowerCase.shouldHaveSize(1)
        lagretMedLowerCase.first().uuid `should be equal to` "uuid-1"
        lagretMedLowerCase.first().variant `should be equal to` "info"

        val lagretMedUpperCase = hentMeldinger(FNR_2)
        lagretMedUpperCase.shouldHaveSize(1)
        lagretMedUpperCase.first().uuid `should be equal to` "uuid-2"
        lagretMedUpperCase.first().variant `should be equal to` "info"
    }

    @Test
    @Order(2)
    fun `Migrer Variant til upper-case`() {
        val antallOppdatert = migrerVariantTilUpperCase.migrer()
        antallOppdatert `should be equal to` 2L

        val meldingerFraDatabase = meldingRepository.findByFnrIn(listOf(FNR_1, FNR_2))
        meldingerFraDatabase.find { it.meldingUuid == "uuid-1" }?.variant `should be equal to` "INFO"
        meldingerFraDatabase.find { it.meldingUuid == "uuid-2" }?.variant `should be equal to` "INFO"
    }
}
