import { ayerISO, hoyISO } from '../../utils/formato.js'

export const SEXOS = [
  { valor: 'M', etiqueta: 'Masculino' },
  { valor: 'F', etiqueta: 'Femenino' },
]

export const nombreCompleto = (p) =>
  [p.nombre, p.apellidoPaterno, p.apellidoMaterno].filter(Boolean).join(' ')

export const FORM_VACIO = {
  dpiPaciente: '',
  nombre: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  fechaNacimiento: '',
  sexo: '',
  telefono: '',
  correoElectronico: '',
  direccion: '',
  idAlergias: '',
  idTipoDeSangre: '',
}

/** Respuesta del backend -> estado del formulario (todo como texto). */
export function pacienteAFormulario(p) {
  return {
    dpiPaciente: p.dpiPaciente ?? '',
    nombre: p.nombre ?? '',
    apellidoPaterno: p.apellidoPaterno ?? '',
    apellidoMaterno: p.apellidoMaterno ?? '',
    fechaNacimiento: p.fechaNacimiento ?? '',
    sexo: p.sexo ?? '',
    telefono: p.telefono ?? '',
    correoElectronico: p.correoElectronico ?? '',
    direccion: p.direccion ?? '',
    idAlergias: p.idAlergias != null ? String(p.idAlergias) : '',
    idTipoDeSangre: p.idTipoDeSangre != null ? String(p.idTipoDeSangre) : '',
  }
}

/** Estado del formulario -> PacienteRequest (los opcionales vacíos se envían como null). */
export function formularioAPeticion(f) {
  const textoONulo = (v) => (v.trim() === '' ? null : v.trim())
  return {
    dpiPaciente: f.dpiPaciente.trim(),
    nombre: f.nombre.trim(),
    apellidoPaterno: f.apellidoPaterno.trim(),
    apellidoMaterno: f.apellidoMaterno.trim(),
    fechaNacimiento: f.fechaNacimiento || null,
    sexo: f.sexo,
    telefono: f.telefono.trim(),
    correoElectronico: textoONulo(f.correoElectronico),
    direccion: textoONulo(f.direccion),
    idAlergias: f.idAlergias ? Number(f.idAlergias) : null,
    idTipoDeSangre: f.idTipoDeSangre ? Number(f.idTipoDeSangre) : null,
  }
}

/** Replica las validaciones del PacienteRequest (el backend siempre tiene la última palabra). */
export function validarPaciente(f) {
  const e = {}
  if (!f.dpiPaciente.trim()) e.dpiPaciente = 'El DPI es obligatorio'
  else if (!/^\d{13}$/.test(f.dpiPaciente.trim())) e.dpiPaciente = 'El DPI debe tener 13 dígitos'

  const obligatorioMax = (campo, mensaje, max) => {
    const v = f[campo].trim()
    if (!v) e[campo] = mensaje
    else if (v.length > max) e[campo] = `Máximo ${max} caracteres`
  }
  obligatorioMax('nombre', 'El nombre es obligatorio', 100)
  obligatorioMax('apellidoPaterno', 'El apellido paterno es obligatorio', 100)
  obligatorioMax('apellidoMaterno', 'El apellido materno es obligatorio', 100)
  obligatorioMax('telefono', 'El teléfono es obligatorio', 20)

  if (!f.sexo) e.sexo = 'El sexo es obligatorio'

  if (f.fechaNacimiento) {
    if (f.fechaNacimiento > ayerISO()) e.fechaNacimiento = 'La fecha de nacimiento debe ser pasada'
  }

  const correo = f.correoElectronico.trim()
  if (correo) {
    if (correo.length > 150) e.correoElectronico = 'Máximo 150 caracteres'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) e.correoElectronico = 'Correo electrónico inválido'
  }
  if (f.direccion.trim().length > 255) e.direccion = 'Máximo 255 caracteres'
  return e
}

/** Tratamiento vigente: activo y sin fecha de fin o con fin hoy o después. */
export const tratamientoVigente = (t) => t.activo !== false && (!t.fechaFin || t.fechaFin >= hoyISO())
