package no.nav.helse.flex

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.melding.domene.Variant
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class VariantDeserializationTest {

    @Test
    fun serializeVariant() {
        Variant.INFO.serialisertTilString() `should be equal to` "\"info\""
    }

    @Test
    fun deserializeVariantLowerCase() {
        objectMapper.readValue<Variant>("\"info\"") `should be equal to` Variant.INFO
    }

    @Test
    fun deserializeVariantUpperCase() {
        objectMapper.readValue<Variant>("\"INFO\"") `should be equal to` Variant.INFO
    }
}
