package no.nav.helse.flex

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.melding.MeldingRepository
import no.nav.helse.flex.melding.domene.MeldingRest
import no.nav.helse.flex.organisasjon.OrganisasjonRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.Awaitility.await
import org.awaitility.core.ConditionFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FellesTestOppsett {
    @Autowired
    lateinit var meldingRepository: MeldingRepository

    @Autowired
    lateinit var organisasjonRepository: OrganisasjonRepository

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    companion object {
        init {
            val threads = mutableListOf<Thread>()

            thread {
                KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.1")).apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                }
            }.also { threads.add(it) }

            thread {
                PostgreSQLContainer14().apply {
                    // Cloud SQL har wal_level = 'logical' på grunn av flagget cloudsql.logical_decoding i
                    // naiserator.yaml. Vi må sette det samme lokalt for at flyway migrering skal fungere.
                    withCommand("postgres", "-c", "wal_level=logical")
                    start()
                    System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                    System.setProperty("spring.datasource.username", username)
                    System.setProperty("spring.datasource.password", password)
                }
            }.also { threads.add(it) }

            threads.forEach { it.join() }
        }

        val ventMaksTiSekunder: ConditionFactory = await().atMost(10, TimeUnit.SECONDS)
    }

    @AfterAll
    fun `Vi resetter databasen`() {
        meldingRepository.deleteAll()
    }

    fun hentMeldinger(fnr: String): List<MeldingRest> {
        val json =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/meldinger")
                    .header("Authorization", "Bearer ${tokenxToken(fnr)}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        return objectMapper.readValue(json)
    }

    fun lukkMelding(
        fnr: String,
        id: String,
    ): String {
        val json =
            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/meldinger/$id/lukk")
                    .header("Authorization", "Bearer ${tokenxToken(fnr)}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

        return json
    }

    fun authMedSpesifiktAcrClaim(
        fnr: String,
        acrClaim: String,
    ): String {
        val responseCode =
            mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/meldinger")
                    .header("Authorization", "Bearer ${tokenxToken(fnr, acrClaim)}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andReturn().response.status

        return responseCode.toString()
    }

    fun tokenxToken(
        fnr: String,
        acrClaim: String = "Level4",
        audience: String = "ditt-sykefravaer-backend-client-id",
        issuerId: String = "tokenx",
        clientId: String = "frontend-client-id",
        claims: Map<String, Any> =
            mapOf(
                "acr" to acrClaim,
                "idp" to "idporten",
                "client_id" to clientId,
                "pid" to fnr,
            ),
    ): String {
        return server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
    }
}
