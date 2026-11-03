CREATE TABLE IF NOT EXISTS usuarios (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    nombre        VARCHAR(100) NOT NULL DEFAULT '',
    email         VARCHAR(254),
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE
);

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS nombre VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(254);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check;

UPDATE usuarios SET email = username || '@localhost' WHERE email IS NULL OR email = '';
UPDATE usuarios SET rol = 'USUARIO' WHERE rol = 'USER';

ALTER TABLE usuarios ALTER COLUMN email SET NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check
    CHECK (rol IN ('USUARIO', 'OPERADOR', 'ADMIN'));
CREATE UNIQUE INDEX IF NOT EXISTS usuarios_email_key ON usuarios(email);

CREATE TABLE IF NOT EXISTS estaciones (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    capacidad INT NOT NULL DEFAULT 30 CHECK (capacidad > 0),
    objetivo_bicicletas INT NOT NULL DEFAULT 0 CHECK (objetivo_bicicletas >= 0),
    objetivo_scooters INT NOT NULL DEFAULT 0 CHECK (objetivo_scooters >= 0),
    CHECK (objetivo_bicicletas + objetivo_scooters <= capacidad)
);

ALTER TABLE estaciones ADD COLUMN IF NOT EXISTS capacidad INT NOT NULL DEFAULT 30;
ALTER TABLE estaciones ADD COLUMN IF NOT EXISTS objetivo_bicicletas INT NOT NULL DEFAULT 0;
ALTER TABLE estaciones ADD COLUMN IF NOT EXISTS objetivo_scooters INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS vehiculos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(50) NOT NULL UNIQUE,
    tipo        VARCHAR(50) NOT NULL CHECK (tipo IN ('BICICLETA', 'SCOOTER')),
    estado      VARCHAR(50) NOT NULL CHECK (
                    estado IN ('DISPONIBLE', 'EN_USO', 'EN_MANTENCION', 'DADO_DE_BAJA')),
    bateria     INT NOT NULL CHECK (bateria BETWEEN 0 AND 100),
    estacion_id BIGINT REFERENCES estaciones(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS billeteras (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE RESTRICT,
    saldo      NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    activa     BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE billeteras ADD COLUMN IF NOT EXISTS activa BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO billeteras (usuario_id)
SELECT id FROM usuarios
ON CONFLICT (usuario_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS tarifas (
    id                BIGSERIAL PRIMARY KEY,
    tipo_vehiculo     VARCHAR(50) NOT NULL CHECK (tipo_vehiculo IN ('BICICLETA', 'SCOOTER')),
    precio_por_minuto NUMERIC(10, 2) NOT NULL CHECK (precio_por_minuto >= 0),
    vigente_desde     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    vigente_hasta     TIMESTAMPTZ,
    activa            BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);

CREATE TABLE IF NOT EXISTS viajes (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    vehiculo_id         BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    estacion_origen_id  BIGINT NOT NULL REFERENCES estaciones(id) ON DELETE RESTRICT,
    estacion_destino_id BIGINT REFERENCES estaciones(id) ON DELETE RESTRICT,
    iniciado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finalizado_en       TIMESTAMPTZ,
    costo               NUMERIC(10, 2),
    tarifa_id           BIGINT REFERENCES tarifas(id) ON DELETE RESTRICT,
    precio_por_minuto_aplicado NUMERIC(10, 2),
    cancelado           BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (finalizado_en IS NULL OR finalizado_en >= iniciado_en),
    CHECK (costo IS NULL OR costo >= 0),
    CHECK (precio_por_minuto_aplicado IS NULL OR precio_por_minuto_aplicado >= 0),
    CHECK (NOT cancelado OR finalizado_en IS NOT NULL)
);

ALTER TABLE viajes ADD COLUMN IF NOT EXISTS estacion_origen_id BIGINT
    REFERENCES estaciones(id) ON DELETE RESTRICT;
ALTER TABLE viajes ADD COLUMN IF NOT EXISTS estacion_destino_id BIGINT
    REFERENCES estaciones(id) ON DELETE RESTRICT;
ALTER TABLE viajes ADD COLUMN IF NOT EXISTS tarifa_id BIGINT
    REFERENCES tarifas(id) ON DELETE RESTRICT;
ALTER TABLE viajes ADD COLUMN IF NOT EXISTS precio_por_minuto_aplicado NUMERIC(10, 2);
ALTER TABLE viajes ADD COLUMN IF NOT EXISTS cancelado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS movimientos_billetera (
    id           BIGSERIAL PRIMARY KEY,
    billetera_id BIGINT NOT NULL REFERENCES billeteras(id) ON DELETE RESTRICT,
    operador_id  BIGINT REFERENCES usuarios(id) ON DELETE RESTRICT,
    tipo         VARCHAR(20) NOT NULL CHECK (tipo IN ('ABONO', 'CARGO', 'CARGO_VIAJE')),
    monto        NUMERIC(10, 2) NOT NULL CHECK (monto <> 0),
    motivo       VARCHAR(500) NOT NULL CHECK (length(trim(motivo)) > 0),
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ordenes_traslado (
    id                   BIGSERIAL PRIMARY KEY,
    solicitud_id         UUID NOT NULL UNIQUE,
    estacion_origen_id   BIGINT NOT NULL REFERENCES estaciones(id) ON DELETE RESTRICT,
    estacion_destino_id  BIGINT NOT NULL REFERENCES estaciones(id) ON DELETE RESTRICT,
    tipo_vehiculo        VARCHAR(50) NOT NULL CHECK (tipo_vehiculo IN ('BICICLETA', 'SCOOTER')),
    cantidad             INT NOT NULL CHECK (cantidad > 0),
    operador_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    creada_en            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (estacion_origen_id <> estacion_destino_id)
);

CREATE TABLE IF NOT EXISTS auditoria_estado_vehiculo (
    id              BIGSERIAL PRIMARY KEY,
    vehiculo_id     BIGINT NOT NULL REFERENCES vehiculos(id) ON DELETE RESTRICT,
    estado_anterior VARCHAR(50),
    estado_nuevo    VARCHAR(50) NOT NULL,
    operador_id     BIGINT REFERENCES usuarios(id) ON DELETE RESTRICT,
    operador        VARCHAR(100),
    fecha           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    motivo          TEXT NOT NULL CHECK (length(trim(motivo)) > 0)
);

ALTER TABLE auditoria_estado_vehiculo ADD COLUMN IF NOT EXISTS operador_id BIGINT
    REFERENCES usuarios(id) ON DELETE RESTRICT;
ALTER TABLE auditoria_estado_vehiculo ADD COLUMN IF NOT EXISTS operador VARCHAR(100);
UPDATE auditoria_estado_vehiculo a
SET operador_id = u.id
FROM usuarios u
WHERE a.operador_id IS NULL AND a.operador = u.username;

CREATE INDEX IF NOT EXISTS idx_vehiculos_estado ON vehiculos (estado);
CREATE INDEX IF NOT EXISTS idx_viajes_usuario_id ON viajes (usuario_id);
CREATE INDEX IF NOT EXISTS idx_viajes_iniciado_en ON viajes (iniciado_en);
CREATE INDEX IF NOT EXISTS idx_movimientos_billetera_fecha
    ON movimientos_billetera (billetera_id, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_vehiculo_fecha
    ON auditoria_estado_vehiculo (vehiculo_id, fecha DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_viajes_usuario_activo
    ON viajes (usuario_id) WHERE finalizado_en IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_viajes_vehiculo_activo
    ON viajes (vehiculo_id) WHERE finalizado_en IS NULL;
