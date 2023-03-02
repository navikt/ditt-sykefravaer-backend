package no.nav.helse.flex.melding

import no.nav.helse.flex.kafka.dittSykefravaerMeldingTopic
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import no.nav.helse.flex.serialisertTilString
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.stereotype.Component

@Component
class MeldingKafkaProducer(
    private val producer: KafkaProducer<String, String>
) {

    fun produserMelding(meldingUuid: String, meldingKafkaDto: MeldingKafkaDto): RecordMetadata {

        return producer.send(
            ProducerRecord(
                dittSykefravaerMeldingTopic,
                meldingUuid,
                meldingKafkaDto.serialisertTilString()
            )
        ).get()
    }
}
