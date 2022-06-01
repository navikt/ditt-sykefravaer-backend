package no.nav.helse.flex.melding.domene

data class MeldingRest(
    val uuid: String,
    val tekst: String,
    val lenke: String?,
    val variant: Variant,
    val lukkbar: Boolean,
)
