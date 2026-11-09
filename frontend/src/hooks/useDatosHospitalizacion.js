import { listarClinicas, listarEstadosHabitacion, listarEstadosHospitalizacion, listarMedicos, listarTiposHabitacion } from '../api/catalogos.js'
import { listarHabitaciones } from '../api/habitaciones.js'
import { listarPacientes } from '../api/pacientes.js'
import { idEstadoHospActiva } from '../pages/hospitalizaciones/utilsHospitalizacion.js'
import { mapaPor, useApi } from './useApi.js'

const tolerante = (promesa) => promesa.catch(() => [])

/**
 * Pacientes, médicos, habitaciones y catálogos (estados de habitación y de hospitalización, tipos de
 * habitación, clínicas) para resolver nombres y llenar selects. Un catálogo no legible queda vacío.
 */
export function useDatosHospitalizacion() {
  const { datos, cargando } = useApi('datos-hospitalizacion', () =>
    Promise.all([
      tolerante(listarPacientes({ todos: true })),
      tolerante(listarMedicos()),
      tolerante(listarHabitaciones()),
      tolerante(listarEstadosHabitacion()),
      tolerante(listarTiposHabitacion()),
      tolerante(listarEstadosHospitalizacion()),
      tolerante(listarClinicas()),
    ]),
  )
  const [pacientes, medicos, habitaciones, estadosHab, tiposHab, estadosHosp, clinicas] = datos ?? [[], [], [], [], [], [], []]
  return {
    cargando,
    pacientes,
    medicos,
    habitaciones,
    estadosHab,
    tiposHab,
    estadosHosp,
    clinicas,
    pacientesPorId: mapaPor(pacientes, 'idPaciente'),
    medicosPorId: mapaPor(medicos, 'idMedico'),
    habitacionesPorId: mapaPor(habitaciones, 'idHabitacion'),
    estadosHabPorId: mapaPor(estadosHab, 'idEstadoHabitacion'),
    tiposHabPorId: mapaPor(tiposHab, 'idTipoHabitacion'),
    estadosHospPorId: mapaPor(estadosHosp, 'idEstadoHosp'),
    clinicasPorId: mapaPor(clinicas, 'idClinica'),
    /** id del estado "Activa", resuelto por descripción como hace el backend (EstadosCatalogo), no por id fijo. */
    idEstadoActiva: idEstadoHospActiva(estadosHosp),
  }
}
