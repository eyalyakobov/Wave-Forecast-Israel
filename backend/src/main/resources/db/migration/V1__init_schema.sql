-- Wave Forecast Israel — initial schema
-- Place at: src/main/resources/db/migration/V1__init_schema.sql

CREATE EXTENSION IF NOT EXISTS postgis;

-- ---------------------------------------------------------------------------
-- spot: a forecast location along the Israeli Mediterranean coast.
--
-- Two geographies per spot, deliberately:
--   shore_point  — the beach itself. Used for map markers, distance-to-user
--                  queries, and display. On land.
--   marine_point — an offshore point ~3-5 km west, inside the wave model's
--                  ocean grid. Used for the Open-Meteo /v1/marine call.
--                  Querying shore_point against the marine API returns nulls.
-- ---------------------------------------------------------------------------
CREATE TABLE spot (
    id             BIGSERIAL PRIMARY KEY,
    slug           VARCHAR(50)  NOT NULL UNIQUE,
    name_he        VARCHAR(100) NOT NULL,
    name_en        VARCHAR(100) NOT NULL,
    region         VARCHAR(10)  NOT NULL
                       CHECK (region IN ('NORTH', 'CENTER', 'SOUTH')),
    shore_point    geography(Point, 4326) NOT NULL,
    marine_point   geography(Point, 4326) NOT NULL,
    display_order  SMALLINT     NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_spot_shore_point ON spot USING GIST (shore_point);
CREATE INDEX idx_spot_active_order ON spot (active, display_order);

-- ---------------------------------------------------------------------------
-- forecast_snapshot: one row per (spot, forecast hour).
--
-- Redis is the read cache; this table is the durable record. Keeping it gives
-- you (a) graceful degradation when Open-Meteo is unreachable, and (b) a real
-- training set later for the ML component — you can compare what was forecast
-- N days out against what was forecast at T-0.
--
-- model_source records which Open-Meteo model served the value. The European
-- 5 km model covers the Mediterranean for ~3 days, then the API falls back to
-- the 28 km global model. Store it so the UI can show confidence honestly.
-- ---------------------------------------------------------------------------
--
-- Column types: DOUBLE PRECISION, not NUMERIC. Hibernate maps a Java Double to
-- `double precision`; with `ddl-auto: validate` a NUMERIC(4,2) column fails
-- schema validation at startup. The cost is floating-point rather than exact
-- decimal storage, which is irrelevant for model output only accurate to
-- ~0.05 m. (If these were money the answer would be the opposite.)
--
-- Likewise the direction columns are INTEGER, not SMALLINT, to match the
-- boxed Integer fields on the entity, and `visibility` is DOUBLE PRECISION —
-- Open-Meteo returns metres as a float and ForecastSnapshotBatchImpl calls
-- setDouble on it.
-- ---------------------------------------------------------------------------
CREATE TABLE forecast_snapshot (
    id                        BIGSERIAL PRIMARY KEY,
    spot_id                   BIGINT      NOT NULL REFERENCES spot(id) ON DELETE CASCADE,
    forecast_time             TIMESTAMPTZ NOT NULL,
    fetched_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    model_source              VARCHAR(40),

    -- combined wave (weighted mean of all components)
    wave_height               DOUBLE PRECISION,
    wave_direction            INTEGER,
    wave_period               DOUBLE PRECISION,

    -- swell: long-period energy from distant systems
    swell_wave_height         DOUBLE PRECISION,
    swell_wave_direction      INTEGER,
    swell_wave_period         DOUBLE PRECISION,

    -- wind wave: locally generated chop
    wind_wave_height          DOUBLE PRECISION,
    wind_wave_direction       INTEGER,
    wind_wave_period          DOUBLE PRECISION,

    sea_surface_temperature   DOUBLE PRECISION,

    -- atmospheric (from /v1/forecast, same spot)
    temperature_2m            DOUBLE PRECISION,
    apparent_temperature      DOUBLE PRECISION,
    precipitation             DOUBLE PRECISION,
    weather_code              INTEGER,
    pressure_msl              DOUBLE PRECISION,
    visibility                DOUBLE PRECISION,
    wind_speed_10m            DOUBLE PRECISION,
    wind_direction_10m        INTEGER,
    wind_gusts_10m            DOUBLE PRECISION,

    CONSTRAINT uq_snapshot_spot_time UNIQUE (spot_id, forecast_time)
);

CREATE INDEX idx_snapshot_spot_time ON forecast_snapshot (spot_id, forecast_time);
CREATE INDEX idx_snapshot_fetched ON forecast_snapshot (fetched_at);

-- ---------------------------------------------------------------------------
-- Seed: 12 spots, north to south.
--
-- shore_point coordinates are approximate city/beach centroids.
-- marine_point is offset west into open water.
--
-- VERIFY BEFORE TRUSTING: run each marine_point through
--   https://marine-api.open-meteo.com/v1/marine?latitude=..&longitude=..&hourly=wave_height
-- and confirm you get numbers rather than nulls. Adjust westward if needed.
-- ---------------------------------------------------------------------------
INSERT INTO spot (slug, name_he, name_en, region, shore_point, marine_point, display_order) VALUES
('nahariya',  'נהריה',      'Nahariya',   'NORTH',
    ST_MakePoint(35.0900, 33.0060)::geography, ST_MakePoint(35.0300, 33.0060)::geography, 1),
('akko',      'עכו',        'Akko',       'NORTH',
    ST_MakePoint(35.0650, 32.9250)::geography, ST_MakePoint(35.0050, 32.9250)::geography, 2),
('haifa-bay', 'מפרץ חיפה',  'Haifa Bay',  'NORTH',
    ST_MakePoint(35.0300, 32.8300)::geography, ST_MakePoint(34.9700, 32.8600)::geography, 3),
('haifa',     'חיפה',       'Haifa',      'NORTH',
    ST_MakePoint(34.9830, 32.8300)::geography, ST_MakePoint(34.9230, 32.8300)::geography, 4),
('atlit',     'עתלית',      'Atlit',      'NORTH',
    ST_MakePoint(34.9350, 32.6900)::geography, ST_MakePoint(34.8750, 32.6900)::geography, 5),
('dor',       'דור',        'Dor',        'NORTH',
    ST_MakePoint(34.9150, 32.6170)::geography, ST_MakePoint(34.8550, 32.6170)::geography, 6),
('netanya',   'נתניה',      'Netanya',    'CENTER',
    ST_MakePoint(34.8500, 32.3215)::geography, ST_MakePoint(34.7900, 32.3215)::geography, 7),
('herzliya',  'הרצליה',     'Herzliya',   'CENTER',
    ST_MakePoint(34.7930, 32.1656)::geography, ST_MakePoint(34.7330, 32.1656)::geography, 8),
('tel-aviv',  'תל אביב',    'Tel Aviv',   'CENTER',
    ST_MakePoint(34.7650, 32.0850)::geography, ST_MakePoint(34.7050, 32.0850)::geography, 9),
('palmachim', 'פלמחים',     'Palmachim',  'CENTER',
    ST_MakePoint(34.6970, 31.9300)::geography, ST_MakePoint(34.6370, 31.9300)::geography, 10),
('ashdod',    'אשדוד',      'Ashdod',     'SOUTH',
    ST_MakePoint(34.6350, 31.8010)::geography, ST_MakePoint(34.5750, 31.8010)::geography, 11),
('ashkelon',  'אשקלון',     'Ashkelon',   'SOUTH',
    ST_MakePoint(34.5580, 31.6690)::geography, ST_MakePoint(34.4980, 31.6690)::geography, 12);
