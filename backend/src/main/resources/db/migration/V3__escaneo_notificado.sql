-- Si el escaneo generó un email al dueño: sirve para no mandar más de uno por hora cuando la mascota no está perdida.
ALTER TABLE escaneo ADD COLUMN notificado BOOLEAN NOT NULL DEFAULT FALSE;
