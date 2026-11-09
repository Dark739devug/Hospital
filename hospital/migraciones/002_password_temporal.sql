-- =====================================================================================================
-- Migración 002: columnas para contraseña temporal y expiración de 24 horas en usuarios.
--
-- Aplicar sobre una base creada previamente:
--   psql -h localhost -U postgres -d Hospital -f migraciones/002_password_temporal.sql
-- =====================================================================================================
BEGIN;

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS fecha_expiracion_password TIMESTAMP;

COMMIT;

