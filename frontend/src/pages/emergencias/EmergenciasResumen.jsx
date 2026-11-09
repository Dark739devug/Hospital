import { Link } from 'react-router-dom'
import { Col, Row } from 'reactstrap'
import { listarProcedimientos } from '../../api/procedimientos.js'
import TarjetaKpi from '../../components/TarjetaKpi.jsx'
import TriajeBadge from '../../components/TriajeBadge.jsx'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { formatearFechaHora, hoyISO } from '../../utils/formato.js'
import { nombreMedico } from '../citas/utilsCita.js'
import { etiquetaPacienteEmergencia } from './utilsEmergencia.js'

const abierta = (e) => e.activo !== false && !e.fechaHoraAlta

/** Nivel de urgencia (N1..N5) de una emergencia: el código del catálogo coincide con el nombre del triaje. */
function nivelDe(e, datos, nivelesPorCodigo) {
  const codigo = String(datos.triajesPorId[e.idTriaje]?.nivelDeEmergencia ?? '').trim().toUpperCase()
  return nivelesPorCodigo[codigo]
}

/** Indicadores y clasificador por nivel de urgencia (filtra la tabla con `onFiltrarTriaje`). */
export function EmergenciasIndicadores({ datos, emergencias, idTriaje, onFiltrarTriaje }) {
  const niveles = [...datos.niveles].filter((n) => n.activo !== false).sort((a, b) => String(a.codigo).localeCompare(String(b.codigo)))
  const nivelesPorCodigo = Object.fromEntries(niveles.map((n) => [String(n.codigo).trim().toUpperCase(), n]))
  const abiertas = (emergencias ?? []).filter(abierta)
  const criticas = abiertas.filter((e) => ['N1', 'N2'].includes(String(nivelDe(e, datos, nivelesPorCodigo)?.codigo ?? '').toUpperCase())).length
  // El sistema no registra cuándo empieza la atención: se cuentan los procedimientos realizados hoy
  const { datos: procedimientos } = useApi('procedimientos-recientes', () => listarProcedimientos().catch(() => []))
  const procedimientosHoy = (procedimientos ?? []).filter((p) => p.activo !== false && String(p.fechaHora).slice(0, 10) === hoyISO()).length
  const altasHoy = (emergencias ?? []).filter((e) => e.fechaHoraAlta && String(e.fechaHoraAlta).slice(0, 10) === hoyISO()).length
  const cargando = emergencias == null

  return (
    <>
      <Row>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'health_ambulance' }} titulo="Pacientes en atención" valor={cargando ? null : abiertas.length} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'ui-1_bell-53', alerta: true }} titulo="Críticos (N1 y N2)" valor={cargando ? null : criticas} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'files_paper' }} titulo="Procedimientos de hoy" valor={procedimientos ? procedimientosHoy : null} ruta="/procedimientos" />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'arrows-1_share-66' }} titulo="Altas de hoy" valor={cargando ? null : altasHoy} />
        </Col>
      </Row>

      {niveles.length > 0 && (
        <div className="card card-body">
          <div className="d-flex flex-wrap align-items-baseline justify-content-between mb-3">
            <h2 className="h5 mb-0">Pacientes en atención por nivel de urgencia</h2>
            {idTriaje && (
              <button type="button" className="btn btn-link text-primary p-0 small" onClick={() => onFiltrarTriaje('')}>
                Ver todos los niveles
              </button>
            )}
          </div>
          <Row>
            {niveles.map((n) => {
              const triaje = datos.triajes.find((t) => String(t.nivelDeEmergencia).trim().toUpperCase() === String(n.codigo).trim().toUpperCase())
              const cantidad = abiertas.filter((e) => nivelDe(e, datos, nivelesPorCodigo)?.idNivelUrgencia === n.idNivelUrgencia).length
              const seleccionado = triaje && idTriaje === String(triaje.idTriaje)
              return (
                <Col key={n.idNivelUrgencia} xs="6" md="4" xl className="mb-2">
                  <button
                    type="button"
                    className={`w-100 text-left border rounded p-2 bg-white${seleccionado ? ' border-primary' : ''}`}
                    aria-pressed={seleccionado}
                    disabled={!triaje}
                    onClick={() => onFiltrarTriaje(seleccionado ? '' : String(triaje.idTriaje))}
                  >
                    <div className="d-flex justify-content-between align-items-center">
                      <TriajeBadge triaje={{ nivelDeEmergencia: n.codigo }} niveles={datos.niveles} />
                      <span className="h5 mb-0">{cantidad}</span>
                    </div>
                    <div className="font-weight-bold small mt-1">{n.descripcion}</div>
                    <div className="text-muted small">{n.tiempoMaxEsperaMin === 0 ? 'Atención inmediata' : `Hasta ${n.tiempoMaxEsperaMin} min`}</div>
                  </button>
                </Col>
              )
            })}
          </Row>
        </div>
      )}
    </>
  )
}

