package no.nav.helse.flex.organisasjon

import no.nav.helse.flex.inntektsmelding.RSInntektsmelding
import no.nav.helse.flex.logger
import org.springframework.stereotype.Component

@Component
class LeggTilOrganisasjonnavn(
    private val organisasjonRepository: OrganisasjonRepository
) {
    val log = logger()

    fun leggTilOrganisasjonnavn(inntektsmeldingene: List<RSInntektsmelding>): List<RSInntektsmelding> {
        val orgnummerene = inntektsmeldingene
            .mapNotNull { it.organisasjonsnavn }
            .toSet()

        val organisasjoner = assosierOrgNummerMedOrgNavn(orgnummerene)

        return inntektsmeldingene.map {
            val orgnavn = organisasjoner[it.organisasjonsnavn]
            if (orgnavn != null) {
                it.copy(organisasjonsnavn = orgnavn)
            } else {
                it
            }
        }
    }

    private fun assosierOrgNummerMedOrgNavn(orgnummere: Set<String>) =
        organisasjonRepository.findByOrgnummerIn(orgnummere).associate { it.orgnummer to it.navn }
}
