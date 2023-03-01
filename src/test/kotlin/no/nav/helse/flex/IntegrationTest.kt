package no.nav.helse.flex

import no.nav.helse.flex.melding.MeldingKafkaProducer
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import no.nav.helse.flex.melding.domene.OpprettMelding
import no.nav.helse.flex.melding.domene.Variant
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.shouldHaveSize
import org.awaitility.Awaitility.await
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
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class IntegrationTest : FellesTestOppsett() {

    @Autowired
    lateinit var meldingKafkaProducer: MeldingKafkaProducer

    val fnr = "12343787332"

    @Test
    @Order(1)
    fun `mottar melding`() {
        val kafkaMelding = MeldingKafkaDto(
            fnr = fnr,
            opprettMelding = OpprettMelding(
                tekst = "Sjekk denne meldinga",
                lenke = "http://www.nav.no",
                meldingType = "whatever",
                synligFremTil = Instant.now().plus(2, ChronoUnit.DAYS),
                variant = Variant.info,
                lukkbar = true,
            ),
            lukkMelding = null
        )
        val uuid = UUID.randomUUID().toString()
        meldingKafkaProducer.produserMelding(uuid, kafkaMelding)

        await().atMost(5, TimeUnit.SECONDS).until {
            meldingRepository.findByFnrIn(listOf(fnr)).isNotEmpty()
        }

        val melding = meldingRepository.findByFnrIn(listOf(fnr)).first()
        melding.lukket.`should be null`()
    }

    @Test
    @Order(2)
    fun `henter melding fra apiet`() {
        val meldinger = hentMeldinger(fnr)
        meldinger.shouldHaveSize(1)
        meldinger.first().tekst `should be equal to` "Sjekk denne meldinga"
    }

    @Test
    @Order(3)
    fun `Vi lukker meldinga`() {
        val meldinger = hentMeldinger(fnr)
        val uuid = meldinger.first().uuid
        lukkMelding(fnr, uuid)
        await().atMost(5, TimeUnit.SECONDS).until {
            meldingRepository.findByMeldingUuid(uuid)!!.lukket != null
        }
        hentMeldinger(fnr).shouldHaveSize(0)
    }

    @Test
    @Order(4)
    fun `en melding med synlig frem til i fortiden vil ikke bli vist`() {
        meldingRepository.findByFnrIn(listOf(fnr)).shouldHaveSize(1)

        val kafkaMelding = MeldingKafkaDto(
            fnr = fnr,
            opprettMelding = OpprettMelding(
                tekst = "Sjekk denne meldinga",
                lenke = "http://www.nav.no",
                meldingType = "whatever",
                synligFremTil = Instant.now().minusSeconds(2),
                variant = Variant.info,
                lukkbar = true,
            ),
            lukkMelding = null
        )
        val uuid = UUID.randomUUID().toString()
        meldingKafkaProducer.produserMelding(uuid, kafkaMelding)

        await().atMost(5, TimeUnit.SECONDS).until {
            meldingRepository.findByFnrIn(listOf(fnr)).size == 2
        }

        hentMeldinger(fnr).shouldHaveSize(0)
    }

    @Test
    fun `Kan ikke lukke random melding`() {
        val uuid = UUID.randomUUID().toString()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/meldinger/$uuid/lukk")
                .header("Authorization", "Bearer ${tokenxToken(fnr)}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
