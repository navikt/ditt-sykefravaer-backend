package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.EnvironmentToggles
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class InntektsmeldingListener(
    val lagreInntektsmeldingerFraKafka: LagreInntektsmeldingerFraKafka,
    val environmentToggles: EnvironmentToggles
) {

    val log = logger()

    @KafkaListener(
        topics = [inntektsmeldingTopic],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        if (environmentToggles.isProduction()) {
            acknowledgment.acknowledge()
            return
        }
        try {
            val deserialisertIm: Inntektsmelding = objectMapper.readValue(cr.value())
            deserialisertIm.toString()
        } catch (e: Exception) {
            log.error("Kunne ikke deserialisere inntektsmelding", e)
            acknowledgment.acknowledge()
            return
        }
        lagreInntektsmeldingerFraKafka.oppdater(cr.value())
        acknowledgment.acknowledge()
    }
}

const val inntektsmeldingTopic = "helsearbeidsgiver." + "privat-sykepenger-inntektsmelding"
