DROP TABLE melding;

CREATE TABLE melding
(
    id              VARCHAR DEFAULT uuid_generate_v4() PRIMARY KEY,
    melding_uuid    VARCHAR                  NOT NULL UNIQUE,
    fnr             VARCHAR                  NOT NULL,
    tekst           VARCHAR                  NOT NULL,
    lenke           VARCHAR                  NOT NULL,
    melding_type    VARCHAR                  NOT NULL,
    variant         VARCHAR                  NOT NULL,
    lukkbar         BOOLEAN                  NOT NULL,
    opprettet       TIMESTAMP WITH TIME ZONE NOT NULL,
    synlig_frem_til TIMESTAMP WITH TIME ZONE,
    lukket          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX melding_fnr_index ON melding (fnr);
