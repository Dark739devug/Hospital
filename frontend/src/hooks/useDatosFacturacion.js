import { listarEstadosFactura, listarMetodosPago } from '../api/catalogos.js'
import { resumenPaciente } from '../api/pacientes.js'
import { mapaPor, useApi } from './useApi.js'

const tolerante = (promesa) => promesa.catch(() => [])

/**
 * Métodos de pago y estados de factura (para selects y etiquetas) y los nombres de los pacientes cuyos ids se pasan.
 * Los nombres se resuelven con GET /api/paciente/{id}/resumen (permitido a ADMIN, CAJERO y RECEPCIONISTA) y no con
 * el listado completo de pacientes, que el rol CAJERO no puede leer. Si un resumen falla (403/404) ese paciente queda
 * sin nombre y las pantallas muestran "Paciente #id".
 * `pacientesPorId[id]` tiene la forma { idPaciente, nombre, dpiPaciente } compatible con `nombrePaciente`.
 */
export function useDatosFacturacion(idsPacientes = []) {
  const { datos, cargando } = useApi('datos-facturacion', () => Promise.all([tolerante(listarMetodosPago()), tolerante(listarEstadosFactura())]))
  const [metodos, estados] = datos ?? [[], []]

  const ids = [...new Set(idsPacientes.filter(Boolean).map(Number))].sort((a, b) => a - b)
  const { datos: resumenes } = useApi(`nombres-pacientes|${ids.join(',')}`, () => Promise.all(ids.map((id) => resumenPaciente(id))))
  const pacientesPorId = Object.fromEntries(
    (resumenes ?? [])
      .filter(Boolean)
      .map((r) => [r.idPaciente, { idPaciente: r.idPaciente, nombre: r.nombreCompleto, dpiPaciente: r.dpiPaciente, activo: r.activo }]),
  )

  return {
    cargando,
    metodos,
    estados,
    pacientesPorId,
    metodosPorId: mapaPor(metodos, 'idMetodoPago'),
    estadosPorId: mapaPor(estados, 'idEstadoFactura'),
    /** id del estado por descripción ('Pendiente', 'Pagada', 'Cancelada'). */
    idEstado: (descripcion) => estados.find((e) => e.descripcion?.toLowerCase() === descripcion.toLowerCase())?.idEstadoFactura,
  }
}
