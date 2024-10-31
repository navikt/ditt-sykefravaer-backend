package no.nav.helse.flex

import no.nav.helse.flex.melding.MeldingKafkaProducer
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import no.nav.helse.flex.melding.domene.OpprettMelding
import no.nav.helse.flex.melding.domene.Variant
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MeldingMedMetadataTest : FellesTestOppsett() {
    @Autowired
    lateinit var meldingKafkaProducer: MeldingKafkaProducer

    val fnr = "1234"

    @Test
    @Order(1)
    fun `Mottar melding`() {
        val kafkaMelding =
            MeldingKafkaDto(
                fnr = fnr,
                opprettMelding =
                    OpprettMelding(
                        tekst = "Melding 1",
                        lenke = "http://www.nav.no",
                        meldingType = "whatever",
                        synligFremTil = Instant.now().plus(2, ChronoUnit.DAYS),
                        variant = Variant.INFO,
                        lukkbar = true,
                        metadata = objectMapper.createObjectNode().put("ol", "1994"),
                    ),
                lukkMelding = null,
            )
        val uuid = UUID.randomUUID().toString()
        meldingKafkaProducer.produserMelding(uuid, kafkaMelding)

        ventMaksTiSekunder.until {
            meldingRepository.findByFnrIn(listOf(fnr)).isNotEmpty()
        }

        val melding = meldingRepository.findByFnrIn(listOf(fnr)).first()
        melding.lukket.`should be null`()
    }

    @Test
    @Order(2)
    fun `Henter meldinger via REST API`() {
        val melding1 = hentMeldinger(fnr)
        melding1.shouldHaveSize(1)
        melding1.first().tekst `should be equal to` "Melding 1"
        melding1.first().variant `should be equal to` "info"
        melding1.first().metadata.toString() `should be equal to` """{"ol":"1994"}"""
    }
}
