import client from './client.js'
import { RUTAS } from './rutas.js'

// Asignación = enfermero + hospitalización + fecha + turno. Escritura solo ADMIN.
/** GET /api/asignacion-enfermeros: todas las asignaciones (lectura: ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA). */
export const listarAsignaciones = () => client.get(RUTAS.asignacionEnfermeros).then((r) => r.data)
/** Con soloActivas=true el servidor omite las asignaciones quitadas (borrado lógico). */
export const listarAsignacionesDeHospitalizacion = (idHospitalizacion, { soloActivas } = {}) =>
  client
    .get(`${RUTAS.asignacionEnfermeros}/hospitalizacion/${idHospitalizacion}`, { params: soloActivas ? { soloActivas: true } : {} })
    .then((r) => r.data)
export const crearAsignacion = (datos) => client.post(RUTAS.asignacionEnfermeros, datos).then((r) => r.data)
/** DELETE = borrado lógico. */
export const quitarAsignacion = (id) => client.delete(`${RUTAS.asignacionEnfermeros}/${id}`)
