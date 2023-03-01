package no.nav.helse.flex.melding

import no.nav.helse.flex.melding.domene.MeldingDbRecord
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MeldingRepository : CrudRepository<MeldingDbRecord, String> {
    fun findByFnrIn(fnrs: List<String>): List<MeldingDbRecord>
    @Modifying
    @Query("delete from Melding m where m.fnr = :fnr")
    fun deleteByFnr(fnr: String): Long

    fun findByMeldingUuid(meldingUuid: String): MeldingDbRecord?
}
