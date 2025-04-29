package no.nav.helse.flex.melding

import com.fasterxml.jackson.databind.JsonNode
import jakarta.annotation.PostConstruct
import no.nav.helse.flex.TokenValidator
import no.nav.helse.flex.exception.AbstractApiError
import no.nav.helse.flex.exception.LogLevel
import no.nav.helse.flex.melding.domene.LukkMelding
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import no.nav.helse.flex.melding.domene.MeldingRest
import no.nav.helse.flex.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.postgresql.util.PGobject
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
class MeldingApi(
    val meldingRepository: MeldingRepository,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val meldingKafkaProducer: MeldingKafkaProducer,
    @Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    val dittSykefravaerFrontendClientId: String,
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, dittSykefravaerFrontendClientId)
    }

    @GetMapping("/meldinger", produces = [APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun hentMeldinger(): List<MeldingRest> {
        val claims = tokenValidator.validerTokenXClaims()
        val fnr = tokenValidator.fnrFraIdportenTokenX(claims)
        return meldingRepository
            .findByFnrIn(listOf(fnr))
            .filter { it.synligFremTil == null || it.synligFremTil.isAfter(Instant.now()) }
            .filter { it.lukket == null }
            .map {
                MeldingRest(
                    uuid = it.meldingUuid,
                    tekst = it.tekst,
                    lenke = it.lenke,
                    // Lagret som upper-case i databasen, men frontend forventer lower-case.
                    variant = it.variant.lowercase(),
                    lukkbar = it.lukkbar,
                    meldingType = it.meldingType,
                    opprettet = it.opprettet,
                    metadata = it.metadata?.tilJsonNode(),
                )
            }
    }

    @PostMapping(value = ["/meldinger/{meldingUuid}/lukk"], produces = [APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun lukkMelding(
        @PathVariable meldingUuid: String,
    ): String {
        val claims = tokenValidator.validerTokenXClaims()
        val fnr = tokenValidator.fnrFraIdportenTokenX(claims)

        val meldingDbRecord = (
            meldingRepository
                .findByFnrIn(listOf(fnr))
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
                lukkMelding = LukkMelding(timestamp = Instant.now()),
            ),
        )
        return "lukket"
    }
}

private fun PGobject.tilJsonNode(): JsonNode = objectMapper.readTree(value)

private class FeilUuidForLukking :
    AbstractApiError(
        message = "Forsøker å lukke uuid vi ikke finner i databasen",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "FEIL_UUID_FOR_LUKKING",
        loglevel = LogLevel.WARN,
    )

private class IkkeLukkbar :
    AbstractApiError(
        message = "Forsøker å lukke melding som ikke er lukkbar",
        httpStatus = HttpStatus.BAD_REQUEST,
        reason = "IKKE_LUKKBAR",
        loglevel = LogLevel.WARN,
    )
