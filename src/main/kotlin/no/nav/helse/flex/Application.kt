package no.nav.helse.flex

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets

@SpringBootApplication
@EnableJwtTokenValidation
@EnableScheduling
class Application {

    @Bean
    fun plainTextUtf8RestTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .messageConverters(StringHttpMessageConverter(StandardCharsets.UTF_8))
            .build()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
