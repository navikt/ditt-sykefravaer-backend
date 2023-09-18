package no.nav.helse.flex.inntektsmelding

import no.nav.helse.flex.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class InntektsmeldingListener(
    val lagreInntektsmeldingerFraKafka: LagreInntektsmeldingerFraKafka
) {

    private val log = logger()

    @KafkaListener(
        topics = [inntektsmeldingTopic],
        containerFactory = "aivenKafkaListenerContainerFactory",
        groupId = "ditt-sykefravaer-test",
        properties = ["auto-offset-reset=earliest"]
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        lagreInntektsmeldingerFraKafka.oppdater(cr.value())
        acknowledgment.acknowledge()
    }
}
const val inntektsmeldingTopic = "helsearbeidsgiver." + "privat-sykepenger-inntektsmelding"
