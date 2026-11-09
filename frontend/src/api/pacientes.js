import client from './client.js'
import { RUTAS } from './rutas.js'

/** GET /api/paciente?q=&todos= (sin paginación en el backend) */
export const listarPacientes = ({ q, todos } = {}) => {
  const params = {}
  if (q && q.trim()) params.q = q.trim()
  if (todos) params.todos = true
  return client.get(RUTAS.pacientes, { params }).then((r) => r.data)
}

export const obtenerPaciente = (id) => client.get(`${RUTAS.pacientes}/${id}`).then((r) => r.data)

export const obtenerPacientePorDpi = (dpi) =>
  client.get(`${RUTAS.pacientes}/dpi/${dpi}`).then((r) => r.data)

export const crearPaciente = (datos) => client.post(RUTAS.pacientes, datos).then((r) => r.data)

export const actualizarPaciente = (id, datos) =>
  client.put(`${RUTAS.pacientes}/${id}`, datos).then((r) => r.data)

/** DELETE = borrado lógico (activo = false). Responde 204. */
export const desactivarPaciente = (id) => client.delete(`${RUTAS.pacientes}/${id}`)

// --- Resúmenes mínimos (ADMIN, CAJERO, RECEPCIONISTA): solo id, nombre completo, DPI y activo ---
const cacheResumenes = new Map()

export const guardarResumenPaciente = (r) => cacheResumenes.set(Number(r.idPaciente), r)

/** GET /api/paciente/busqueda?q= (pacientes activos; con menos de 2 caracteres devuelve []; máximo 20). */
export const buscarPacientesResumen = (q) =>
  client.get(`${RUTAS.pacientes}/busqueda`, { params: { q } }).then((r) => {
    r.data.forEach(guardarResumenPaciente)
    return r.data
  })

/** GET /api/paciente/{id}/resumen (activo o inactivo; 404 si no existe). */
export const obtenerPacienteResumen = (id) => client.get(`${RUTAS.pacientes}/${id}/resumen`).then((r) => r.data)

/** Resumen con caché; devuelve null si no se puede leer (403/404/red) para que las pantallas muestren "Paciente #id". */
export async function resumenPaciente(id) {
  const clave = Number(id)
  if (cacheResumenes.has(clave)) return cacheResumenes.get(clave)
  try {
    const r = await obtenerPacienteResumen(clave)
    guardarResumenPaciente(r)
    return r
  } catch {
    return null
  }
}
