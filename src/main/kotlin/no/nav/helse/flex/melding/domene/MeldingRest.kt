package no.nav.helse.flex.melding.domene

import java.time.Instant

data class MeldingRest(
    val uuid: String,
    val tekst: String,
    val lenke: String?,
    val variant: Variant,
    val lukkbar: Boolean,
    val meldingType: String,
    val opprettet: Instant,
)
