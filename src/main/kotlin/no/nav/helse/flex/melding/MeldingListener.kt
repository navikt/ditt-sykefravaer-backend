package no.nav.helse.flex.melding

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
class MeldingListener(
    val oppdaterMeldingerFraKafka: OppdaterMeldingerFraKafka,
    val environmentToggles: EnvironmentToggles
) {

    private val log = logger()

    @KafkaListener(
        topics = [dittSykefravaerMeldingTopic],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        try {
            oppdaterMeldingerFraKafka.oppdater(cr.key(), objectMapper.readValue(cr.value()))
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            if (environmentToggles.isDevelopment()) {
                log.warn("Kafka-melding med key: ${cr.key()} feilet men ble acknowledged siden dette er i DEV.", e)
                acknowledgment.acknowledge()
            } else {
                throw e
            }
        }
    }
}
