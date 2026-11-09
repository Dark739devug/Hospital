import client from './client.js'
import { RUTAS } from './rutas.js'

/** GET /api/citas. Los filtros idPaciente, idMedico y fecha son opcionales y se combinan en el servidor. */
export const listarCitas = ({ idPaciente, idMedico, fecha } = {}) => {
  const params = {}
  if (idPaciente) params.idPaciente = idPaciente
  if (idMedico) params.idMedico = idMedico
  if (fecha) params.fecha = fecha
  return client.get(RUTAS.citas, { params }).then((r) => r.data)
}

/**
 * GET /api/citas/pendientes: citas activas en estado Programada, Confirmada o Reprogramada, ordenadas por fecha y hora.
 * Filtros opcionales y combinables: idMedico, desde y hasta (YYYY-MM-DD, inclusivos). Sin `desde` incluye las vencidas.
 */
export const listarCitasPendientes = ({ idMedico, desde, hasta } = {}) => {
  const params = {}
  if (idMedico) params.idMedico = idMedico
  if (desde) params.desde = desde
  if (hasta) params.hasta = hasta
  return client.get(`${RUTAS.citas}/pendientes`, { params }).then((r) => r.data)
}

export const obtenerCita = (id) => client.get(`${RUTAS.citas}/${id}`).then((r) => r.data)
export const crearCita = (datos) => client.post(RUTAS.citas, datos).then((r) => r.data)
export const actualizarCita = (id, datos) => client.put(`${RUTAS.citas}/${id}`, datos).then((r) => r.data)

/** PATCH /api/citas/{id}/estado/{nuevoIdEstado} (valida la máquina de estados). */
export const cambiarEstadoCita = (id, idEstado) =>
  client.patch(`${RUTAS.citas}/${id}/estado/${idEstado}`).then((r) => r.data)

/** DELETE = borrado lógico. 409 si la cita ya tiene consulta. */
export const desactivarCita = (id) => client.delete(`${RUTAS.citas}/${id}`)
