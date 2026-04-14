package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class LagreInntektsmeldingerFraKafka(
    val inntektsmeldingRepository: InntektsmeldingRepository,
) {
    val log = logger()

    fun oppdater(value: String) {
        val inntektsmeldingNode = objectMapper.readTree(value)
        val inntektsmeldingId = inntektsmeldingNode.hentPakrevdTekst("inntektsmeldingId")

        if (inntektsmeldingRepository.existsByInntektsmeldingId(inntektsmeldingId)) {
            log.info("Inntektsmelding med id $inntektsmeldingId finnes allerede i databasen")
            return
        }

        inntektsmeldingRepository.save(
            InntektsmeldingDbRecord(
                inntektsmeldingId = inntektsmeldingId,
                mottattDato =
                    inntektsmeldingNode
                        .hentPakrevdLocalDateTime("mottattDato")
                        .atZone(ZoneId.of("Europe/Oslo"))
                        .toInstant(),
                opprettet = Instant.now(),
                fnr = inntektsmeldingNode.hentPakrevdTekst("arbeidstakerFnr"),
                arbeidsgivertype = inntektsmeldingNode.hentPakrevdTekst("arbeidsgivertype"),
                inntektsmelding = value,
            ),
        )
        log.info("Lagret inntektsmelding med id $inntektsmeldingId i databasen")
    }
}

private fun JsonNode.hentPakrevdTekst(feltnavn: String): String =
    get(feltnavn)
        ?.takeUnless { it.isNull }
        ?.asText()
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Mangler feltet $feltnavn i inntektsmelding")

private fun JsonNode.hentPakrevdLocalDateTime(feltnavn: String): LocalDateTime = LocalDateTime.parse(hentPakrevdTekst(feltnavn))
