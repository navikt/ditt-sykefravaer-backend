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

    val fnr_1 = "12343787332"
    val fnr_2 = "12343787333"

    @Test
    @Order(1)
    fun `mottar melding`() {
        val kafkaMelding = MeldingKafkaDto(
            fnr = fnr_1,
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
            meldingRepository.findByFnrIn(listOf(fnr_1)).isNotEmpty()
        }

        val melding = meldingRepository.findByFnrIn(listOf(fnr_1)).first()
        melding.lukket.`should be null`()
    }

    @Test
    @Order(1)
    fun `Mottar melding med upper case enum-verdier`() {
        val kafkaMeldingSomString =
            "{\"opprettMelding\":{\"tekst\":\"Sjekk denne meldinga\",\"lenke\":\"http://www.nav.no\",\"variant\":\"INFO\",\"lukkbar\":true,\"meldingType\":\"whatever\",\"synligFremTil\":\"2023-03-02T12:25:53.522821Z\"},\"lukkMelding\":null,\"fnr\":\"12343787333\"}"

        val uuid = "3f98fdcb-6abf-48ce-bced-21993ead3f50"
        meldingKafkaProducer.produserMelding(uuid, kafkaMeldingSomString)

        await().atMost(5, TimeUnit.SECONDS).until {
            meldingRepository.findByFnrIn(listOf(fnr_2)).isNotEmpty()
        }

        val melding = meldingRepository.findByFnrIn(listOf(fnr_2)).first()
        melding.lukket.`should be null`()
    }

    @Test
    @Order(2)
    fun `henter melding fra apiet`() {
        val meldinger = hentMeldinger(fnr_1)
        meldinger.shouldHaveSize(1)
        meldinger.first().tekst `should be equal to` "Sjekk denne meldinga"
    }

    @Test
    @Order(3)
    fun `Vi lukker meldinga`() {
        val meldinger = hentMeldinger(fnr_1)
        val uuid = meldinger.first().uuid
        lukkMelding(fnr_1, uuid)
        await().atMost(5, TimeUnit.SECONDS).until {
            meldingRepository.findByMeldingUuid(uuid)!!.lukket != null
        }
        hentMeldinger(fnr_1).shouldHaveSize(0)
    }

    @Test
    @Order(4)
    fun `en melding med synlig frem til i fortiden vil ikke bli vist`() {
        meldingRepository.findByFnrIn(listOf(fnr_1)).shouldHaveSize(1)

        val kafkaMelding = MeldingKafkaDto(
            fnr = fnr_1,
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
            meldingRepository.findByFnrIn(listOf(fnr_1)).size == 2
        }

        hentMeldinger(fnr_1).shouldHaveSize(0)
    }

    @Test
    fun `Kan ikke lukke random melding`() {
        val uuid = UUID.randomUUID().toString()

        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/meldinger/$uuid/lukk")
                .header("Authorization", "Bearer ${tokenxToken(fnr_1)}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
