import { Link } from 'react-router-dom'
import { listarHospitalizacionesDePaciente } from '../../api/hospitalizaciones.js'
import { EstadoHospBadge } from '../../components/EstadosBadge.jsx'
import { useApi } from '../../hooks/useApi.js'
import { useDatosHospitalizacion } from '../../hooks/useDatosHospitalizacion.js'
import { formatearFechaHora } from '../../utils/formato.js'
import { estaActiva, nombreHabitacion } from './utilsHospitalizacion.js'

/** Sección del detalle del paciente: sus hospitalizaciones (activas primero). */
export default function PacienteHospitalizaciones({ idPaciente }) {
  const datos = useDatosHospitalizacion()
  const { datos: lista, error, cargando } = useApi(`hosp-paciente|${idPaciente}`, () => listarHospitalizacionesDePaciente(idPaciente))

  const filas = (lista ?? [])
    .filter((h) => h.activo !== false)
    .sort((a, b) => (estaActiva(a, datos.idEstadoActiva) ? -1 : 0) - (estaActiva(b, datos.idEstadoActiva) ? -1 : 0) || String(b.fechaIngreso).localeCompare(String(a.fechaIngreso)))

  return (
    <div className="card card-body">
      <h2 className="h5 mb-3">Hospitalizaciones</h2>
      {cargando || datos.cargando ? (
        <p className="text-muted mb-0">Cargando...</p>
      ) : error ? (
        <p className="text-muted mb-0">{error.message}</p>
      ) : filas.length === 0 ? (
        <p className="text-muted mb-0">El paciente no tiene hospitalizaciones registradas.</p>
      ) : (
        <ul className="lista-divisoria">
          {filas.map((h) => (
            <li key={h.idHospitalizacion} className="d-flex flex-wrap align-items-center justify-content-between">
              <Link to={`/hospitalizaciones/${h.idHospitalizacion}`} className="font-weight-bold">
                Ingreso {formatearFechaHora(h.fechaIngreso)} · {nombreHabitacion(datos.habitacionesPorId[h.idHabitacion], h.idHabitacion)}
              </Link>
              <EstadoHospBadge estado={datos.estadosHospPorId[h.idEstadoHosp]} fallback={`#${h.idEstadoHosp}`} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
