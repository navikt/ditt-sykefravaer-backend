package no.nav.helse.flex.inntektsmelding

import no.nav.helse.flex.logger
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LagreInntektsmeldingerFraKafka(
    val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    val log = logger()

    fun oppdater(value: String) {
        val lagringsfelter = InntektsmeldingJsonParser.fraJsonTilLagringsfelter(value)

        if (inntektsmeldingRepository.existsByInntektsmeldingId(lagringsfelter.inntektsmeldingId)) {
            log.info("Inntektsmelding med id ${lagringsfelter.inntektsmeldingId} finnes allerede i databasen")
            return
        }

        inntektsmeldingRepository.save(
            InntektsmeldingDbRecord(
                inntektsmeldingId = lagringsfelter.inntektsmeldingId,
                mottattDato = lagringsfelter.mottattDato,
                opprettet = Instant.now(),
                fnr = lagringsfelter.fnr,
                arbeidsgivertype = lagringsfelter.arbeidsgivertype,
                inntektsmelding = value,
            ),
        )
        log.info("Lagret inntektsmelding med id ${lagringsfelter.inntektsmeldingId} i databasen")
    }
}
