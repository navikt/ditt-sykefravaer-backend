package no.nav.helse.flex.inntektsmelding

import no.nav.helse.flex.melding.domene.MeldingDbRecord
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LagreInntektsmeldingerFraKafka(
    val inntektsmeldingRepository: InntektsmeldingRepository
) {

    fun oppdater(key: String, value: String) {

    }
}
