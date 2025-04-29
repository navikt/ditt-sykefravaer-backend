package no.nav.helse.flex.inntektsmelding

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import no.nav.helse.flex.TokenValidator
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.organisasjon.LeggTilOrganisasjonnavn
import no.nav.inntektsmeldingkontrakt.*
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
    @Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    val dittSykefravaerFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val leggTilOrganisasjonnavn: LeggTilOrganisasjonnavn,
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, dittSykefravaerFrontendClientId)
    }

    @GetMapping("/inntektsmeldinger", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
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
    val im: Inntektsmelding = objectMapper.readValue(this.inntektsmelding)
    return RSInntektsmelding(
        mottattDato = this.mottattDato,
        beregnetInntekt = im.beregnetInntekt,
        inntektsmeldingId = im.inntektsmeldingId,
        arbeidsgiverperioder = im.arbeidsgiverperioder,
        foersteFravaersdag = im.foersteFravaersdag,
        refusjon = im.refusjon,
        endringIRefusjoner = im.endringIRefusjoner,
        opphoerAvNaturalytelser = im.opphoerAvNaturalytelser,
        organisasjonsnavn = im.virksomhetsnummer ?: "Ukjent",
        begrunnelseForReduksjonEllerIkkeUtbetalt = im.begrunnelseForReduksjonEllerIkkeUtbetalt,
        bruttoUtbetalt = im.bruttoUtbetalt,
        innsenderFulltNavn = im.innsenderFulltNavn,
    )
}

data class RSInntektsmelding(
    val organisasjonsnavn: String,
    val inntektsmeldingId: String,
    @field:JsonSerialize(using = PengeSerialiserer::class)
    val beregnetInntekt: BigDecimal?,
    val foersteFravaersdag: LocalDate?,
    val mottattDato: Instant,
    val arbeidsgiverperioder: List<Periode>,
    val endringIRefusjoner: List<EndringIRefusjon>,
    val opphoerAvNaturalytelser: List<OpphoerAvNaturalytelse>,
    val refusjon: Refusjon,
    val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    @field:JsonSerialize(using = PengeSerialiserer::class)
    val bruttoUtbetalt: BigDecimal?,
    val innsenderFulltNavn: String,
)
