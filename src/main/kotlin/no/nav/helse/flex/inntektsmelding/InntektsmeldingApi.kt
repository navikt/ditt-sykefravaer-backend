package no.nav.helse.flex.inntektsmelding

import jakarta.annotation.PostConstruct
import no.nav.helse.flex.TokenValidator
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1")
class InntektsmeldingApi(
    val inntektsmeldingRepository: InntektsmeldingRepository,
    @Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    val dittSykefravaerFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, dittSykefravaerFrontendClientId)
    }

    @GetMapping("/inntektsmeldinger", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun getInntektsmeldinger(): List<InntektsmeldingDbRecord> {
        val claims = tokenValidator.validerTokenXClaims()
        val fnr = tokenValidator.fnrFraIdportenTokenX(claims)
        return inntektsmeldingRepository.findByFnrIn(listOf(fnr))
    }
}
