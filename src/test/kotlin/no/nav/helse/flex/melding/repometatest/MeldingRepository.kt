package no.nav.helse.flex.melding.repometatest

import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface MeldingRepositoryMeta : CrudRepository<MeldingDbRecord, String> {
    fun findByFnrIn(fnrs: List<String>): List<MeldingDbRecord>
}

@Table("melding")
data class MeldingDbRecord(
    @Id
    val id: String? = null,
    val meldingUuid: String,
    val fnr: String,
    val tekst: String,
    val lenke: String?,
    val meldingType: String,
    val variant: String,
    val lukkbar: Boolean,
    val opprettet: Instant,
    val synligFremTil: Instant?,
    val lukket: Instant?,
    val metadata: PGobject? = null,
)
