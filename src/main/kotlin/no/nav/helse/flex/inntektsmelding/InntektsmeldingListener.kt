package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.EnvironmentToggles
import no.nav.helse.flex.kafka.dittSykefravaerMeldingTopic
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class InntektsmeldingListener(
    val lagreInntektsmeldingerFraKafka: LagreInntektsmeldingerFraKafka,
) {

    private val log = logger()

    @KafkaListener(
        topics = [dittSykefravaerMeldingTopic],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
            lagreInntektsmeldingerFraKafka.oppdater(cr.key(), cr.value())
            acknowledgment.acknowledge()

    }
}
const val inntektsmeldingTopic = "flex." + "ditt-sykefravaer-melding"
