SELECT PG_DROP_REPLICATION_SLOT(slot_name)
FROM pg_replication_slots
WHERE slot_name = 'ditt_sykefravaer_backend_replication';