/** Procedimientos recientes y habitaciones ocupadas por pacientes de emergencia. */
export function EmergenciasActividad({ datos, emergencias }) {
  const { datos: procedimientos } = useApi('procedimientos-recientes', () => listarProcedimientos().catch(() => []))
  const emergenciasPorId = mapaPor(emergencias ?? [], 'idEmergencia')
  const recientes = [...(procedimientos ?? [])]
    .filter((p) => p.activo !== false)
    .sort((a, b) => String(b.fechaHora).localeCompare(String(a.fechaHora)))
    .slice(0, 5)
  const enHabitacion = (emergencias ?? []).filter((e) => abierta(e) && e.idHabitacion)

  return (
    <Row>
      <Col lg="8">
        <div className="card card-body">
          <div className="d-flex align-items-baseline justify-content-between mb-3">
            <h2 className="h5 mb-0">Procedimientos recientes</h2>
            <Link to="/procedimientos" className="small">
              Ver todos
            </Link>
          </div>
          {recientes.length === 0 ? (
            <p className="text-muted mb-0">No hay procedimientos registrados.</p>
          ) : (
            <ul className="lista-divisoria">
              {recientes.map((p) => {
                const e = emergenciasPorId[p.idEmergencia]
                return (
                  <li key={p.idProcedimiento} className="d-flex justify-content-between align-items-start">
                    <div>
                      <div className="font-weight-bold">{p.descripcion}</div>
                      <div className="small">
                        <Link to={`/emergencia/${p.idEmergencia}`}>{e ? etiquetaPacienteEmergencia(e, datos.pacientesPorId) : `Emergencia #${p.idEmergencia}`}</Link>
                      </div>
                      {p.observaciones && <div className="small text-muted">{p.observaciones}</div>}
                    </div>
                    <div className="small text-muted text-right text-nowrap ml-2">
                      <div>{p.idMedico ? nombreMedico(datos.medicosPorId[p.idMedico], p.idMedico) : ''}</div>
                      <div>{formatearFechaHora(p.fechaHora)}</div>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </Col>
      <Col lg="4">
        <div className="card card-body">
          <h2 className="h5 mb-3">Habitaciones en uso</h2>
          {enHabitacion.length === 0 ? (
            <p className="text-muted mb-0">Ningún paciente de emergencia tiene habitación asignada.</p>
          ) : (
            <ul className="lista-divisoria">
              {enHabitacion.map((e) => (
                <li key={e.idEmergencia} className="d-flex justify-content-between align-items-center">
                  <span className="font-weight-bold">Habitación {datos.habitacionesPorId[e.idHabitacion]?.numero ?? `#${e.idHabitacion}`}</span>
                  <Link to={`/emergencia/${e.idEmergencia}`} className="small">
                    {etiquetaPacienteEmergencia(e, datos.pacientesPorId)}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </Col>
    </Row>
  )
}
