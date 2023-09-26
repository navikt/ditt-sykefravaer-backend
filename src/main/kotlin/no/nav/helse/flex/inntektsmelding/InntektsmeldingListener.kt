package no.nav.helse.flex.inntektsmelding

import no.nav.helse.flex.EnvironmentToggles
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class InntektsmeldingListener(
    val lagreInntektsmeldingerFraKafka: LagreInntektsmeldingerFraKafka,
    val environmentToggles: EnvironmentToggles
) {

    @KafkaListener(
        topics = [inntektsmeldingTopic],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        if (environmentToggles.isProduction()) {
            acknowledgment.acknowledge()
            return
        }
        lagreInntektsmeldingerFraKafka.oppdater(cr.value())
        acknowledgment.acknowledge()
    }
}

const val inntektsmeldingTopic = "helsearbeidsgiver." + "privat-sykepenger-inntektsmelding"
