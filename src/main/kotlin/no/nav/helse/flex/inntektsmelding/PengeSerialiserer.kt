package no.nav.helse.flex.inntektsmelding

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer
import java.math.BigDecimal
import java.math.RoundingMode

// Lokal Jackson 3-erstatning for PengeSerialiserer fra inntektsmelding-kontrakt,
// som er bygget med Jackson 2 og ikke er kompatibel med Spring Boot 4.
class PengeSerialiserer : StdSerializer<BigDecimal>(BigDecimal::class.java) {
    override fun serialize(
        value: BigDecimal,
        gen: JsonGenerator,
        ctxt: SerializationContext,
    ) {
        gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toString())
    }
}
