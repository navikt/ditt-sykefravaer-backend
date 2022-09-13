package no.nav.helse.flex

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
public class EnvironmentToggles(
    @Value("\${nais.cluster}") private val naisCluster: String
) {
    public fun isProduction() = "prod-gcp" == naisCluster

    public fun isDevelopment() = !isProduction()
}
