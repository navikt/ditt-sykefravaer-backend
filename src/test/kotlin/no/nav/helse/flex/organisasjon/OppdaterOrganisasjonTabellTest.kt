package no.nav.helse.flex.organisasjon

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykepengesoknad.kafka.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OppdaterOrganisasjonTabellTest : FellesTestOppsett() {
    @Autowired
    lateinit var producer: KafkaProducer<String, String>

    @Test
    fun `Oppretter ny organisasjon hvis den ikke finnes fra før`() {
        organisasjonRepository.deleteAll()

        val soknad =
            SykepengesoknadDTO(
                fnr = "bla",
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.NY,
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now(),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                arbeidsgiver = ArbeidsgiverDTO(navn = "Bedriften AS", orgnummer = "123456547"),
            )

        organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!).shouldBeNull()

        sendSykepengesoknad(soknad)

        ventMaksTiSekunder.until {
            organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!) != null
        }

        val org = organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!
        org.orgnummer `should be equal to` soknad.arbeidsgiver!!.orgnummer!!
        org.navn `should be equal to` soknad.arbeidsgiver!!.navn!!
        org.oppdatertAv `should be equal to` soknad.id
    }

    @Test
    fun `Den andre av to like meldinger blir ikke prosessert`() {
        organisasjonRepository.deleteAll()

        val soknad =
            SykepengesoknadDTO(
                fnr = "bla",
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.NY,
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now(),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                arbeidsgiver = ArbeidsgiverDTO(navn = "Bedriften AS", orgnummer = "123456547"),
            )

        organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!).shouldBeNull()

        sendSykepengesoknad(soknad)
        ventMaksTiSekunder.until {
            organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!) != null
        }
        val org = organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!

        sendSykepengesoknad(soknad)

        val soknad2 =
            soknad.copy(
                id = UUID.randomUUID().toString(),
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "Bedriften AS Medssfdsdf nytt navn :)",
                        orgnummer = "0002",
                    ),
            )
        sendSykepengesoknad(soknad2)
        ventMaksTiSekunder.until {
            organisasjonRepository.findByOrgnummer(soknad2.arbeidsgiver!!.orgnummer!!) != null
        }

        val org2 = organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!
        org2.oppdatert `should be equal to` org.oppdatert
    }

    @Test
    fun `Oppdaterer organisasjon hvis den finnes fra før`() {
        organisasjonRepository.deleteAll()

        val soknad =
            SykepengesoknadDTO(
                fnr = "bla",
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.NY,
                fom = LocalDate.now().minusDays(1),
                tom = LocalDate.now(),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                arbeidsgiver = ArbeidsgiverDTO(navn = "Bedriften AS", orgnummer = "1234534"),
            )

        organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!).shouldBeNull()

        sendSykepengesoknad(soknad)

        ventMaksTiSekunder.until {
            organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!) != null
        }

        val org = organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!
        org.navn `should be equal to` soknad.arbeidsgiver!!.navn!!

        val soknad2 =
            soknad.copy(
                id = UUID.randomUUID().toString(),
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "Bedriften AS Med nytt navn :)",
                        orgnummer = "1234534",
                    ),
            )
        sendSykepengesoknad(soknad2)

        ventMaksTiSekunder.until {
            organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!.navn == "Bedriften AS Med nytt navn :)"
        }

        val orgEtterOppdatering = organisasjonRepository.findByOrgnummer(soknad.arbeidsgiver!!.orgnummer!!)!!
        orgEtterOppdatering.navn `should be equal to` "Bedriften AS Med nytt navn :)"
        orgEtterOppdatering.oppdatertAv `should be equal to` soknad2.id
    }

    fun sendSykepengesoknad(soknad: SykepengesoknadDTO) {
        producer
            .send(
                ProducerRecord(
                    FLEX_SYKEPENGESOKNAD_TOPIC,
                    null,
                    soknad.id,
                    soknad.serialisertTilString(),
                ),
            ).get()
    }
}
