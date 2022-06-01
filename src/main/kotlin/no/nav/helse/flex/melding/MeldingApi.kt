package no.nav.helse.flex.melding

import no.nav.helse.flex.exception.AbstractApiError
import no.nav.helse.flex.exception.LogLevel
import no.nav.helse.flex.melding.domene.LukkMelding
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import no.nav.helse.flex.melding.domene.MeldingRest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.time.Instant

@Controller
@RequestMapping("/api/v1")
class VedtakTokenXController(
    val meldingRepository: MeldingRepository,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val meldingKafkaProducer: MeldingKafkaProducer,

    @Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    val dittSykefravaerFrontendClientId: String,

    @Value("\${DITT_SYKEFRAVAER_FRONTEND_TOKENX_IDP}")
    val dittSykefravaerFrontendTokenxIdp: String,
) {

    @GetMapping("/meldinger", produces = [APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    fun hentVedtak(): List<MeldingRest> {
        val fnr = validerTokenXClaims().fnrFraIdportenTokenX()
        return meldingRepository.findByFnrIn(listOf(fnr))
            .filter { it.synligFremTil == null || it.synligFremTil.isAfter(Instant.now()) }
            .filter { it.lukket == null }
            .map {
                MeldingRest(
                    uuid = it.meldingUuid,
                    tekst = it.tekst,
                    lenke = it.lenke,
                    variant = it.variant,
                    lukkbar = it.lukkbar,
                )
            }
    }

    @PostMapping(value = ["/meldinger/{meldingUuid}/lukk"], produces = [APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    fun lukkMelding(@PathVariable meldingUuid: String): String {
        val fnr = validerTokenXClaims().fnrFraIdportenTokenX()

        val meldingDbRecord = (
            meldingRepository.findByFnrIn(listOf(fnr))
                .firstOrNull { it.meldingUuid == meldingUuid }
                ?: throw FeilUuidForLukking()
            )
        if (!meldingDbRecord.lukkbar) {
            throw IkkeLukkbar()
        }
        meldingKafkaProducer.produserMelding(
            meldingDbRecord.meldingUuid,
            MeldingKafkaDto(
                fnr = meldingDbRecord.fnr,
                opprettMelding = null,
                lukkMelding = LukkMelding(timestamp = Instant.now())
            )
        )
        return "lukket"
    }

    private fun validerTokenXClaims(): JwtTokenClaims {
        val context = tokenValidationContextHolder.tokenValidationContext
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")
        if (clientId != dittSykefravaerFrontendClientId) {
            throw IngenTilgang("Uventet client id $clientId")
        }
        val idp = claims.getStringClaim("idp")
        if (idp != dittSykefravaerFrontendTokenxIdp) {
            // Sjekker at det var idporten som er IDP for tokenX tokenet
            throw IngenTilgang("Uventet idp $idp")
        }
        return claims
    }

    private fun JwtTokenClaims.fnrFraIdportenTokenX(): String {
        return this.getStringClaim("pid")
    }
}

private class IngenTilgang(override val message: String) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "INGEN_TILGANG",
    loglevel = LogLevel.WARN
)

private class FeilUuidForLukking : AbstractApiError(
    message = "Forsøker å lukke uuid vi ikke finner i databasen",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "FEIL_UUID_FOR_LUKKING",
    loglevel = LogLevel.WARN
)

private class IkkeLukkbar : AbstractApiError(
    message = "Forsøker å lukke melding som ikke er lukkbar",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "IKKE_LUKKBAR",
    loglevel = LogLevel.WARN
)
