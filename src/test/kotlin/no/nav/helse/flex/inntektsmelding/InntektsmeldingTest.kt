package no.nav.helse.flex.inntektsmelding

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.TimeUnit

class InntektsmeldingTest : FellesTestOppsett() {

    @Autowired
    lateinit var inntektsmeldingRepository: InntektsmeldingRepository

    @Autowired
    lateinit var producer: KafkaProducer<String, String>

    val fnr = "12345678787"

    @Test
    @Order(1)
    fun `Mottar inntektsmeldingmelding`() {
        inntektsmeldingRepository.findByFnrIn(listOf(fnr)).shouldHaveSize(0)
        produserMelding(UUID.randomUUID().toString(), inntektsmelding)

        await().atMost(5, TimeUnit.SECONDS).until {
            inntektsmeldingRepository.findByFnrIn(listOf(fnr)).isNotEmpty()
        }
        inntektsmeldingRepository.findByFnrIn(listOf(fnr)).shouldHaveSize(1)

        val melding = inntektsmeldingRepository.findByFnrIn(listOf(fnr)).first()
        melding.arbeidsgivertype `should be equal to` "VIRKSOMHET"
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
        "mottattDato": "2023-05-16T08:49:43",
        "naerRelasjon": true,
        "avsenderSystem": {
            "navn": "AltinnPortal",
            "versjon": "1.489"
        }
    }
""".trimIndent()
