package no.nav.helse.flex.inntektsmelding

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InntektsmeldingRepository : CrudRepository<InntektsmeldingDbRecord, String> {
    fun findByFnrIn(fnrs: List<String>): List<InntektsmeldingDbRecord>

    @Modifying
    @Query("DELETE FROM melding WHERE fnr = :fnr")
    fun deleteByFnr(fnr: String): Long

    fun existsByInntektsmeldingId(innteksmeldingId: String): Boolean
}
