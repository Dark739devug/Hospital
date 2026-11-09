-- =====================================================================================================
-- Migración 003: factura asociada a una consulta (medicamentos recetados en la consulta).
--
-- Aplicar sobre una base creada previamente:
--   psql -h localhost -U postgres -d Hospital -f migraciones/003_factura_consulta.sql
-- =====================================================================================================
BEGIN;

ALTER TABLE facturas ADD COLUMN IF NOT EXISTS id_consulta BIGINT REFERENCES consultas(id_consulta);
CREATE INDEX IF NOT EXISTS idx_facturas_consulta ON facturas(id_consulta);

COMMIT;
