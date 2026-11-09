import client from './client.js'

/** GET /api/reportes/resumen (ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, CAJERO) -> indicadores del día. */
export const obtenerResumen = () => client.get('/api/reportes/resumen').then((r) => r.data)

/**
 * GET /api/reportes/paciente/{id}/historial?incluirAnulados= (ADMIN, MEDICO, ENFERMERO) -> { paciente, citas, consultas,
 * diagnosticosConsulta, diagnosticos, tratamientos, medicamentosTratamiento, hospitalizaciones, emergencias, cuentas, facturas,
 * incluyeDatosFinancieros, incluirAnulados }. Un paciente inactivo también devuelve su historial; 404 solo si no existe.
 * Las cuentas y facturas solo vienen para ADMIN (el servicio también las permite a CAJERO, pero CAJERO no tiene acceso a
 * este endpoint); para MEDICO y ENFERMERO van vacías e `incluyeDatosFinancieros` es false.
 */
export const obtenerHistorialPaciente = (idPaciente, { incluirAnulados = false } = {}) =>
  client.get(`/api/reportes/paciente/${idPaciente}/historial`, { params: incluirAnulados ? { incluirAnulados: true } : {} }).then((r) => r.data)
