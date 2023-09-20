package no.nav.helse.flex

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class ApiAuthTest : FellesTestOppsett() {

    val fnr = "12343787332"

    @Test
    fun `melding - krever riktig audience`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/meldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, audience = "facebook")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `melding - krever riktig client id`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/meldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, clientId = "facebook")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `melding - krever riktig idp`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/meldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, issuerId = "loginservice")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Inntektsmelding - krever riktig audience`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/inntektsmeldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, audience = "facebook")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Inntektsmelding - krever riktig client id`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/inntektsmeldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, clientId = "facebook")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Inntektsmelding - krever riktig idp`() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/inntektsmeldinger")
                .header("Authorization", "Bearer ${tokenxToken(fnr = fnr, issuerId = "loginservice")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `tester at henting av vedtak fungerer med gammelt acr claim`() {
        val response = authMedSpesifiktAcrClaim(fnr, "Level4")
        assertThat(response).isEqualTo("200")
    }

    @Test
    fun `tester at henting av vedtak fungerer med nytt acr claim`() {
        val response = authMedSpesifiktAcrClaim(fnr, "idporten-loa-high")
        assertThat(response).isEqualTo("200")
    }

    @Test
    fun `tester at henting av vedtak ikke fungerer med tilfeldig valgt acr claim`() {
        val response = authMedSpesifiktAcrClaim(fnr, "doNotLetMeIn")
        assertThat(response).isEqualTo("401")
    }
}
