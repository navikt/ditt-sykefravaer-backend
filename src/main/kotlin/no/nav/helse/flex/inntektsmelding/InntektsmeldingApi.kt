package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.annotation.PostConstruct
import no.nav.helse.flex.TokenValidator
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.organisasjon.LeggTilOrganisasjonnavn
import no.nav.inntektsmeldingkontrakt.PengeSerialiserer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Controller
@RequestMapping("/api/v1")
class InntektsmeldingApi(
    val inntektsmeldingRepository: InntektsmeldingRepository,
    @param:Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    val dittSykefravaerFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val leggTilOrganisasjonnavn: LeggTilOrganisasjonnavn,
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator =
            TokenValidator(tokenValidationContextHolder, dittSykefravaerFrontendClientId)
    }

    @GetMapping("/inntektsmeldinger", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(
        issuer = "tokenx",
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getInntektsmeldinger(): List<RSInntektsmelding> {
        val claims = tokenValidator.validerTokenXClaims()
        val fnr = tokenValidator.fnrFraIdportenTokenX(claims)
        val inntektsmeldinger =
            inntektsmeldingRepository
                .findByFnrIn(listOf(fnr))
                .filter { it.arbeidsgivertype == "VIRKSOMHET" }
                .map { it.tilRsInntektsmelding() }

        return leggTilOrganisasjonnavn.leggTilOrganisasjonnavn(inntektsmeldinger)
    }
}

private fun InntektsmeldingDbRecord.tilRsInntektsmelding(): RSInntektsmelding {
    val inntektsmeldingNode = objectMapper.readTree(this.inntektsmelding)
    return RSInntektsmelding(
        mottattDato = this.mottattDato,
        beregnetInntekt = inntektsmeldingNode.hentBigDecimal("beregnetInntekt"),
        inntektsmeldingId = inntektsmeldingNode.hentPakrevdTekst("inntektsmeldingId"),
        arbeidsgiverperioder = inntektsmeldingNode.hentNodeEllerTomArray("arbeidsgiverperioder"),
        foersteFravaersdag = inntektsmeldingNode.hentLocalDate("foersteFravaersdag"),
        refusjon = inntektsmeldingNode.hentNodeEllerTomtObjekt("refusjon"),
        endringIRefusjoner = inntektsmeldingNode.hentNodeEllerTomArray("endringIRefusjoner"),
        opphoerAvNaturalytelser =
            inntektsmeldingNode.hentNodeEllerTomArray("opphoerAvNaturalytelser"),
        organisasjonsnavn = inntektsmeldingNode.hentTekst("virksomhetsnummer") ?: "Ukjent",
        begrunnelseForReduksjonEllerIkkeUtbetalt =
            inntektsmeldingNode.hentTekst("begrunnelseForReduksjonEllerIkkeUtbetalt"),
        bruttoUtbetalt = inntektsmeldingNode.hentBigDecimal("bruttoUtbetalt"),
        innsenderFulltNavn = inntektsmeldingNode.hentPakrevdTekst("innsenderFulltNavn"),
    )
}

data class RSInntektsmelding(
    val organisasjonsnavn: String,
    val inntektsmeldingId: String,
    @field:JsonSerialize(using = PengeSerialiserer::class)
    val beregnetInntekt: BigDecimal?,
    val foersteFravaersdag: LocalDate?,
    val mottattDato: Instant,
    val arbeidsgiverperioder: JsonNode,
    val endringIRefusjoner: JsonNode,
    val opphoerAvNaturalytelser: JsonNode,
    val refusjon: JsonNode,
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    @field:JsonSerialize(using = PengeSerialiserer::class)
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

private fun JsonNode.hentNodeEllerTomArray(feltnavn: String): JsonNode =
    get(feltnavn)?.takeUnless { it.isNull } ?: objectMapper.createArrayNode()

private fun JsonNode.hentNodeEllerTomtObjekt(feltnavn: String): JsonNode =
    get(feltnavn)?.takeUnless { it.isNull } ?: objectMapper.createObjectNode()
