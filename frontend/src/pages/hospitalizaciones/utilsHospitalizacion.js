import { aInputFechaHora, deInputFechaHora } from '../../utils/formato.js'

// El backend resuelve el estado "Activa" por su descripción (EstadosCatalogo.HOSP_ACTIVA), no por un id fijo.
export const DESCRIPCION_HOSP_ACTIVA = 'activa'

/** id del estado "Activa" dentro del catálogo estados-hospitalizacion (undefined si no se pudo leer). */
export const idEstadoHospActiva = (estadosHosp = []) =>
  estadosHosp.find((e) => String(e.descripcion ?? '').trim().toLowerCase() === DESCRIPCION_HOSP_ACTIVA)?.idEstadoHosp

/**
 * La hospitalización está en curso (estado "Activa"). Si no se conoce el id de "Activa" (catálogo no legible),
 * se usa la ausencia de fecha de alta, que el backend fija al dar de alta.
 */
export const estaActiva = (h, idActiva) => (idActiva != null ? h.idEstadoHosp === idActiva : !h.fechaAlta)

export const nombreHabitacion = (h, id) => (h ? `Habitación ${h.numero}` : `Habitación #${id}`)

export const FORM_HOSP_VACIO = {
  idPaciente: '',
  idMedicoResponsable: '',
  idHabitacion: '',
  idClinica: '',
  fechaIngreso: '',
  motivoIngreso: '',
  diagnosticoIngreso: '',
}

export function hospitalizacionAFormulario(h) {
  const id = (v) => (v != null ? String(v) : '')
  return {
    idPaciente: id(h.idPaciente),
    idMedicoResponsable: id(h.idMedicoResponsable),
    idHabitacion: id(h.idHabitacion),
    idClinica: id(h.idClinica),
    fechaIngreso: aInputFechaHora(h.fechaIngreso),
    motivoIngreso: h.motivoIngreso ?? '',
    diagnosticoIngreso: h.diagnosticoIngreso ?? '',
  }
}

/**
 * Formulario -> HospitalizacionesDTO. El estado y `activo` los fija el servidor; en edición el
 * backend ignora la fecha de ingreso.
 */
export function formularioAHospitalizacion(f) {
  const numeroONulo = (v) => (v ? Number(v) : null)
  const textoONulo = (v) => (v.trim() === '' ? null : v.trim())
  return {
    idPaciente: numeroONulo(f.idPaciente),
    idMedicoResponsable: numeroONulo(f.idMedicoResponsable),
    idHabitacion: numeroONulo(f.idHabitacion),
    idClinica: numeroONulo(f.idClinica),
    // Si no se indica al ingresar, el servidor usa la fecha y hora actuales.
    fechaIngreso: deInputFechaHora(f.fechaIngreso),
    motivoIngreso: f.motivoIngreso.trim(),
    diagnosticoIngreso: textoONulo(f.diagnosticoIngreso),
  }
}

export function validarHospitalizacion(f) {
  const e = {}
  if (!f.idPaciente) e.idPaciente = 'Seleccione el paciente'
  if (!f.idMedicoResponsable) e.idMedicoResponsable = 'Seleccione el médico responsable'
  if (!f.idHabitacion) e.idHabitacion = 'Toda hospitalización debe tener una habitación'
  if (!f.motivoIngreso.trim()) e.motivoIngreso = 'El motivo de ingreso es obligatorio'
  return e
}
