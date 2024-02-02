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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private const val FNR_1 = "fnr-1"

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrationTest : FellesTestOppsett() {
    @Autowired
    lateinit var meldingKafkaProducer: MeldingKafkaProducer

    @Test
    @Order(1)
    fun `Mottar melding`() {
        val kafkaMelding =
            MeldingKafkaDto(
                fnr = FNR_1,
                opprettMelding =
                    OpprettMelding(
                        tekst = "Melding 1",
                        lenke = "http://www.nav.no",
                        meldingType = "whatever",
                        synligFremTil = Instant.now().plus(2, ChronoUnit.DAYS),
                        variant = Variant.INFO,
                        lukkbar = true,
                    ),
                lukkMelding = null,
            )
        val uuid = UUID.randomUUID().toString()
        meldingKafkaProducer.produserMelding(uuid, kafkaMelding)

        ventMaksTiSekunder.until {
            meldingRepository.findByFnrIn(listOf(FNR_1)).isNotEmpty()
        }

        val melding = meldingRepository.findByFnrIn(listOf(FNR_1)).first()
        melding.lukket.`should be null`()
    }

    @Test
    @Order(2)
    fun `Henter meldinger via REST API`() {
        val melding1 = hentMeldinger(FNR_1)
        melding1.shouldHaveSize(1)
        melding1.first().tekst `should be equal to` "Melding 1"
        melding1.first().variant `should be equal to` "info"
    }

    @Test
    @Order(3)
    fun `Lukk melding`() {
        val meldinger = hentMeldinger(FNR_1)
        val uuid = meldinger.first().uuid
        lukkMelding(FNR_1, uuid)
        ventMaksTiSekunder.until {
            meldingRepository.findByMeldingUuid(uuid)!!.lukket != null
        }
        hentMeldinger(FNR_1).shouldHaveSize(0)
    }

    @Test
    @Order(4)
    fun `Melding med synlig-frem-til i fortiden vil ikke bli vist`() {
        meldingRepository.findByFnrIn(listOf(FNR_1)).shouldHaveSize(1)

        val kafkaMelding =
            MeldingKafkaDto(
                fnr = FNR_1,
                opprettMelding =
                    OpprettMelding(
                        tekst = "Melding 3",
                        lenke = "http://www.nav.no",
                        meldingType = "whatever",
                        synligFremTil = Instant.now().minusSeconds(2),
                        variant = Variant.INFO,
                        lukkbar = true,
                    ),
                lukkMelding = null,
            )
        val uuid = UUID.randomUUID().toString()
        meldingKafkaProducer.produserMelding(uuid, kafkaMelding)

        ventMaksTiSekunder.until {
            meldingRepository.findByFnrIn(listOf(FNR_1)).size == 2
        }

        hentMeldinger(FNR_1).shouldHaveSize(0)
    }

    @Test
    @Order(5)
    fun `Kan ikke lukke melding tilhørende noen andre`() {
        val uuid = UUID.randomUUID().toString()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/meldinger/$uuid/lukk")
                .header("Authorization", "Bearer ${tokenxToken(FNR_1)}")
                .contentType(MediaType.APPLICATION_JSON),
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
