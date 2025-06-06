package no.nav.helse.flex.melding

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.melding.domene.MeldingDbRecord
import no.nav.helse.flex.melding.domene.MeldingKafkaDto
import org.postgresql.util.PGobject
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OppdaterMeldingerFraKafka(
    val meldingRepository: MeldingRepository,
) {
    fun oppdater(
        meldingUuid: String,
        meldingKafkaDto: MeldingKafkaDto,
    ) {
        if (meldingKafkaDto.lukkMelding == null && meldingKafkaDto.opprettMelding == null) {
            throw RuntimeException("$meldingUuid må ha lukkMelding eller opprettMelding ")
        }
        if (meldingKafkaDto.lukkMelding != null && meldingKafkaDto.opprettMelding != null) {
            throw RuntimeException("$meldingUuid må ha enten lukkMelding eller opprettMelding ")
        }
        if (meldingKafkaDto.opprettMelding != null) {
            if (meldingRepository.findByMeldingUuid(meldingUuid) != null) {
                return
            }
            meldingRepository.save(
                MeldingDbRecord(
                    meldingUuid = meldingUuid,
                    fnr = meldingKafkaDto.fnr,
                    tekst = meldingKafkaDto.opprettMelding.tekst,
                    synligFremTil = meldingKafkaDto.opprettMelding.synligFremTil,
                    opprettet = Instant.now(),
                    lukket = null,
                    meldingType = meldingKafkaDto.opprettMelding.meldingType,
                    lenke = meldingKafkaDto.opprettMelding.lenke,
                    variant = meldingKafkaDto.opprettMelding.variant.toString(),
                    lukkbar = meldingKafkaDto.opprettMelding.lukkbar,
                    metadata = meldingKafkaDto.opprettMelding.metadata?.tilPgJson(),
                ),
            )
        }
        if (meldingKafkaDto.lukkMelding != null) {
            val dbMelding =
                meldingRepository.findByMeldingUuid(meldingUuid)
                    ?: throw RuntimeException("$meldingUuid skal eksistere i databasen!")
            if (dbMelding.lukket != null) {
                return
            }
            if (dbMelding.fnr != meldingKafkaDto.fnr) {
                throw RuntimeException("$meldingUuid har mismatch på fnr fra kafka og i db!")
            }

            meldingRepository.save(dbMelding.copy(lukket = Instant.now()))
        }
    }
}

fun JsonNode.tilPgJson(): PGobject {
    val pgobj = PGobject()
    pgobj.type = "json"
    pgobj.value = this.toString()
    return pgobj
}
