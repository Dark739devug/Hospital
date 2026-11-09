-- =====================================================================================================
-- Migración 001 (2026-11-07): restricciones e índices que faltaban en la base.
--
-- Aplicar sobre una base creada con Hosp_postgres.sql (anterior a este cambio):
--   psql -h localhost -U postgres -d Hospital -f migraciones/001_restricciones_e_indices.sql
-- Es idempotente: se puede ejecutar más de una vez sin error. Una base NUEVA no la necesita, porque
-- Hosp_postgres.sql ya incluye estos cambios.
--
-- Las reglas ya las valida el backend; tenerlas también en la base evita datos inválidos cuando dos
-- peticiones llegan al mismo tiempo (el backend responde 409 si la base rechaza la operación).
-- =====================================================================================================
BEGIN;

-- Unicidades que el backend valida sin distinguir mayúsculas -----------------------------------------
-- Triaje: no se repite el nivel (N1, N2...)
CREATE UNIQUE INDEX IF NOT EXISTS ux_triaje_nivel ON triaje (lower(trim(nivel_de_emergencia)));
-- Médico: el correo es opcional, pero si se indica no se repite
CREATE UNIQUE INDEX IF NOT EXISTS ux_medico_email ON medico (lower(email))
    WHERE email IS NOT NULL AND email <> '';

-- Cuentas: estado válido y saldo no negativo -----------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cuentas_estado') THEN
        ALTER TABLE cuentas ADD CONSTRAINT ck_cuentas_estado CHECK (estado IN ('Abierta', 'Cerrada', 'Liquidada'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cuentas_saldo') THEN
        ALTER TABLE cuentas ADD CONSTRAINT ck_cuentas_saldo CHECK (saldo >= 0);
    END IF;
END $$;

-- Índices para las consultas frecuentes del backend ----------------------------------------------------
-- Agenda del médico y chequeo de choque de horario (id_medico + fecha + hora)
CREATE INDEX IF NOT EXISTS ix_citas_medico_fecha ON citas (id_medico, fecha, hora);
-- Citas del día (Inicio, filtros por fecha, agenda)
CREATE INDEX IF NOT EXISTS ix_citas_fecha ON citas (fecha);
-- Filtro de habitaciones por tipo
CREATE INDEX IF NOT EXISTS ix_habitacion_tipo ON habitacion (id_tipo_habitacion);
-- Tablas de enlace: la llave primaria empieza por la otra columna, así que la búsqueda por esta no la usa
CREATE INDEX IF NOT EXISTS ix_medico_esp_medico ON medico_especialidades (id_medico);
CREATE INDEX IF NOT EXISTS ix_diag_consulta_consulta ON diagnostico_consulta (id_consulta);
CREATE INDEX IF NOT EXISTS ix_trat_med_tratamiento ON tratamiento_medicamento (id_tratamiento);

COMMIT;
