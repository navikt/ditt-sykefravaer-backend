package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId

@Component
class LagreInntektsmeldingerFraKafka(
    val inntektsmeldingRepository: InntektsmeldingRepository
) {

    val log = logger()

    fun oppdater(value: String) {
        val deserialisertIm: Inntektsmelding = objectMapper.readValue(value)
        if (inntektsmeldingRepository.existsByInntektsmeldingId(deserialisertIm.inntektsmeldingId)) {
            log.info("Inntektsmelding med id ${deserialisertIm.inntektsmeldingId} finnes allerede i databasen")
            return
        }

        inntektsmeldingRepository.save(
            InntektsmeldingDbRecord(
                inntektsmeldingId = deserialisertIm.inntektsmeldingId,
                mottattDato = deserialisertIm.mottattDato.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
                opprettet = Instant.now(),
                fnr = deserialisertIm.arbeidstakerFnr,
                arbeidsgivertype = deserialisertIm.arbeidsgivertype.toString(),
                inntektsmelding = value
            )
        )
        log.info("Lagret inntektsmelding med id ${deserialisertIm.inntektsmeldingId} i databasen")
    }
}
