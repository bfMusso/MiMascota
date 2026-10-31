-- Esquema inicial del MVP: usuarios, autenticación, placas, mascotas y escaneos.
-- Las tablas de fase 2 y 3 (invitacion, traspaso, alerta, suscripcion, adopción) llegan en migraciones posteriores.

CREATE TABLE usuario (
    id                UUID PRIMARY KEY,
    email             VARCHAR(254) NOT NULL,
    password_hash     VARCHAR(100) NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    telefono          VARCHAR(30),
    mostrar_contacto  BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verificado  BOOLEAN      NOT NULL DEFAULT FALSE,
    rol               VARCHAR(10)  NOT NULL DEFAULT 'USER' CHECK (rol IN ('USER', 'ADMIN')),
    fecha_alta        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email));

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    usuario_id  UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expira      TIMESTAMPTZ  NOT NULL,
    revocado    BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_alta  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);

CREATE TABLE codigo_verificacion (
    id           UUID PRIMARY KEY,
    usuario_id   UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo         VARCHAR(20)  NOT NULL CHECK (tipo IN ('VERIFICAR_EMAIL', 'RESET', 'TRASPASO')),
    codigo_hash  VARCHAR(100) NOT NULL,
    expira       TIMESTAMPTZ  NOT NULL,
    intentos     SMALLINT     NOT NULL DEFAULT 0,
    usado        BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_alta   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_codigo_verificacion_usuario_tipo ON codigo_verificacion (usuario_id, tipo);

CREATE TABLE mascota (
    id                UUID PRIMARY KEY,
    nombre            VARCHAR(60)   NOT NULL,
    especie           VARCHAR(30)   NOT NULL,
    raza              VARCHAR(60),
    sexo              VARCHAR(10)   CHECK (sexo IN ('MACHO', 'HEMBRA')),
    fecha_nacimiento  DATE,
    color             VARCHAR(60),
    foto              VARCHAR(100),
    info_medica       VARCHAR(1000),
    observaciones     VARCHAR(1000),
    perdida           BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Campos que el dueño decide mostrar en la ficha pública. Nombre y especie se muestran siempre.
    campos_publicos   VARCHAR(20)[] NOT NULL DEFAULT ARRAY['FOTO', 'RAZA', 'COLOR']::VARCHAR(20)[],
    fecha_alta        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE mascota_usuario (
    mascota_id  UUID        NOT NULL REFERENCES mascota (id) ON DELETE CASCADE,
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol         VARCHAR(15) NOT NULL CHECK (rol IN ('TITULAR', 'CORESPONSABLE')),
    PRIMARY KEY (mascota_id, usuario_id)
);
CREATE INDEX ix_mascota_usuario_usuario ON mascota_usuario (usuario_id);

CREATE TABLE placa (
    codigo      VARCHAR(12) PRIMARY KEY,
    estado      VARCHAR(12) NOT NULL DEFAULT 'SIN_ACTIVAR' CHECK (estado IN ('SIN_ACTIVAR', 'ACTIVA', 'BLOQUEADA')),
    lote        VARCHAR(40) NOT NULL,
    usuario_id  UUID        REFERENCES usuario (id) ON DELETE SET NULL,
    mascota_id  UUID        REFERENCES mascota (id) ON DELETE SET NULL,
    fecha_alta  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_placa_lote ON placa (lote);
CREATE INDEX ix_placa_usuario ON placa (usuario_id);
CREATE INDEX ix_placa_mascota ON placa (mascota_id);

CREATE TABLE escaneo (
    id            UUID PRIMARY KEY,
    placa_codigo  VARCHAR(12)      NOT NULL REFERENCES placa (codigo) ON DELETE CASCADE,
    fecha         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    lat           DOUBLE PRECISION CHECK (lat BETWEEN -90 AND 90),
    lng           DOUBLE PRECISION CHECK (lng BETWEEN -180 AND 180),
    mensaje       VARCHAR(500),
    contacto      VARCHAR(100),
    -- Se completa en fase 2, cuando exista la tabla alerta.
    alerta_id     UUID
);
CREATE INDEX ix_escaneo_placa_fecha ON escaneo (placa_codigo, fecha DESC);
