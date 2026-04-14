package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.objectMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object InntektsmeldingJsonParser {
    fun fraJsonTilLagringsfelter(json: String): InntektsmeldingLagringsfelter {
        val inntektsmeldingNode = objectMapper.readTree(json)

        return InntektsmeldingLagringsfelter(
            inntektsmeldingId = inntektsmeldingNode.hentPakrevdTekst("inntektsmeldingId"),
            mottattDato =
                inntektsmeldingNode
                    .hentPakrevdLocalDateTime("mottattDato")
                    .atZone(ZoneId.of("Europe/Oslo"))
                    .toInstant(),
            fnr = inntektsmeldingNode.hentPakrevdTekst("arbeidstakerFnr"),
            arbeidsgivertype = inntektsmeldingNode.hentPakrevdTekst("arbeidsgivertype"),
        )
    }

    fun fraJsonTilApifelter(json: String): InntektsmeldingApifelter {
        val inntektsmeldingNode = objectMapper.readTree(json)

        return InntektsmeldingApifelter(
            inntektsmeldingId = inntektsmeldingNode.hentPakrevdTekst("inntektsmeldingId"),
            beregnetInntekt = inntektsmeldingNode.hentBigDecimal("beregnetInntekt"),
            arbeidsgiverperioder = inntektsmeldingNode.hentNodeEllerTomArray("arbeidsgiverperioder"),
            foersteFravaersdag = inntektsmeldingNode.hentLocalDate("foersteFravaersdag"),
            refusjon = inntektsmeldingNode.hentNodeEllerTomtObjekt("refusjon"),
            endringIRefusjoner = inntektsmeldingNode.hentNodeEllerTomArray("endringIRefusjoner"),
            opphoerAvNaturalytelser = inntektsmeldingNode.hentNodeEllerTomArray("opphoerAvNaturalytelser"),
            organisasjonsnavn = inntektsmeldingNode.hentTekst("virksomhetsnummer") ?: "Ukjent",
            begrunnelseForReduksjonEllerIkkeUtbetalt = inntektsmeldingNode.hentTekst("begrunnelseForReduksjonEllerIkkeUtbetalt"),
            bruttoUtbetalt = inntektsmeldingNode.hentBigDecimal("bruttoUtbetalt"),
            innsenderFulltNavn = inntektsmeldingNode.hentPakrevdTekst("innsenderFulltNavn"),
        )
    }
}

data class InntektsmeldingLagringsfelter(
    val inntektsmeldingId: String,
    val mottattDato: Instant,
    val fnr: String,
    val arbeidsgivertype: String,
)

data class InntektsmeldingApifelter(
    val inntektsmeldingId: String,
    val beregnetInntekt: BigDecimal?,
    val foersteFravaersdag: LocalDate?,
    val arbeidsgiverperioder: JsonNode,
    val endringIRefusjoner: JsonNode,
    val opphoerAvNaturalytelser: JsonNode,
    val refusjon: JsonNode,
    val organisasjonsnavn: String,
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    val bruttoUtbetalt: BigDecimal?,
    val innsenderFulltNavn: String,
)

private fun JsonNode.hentPakrevdTekst(feltnavn: String): String =
    hentTekst(feltnavn)
        ?: throw IllegalArgumentException("Mangler feltet $feltnavn i inntektsmelding")

private fun JsonNode.hentTekst(feltnavn: String): String? =
    get(feltnavn)
        ?.takeUnless { it.isNull }
        ?.asText()
        ?.takeIf { it.isNotBlank() }

private fun JsonNode.hentBigDecimal(feltnavn: String): BigDecimal? = hentTekst(feltnavn)?.let(::BigDecimal)

private fun JsonNode.hentLocalDate(feltnavn: String): LocalDate? = hentTekst(feltnavn)?.let(LocalDate::parse)

private fun JsonNode.hentPakrevdLocalDateTime(feltnavn: String): LocalDateTime = LocalDateTime.parse(hentPakrevdTekst(feltnavn))

private fun JsonNode.hentNodeEllerTomArray(feltnavn: String): JsonNode =
    get(feltnavn)?.takeUnless { it.isNull } ?: objectMapper.createArrayNode()

private fun JsonNode.hentNodeEllerTomtObjekt(feltnavn: String): JsonNode =
    get(feltnavn)?.takeUnless { it.isNull } ?: objectMapper.createObjectNode()
