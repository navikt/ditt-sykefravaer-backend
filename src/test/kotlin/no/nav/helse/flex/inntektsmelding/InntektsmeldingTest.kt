package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.objectMapper
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*
import java.util.concurrent.TimeUnit

class InntektsmeldingTest : FellesTestOppsett() {

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository

    @Autowired
    lateinit var producer: KafkaProducer<String, String>

    @BeforeEach
    fun clearDatabase() {
        inntektsmeldingRepository.deleteAll()
    }

    val fnr = "12345678787"

    @Test
    @Order(1)
    fun `Mottar inntektsmeldingmelding`() {
        setupTest()

        val melding = inntektsmeldingRepository.findByFnrIn(listOf(fnr)).first()
        melding.arbeidsgivertype `should be equal to` "VIRKSOMHET"
    }

    @Test
    @Order(2)
    fun `verifiser at getInntektsmeldinger returnerer riktig data`() {
        setupTest()

        val fetchedInntektsmeldings = hentInntektsmeldinger(fnr)
        fetchedInntektsmeldings.shouldHaveSize(1)

        val savedInntektsmelding = fetchedInntektsmeldings.first()

        savedInntektsmelding.inntektsmeldingId `should be equal to` "67e56f3c-6eee-4378-b9e3-ad8be6b7a111"
        savedInntektsmelding.fnr `should be equal to` fnr
        savedInntektsmelding.arbeidsgivertype `should be equal to` "VIRKSOMHET"
        savedInntektsmelding.inntektsmelding `should be equal to` inntektsmelding
    }

    private fun setupTest() {
        inntektsmeldingRepository.findByFnrIn(listOf(fnr)).shouldHaveSize(0)
        produserMelding(UUID.randomUUID().toString(), inntektsmelding)

        await().atMost(10, TimeUnit.SECONDS).until {
            inntektsmeldingRepository.findByFnrIn(listOf(fnr)).isNotEmpty()
        }
        inntektsmeldingRepository.findByFnrIn(listOf(fnr)).shouldHaveSize(1)
    }

    fun produserMelding(meldingUuid: String, melding: String): RecordMetadata {
        return producer.send(
            ProducerRecord(
                inntektsmeldingTopic,
                meldingUuid,
                melding
            )
        ).get()
    }

    fun hentInntektsmeldinger(fnr: String): List<InntektsmeldingDbRecord> {
        val json = mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/inntektsmeldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr)}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        return objectMapper.readValue(json)
    }
}

val inntektsmelding = """
    {
        "inntektsmeldingId": "67e56f3c-6eee-4378-b9e3-ad8be6b7a111",
        "arbeidstakerFnr": "12345678787",
        "arbeidstakerAktorId": "213456",
        "virksomhetsnummer": "910825585",
        "arbeidsgiverFnr": null,
        "arbeidsgiverAktorId": null,
        "begrunnelseForReduksjonEllerIkkeUtbetalt": "",
        "arbeidsgivertype": "VIRKSOMHET",
        "arbeidsforholdId": "123456789",
        "beregnetInntekt": "52000.00",
        "refusjon": {
            "beloepPrMnd": null,
            "opphoersdato": null
        },
        "endringIRefusjoner": [],
        "opphoerAvNaturalytelser": [],
        "gjenopptakelseNaturalytelser": [],
        "arbeidsgiverperioder": [
            {
                "fom": "2023-04-18",
                "tom": "2023-05-03"
            }
        ],
        "status": "GYLDIG",
        "arkivreferanse": "AR13764323",
        "ferieperioder": [],
        "foersteFravaersdag": "2023-04-18",
        "mottattDato": "2023-05-16T08:49:43Z",
        "naerRelasjon": true,
        "avsenderSystem": {
            "navn": "AltinnPortal",
            "versjon": "1.489"
        }
    }
""".trimIndent()
