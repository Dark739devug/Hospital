import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Col, Row } from 'reactstrap'
import { listarAsignaciones } from '../../api/asignaciones.js'
import { listarEnfermeros, listarTurnos } from '../../api/catalogos.js'
import Badge from '../../components/Badge.jsx'
import TarjetaKpi from '../../components/TarjetaKpi.jsx'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { formatearFecha, formatearHora, formatearMoneda, hoyISO } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import { estaActiva } from './utilsHospitalizacion.js'

const TONO_ESTADO = { disponible: 'verde', ocupada: 'azul', mantenimiento: 'ambar' }
const nombreEstado = (estado) => String(estado?.nombre ?? '').trim()
const diasDesde = (fecha) => Math.max(0, Math.floor((Date.now() - new Date(fecha).getTime()) / 86400000))

/**
 * Censo de camas: indicadores de ocupación, habitaciones por piso con sus pacientes hospitalizados,
 * enfermería asignada a las hospitalizaciones en curso y tarifa diaria por tipo de habitación.
 * `datos` = useDatosHospitalizacion(); `hospitalizaciones` = GET /api/hospitalizaciones.
 */
export default function CensoHabitaciones({ datos, hospitalizaciones, puedeIngresar }) {
  const [piso, setPiso] = useState('')

  const { datos: enfermeria } = useApi('censo-enfermeria', async () => {
    const [asignaciones, enfermeros, turnos] = await Promise.all([
      listarAsignaciones().catch(() => []),
      listarEnfermeros().catch(() => []),
      listarTurnos().catch(() => []),
    ])
    return { asignaciones, enfermerosPorId: mapaPor(enfermeros, 'idEnfermero'), turnosPorId: mapaPor(turnos, 'idTurno') }
  })

  const habitaciones = useMemo(
    () => datos.habitaciones.filter((h) => h.activo !== false).sort((a, b) => (a.piso ?? 0) - (b.piso ?? 0) || String(a.numero).localeCompare(String(b.numero), 'es', { numeric: true })),
    [datos.habitaciones],
  )
  const activas = useMemo(
    () => (hospitalizaciones ?? []).filter((h) => h.activo !== false && estaActiva(h, datos.idEstadoActiva)),
    [hospitalizaciones, datos.idEstadoActiva],
  )
  const pisos = [...new Set(habitaciones.map((h) => h.piso).filter((p) => p != null))].sort((a, b) => a - b)
  const visibles = habitaciones.filter((h) => piso === '' || String(h.piso) === piso)

  const pacientesEn = (idHabitacion) => activas.filter((h) => h.idHabitacion === idHabitacion)
  const estadoDe = (h) => nombreEstado(datos.estadosHabPorId[h.idEstadoHabitacion])
  const cuentaEstado = (nombre) => habitaciones.filter((h) => estadoDe(h).toLowerCase() === nombre).length

  const capacidad = habitaciones.reduce((total, h) => total + (h.capacidad ?? 0), 0)
  const ocupacion = capacidad ? Math.round((activas.length / capacidad) * 100) : 0
  const altasHoy = (hospitalizaciones ?? []).filter((h) => h.fechaAlta && String(h.fechaAlta).slice(0, 10) === hoyISO()).length

  // Enfermería: asignaciones activas de hospitalizaciones en curso, agrupadas por enfermero
  const idsActivas = new Set(activas.map((h) => h.idHospitalizacion))
  const porEnfermero = new Map()
  for (const a of enfermeria?.asignaciones ?? []) {
    if (a.activo === false || !idsActivas.has(a.idHospitalizacion)) continue
    if (!porEnfermero.has(a.idEnfermero)) porEnfermero.set(a.idEnfermero, [])
    porEnfermero.get(a.idEnfermero).push(a)
  }
  const habitacionDeHosp = mapaPor(activas, 'idHospitalizacion')

  return (
    <>
      <Row>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'business_bank' }} titulo={`Ocupación (${activas.length} de ${capacidad} camas)`} valor={datos.cargando ? null : `${ocupacion}%`} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'ui-1_check' }} titulo="Habitaciones disponibles" valor={datos.cargando ? null : cuentaEstado('disponible')} ruta="/habitaciones" />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'ui-2_settings-90', alerta: true }} titulo="En mantenimiento" valor={datos.cargando ? null : cuentaEstado('mantenimiento')} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'arrows-1_share-66' }} titulo="Altas de hoy" valor={hospitalizaciones ? altasHoy : null} />
        </Col>
      </Row>

      <Row>
        <Col xl="8">
          <div className="card card-body">
            <div className="d-flex flex-wrap align-items-center justify-content-between mb-3">
              <h2 className="h5 mb-0">Habitaciones</h2>
              <div className="d-flex flex-wrap acciones-filtros" role="group" aria-label="Filtrar por piso">
                <button type="button" className={`btn btn-sm btn-round ${piso === '' ? 'btn-primary' : 'btn-outline-secondary'}`} aria-pressed={piso === ''} onClick={() => setPiso('')}>
                  Todos los pisos
                </button>
                {pisos.map((p) => (
                  <button key={p} type="button" className={`btn btn-sm btn-round ${piso === String(p) ? 'btn-primary' : 'btn-outline-secondary'}`} aria-pressed={piso === String(p)} onClick={() => setPiso(String(p))}>
                    Piso {p}
                  </button>
                ))}
              </div>
            </div>

            {visibles.length === 0 ? (
              <p className="text-muted mb-0">{datos.cargando ? 'Cargando habitaciones...' : 'No hay habitaciones registradas.'}</p>
            ) : (
              <Row>
                {visibles.map((h) => {
                  const ocupantes = pacientesEn(h.idHabitacion)
                  const estado = estadoDe(h)
                  const tipo = datos.tiposHabPorId[h.idTipoHabitacion]
                  const libres = (h.capacidad ?? 0) - ocupantes.length
                  return (
                    <Col key={h.idHabitacion} md="6" className="mb-3">
                      <div className="border rounded p-3 h-100">
                        <div className="d-flex justify-content-between align-items-start">
                          <div>
                            <Link to={`/habitaciones/${h.idHabitacion}`} className="font-weight-bold">
                              Habitación {h.numero}
                            </Link>
                            <div className="small text-muted">
                              {[h.piso != null && `Piso ${h.piso}`, tipo?.descripcion, tipo?.costoDia != null && `${formatearMoneda(tipo.costoDia)} / día`].filter(Boolean).join(' · ')}
                            </div>
                          </div>
                          <Badge tono={TONO_ESTADO[estado.toLowerCase()] ?? 'gris'}>{estado || '—'}</Badge>
                        </div>
                        <div className="small text-muted mt-1">
                          {ocupantes.length} de {h.capacidad ?? 0} camas ocupadas
                        </div>

                        {ocupantes.length > 0 && (
                          <ul className="lista-divisoria small mt-2 mb-0">
                            {ocupantes.map((o) => (
                              <li key={o.idHospitalizacion}>
                                <Link to={`/hospitalizaciones/${o.idHospitalizacion}`} className="font-weight-bold">
                                  {nombrePaciente(datos.pacientesPorId[o.idPaciente], o.idPaciente)}
                                </Link>
                                {(o.diagnosticoIngreso || o.motivoIngreso) && <div>{o.diagnosticoIngreso || o.motivoIngreso}</div>}
                                <div className="text-muted">
                                  Ingreso {formatearFecha(o.fechaIngreso)} ({diasDesde(o.fechaIngreso)} días) · {nombreMedico(datos.medicosPorId[o.idMedicoResponsable], o.idMedicoResponsable)}
                                </div>
                              </li>
                            ))}
                          </ul>
                        )}

                        {puedeIngresar && libres > 0 && estado.toLowerCase() !== 'mantenimiento' && (
                          <Link to={`/hospitalizaciones/nueva?idHabitacion=${h.idHabitacion}`} className="btn btn-link text-primary p-0 mt-2 small font-weight-bold">
                            + Ingresar paciente
                          </Link>
                        )}
                      </div>
                    </Col>
                  )
                })}
              </Row>
            )}
          </div>
        </Col>

        <Col xl="4">
          <div className="card card-body">
            <h2 className="h5 mb-3">Enfermería asignada</h2>
            {porEnfermero.size === 0 ? (
              <p className="text-muted mb-0">No hay enfermeros asignados a hospitalizaciones en curso.</p>
            ) : (
              <ul className="lista-divisoria">
                {[...porEnfermero.entries()].map(([idEnfermero, asignaciones]) => {
                  const e = enfermeria.enfermerosPorId[idEnfermero]
                  const turnos = [...new Set(asignaciones.map((a) => a.idTurno))].map((id) => enfermeria.turnosPorId[id])
                  const habs = [...new Set(asignaciones.map((a) => datos.habitacionesPorId[habitacionDeHosp[a.idHospitalizacion]?.idHabitacion]?.numero).filter(Boolean))]
                  return (
                    <li key={idEnfermero}>
                      <div className="font-weight-bold">{e ? [e.nombre, e.apellidoPaterno].filter(Boolean).join(' ') : `Enfermero #${idEnfermero}`}</div>
                      <div className="small text-muted">
                        {asignaciones.length} {asignaciones.length === 1 ? 'paciente' : 'pacientes'}
                        {habs.length > 0 && ` · Hab. ${habs.join(', ')}`}
                      </div>
                      <div className="small text-muted">
                        {turnos.map((t) => (t ? `${t.descripcion} (${formatearHora(t.horaInicio)} – ${formatearHora(t.horaFin)})` : 'Turno sin nombre')).join(' · ')}
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>

          <div className="card card-body">
            <h2 className="h5 mb-3">Tarifa por tipo de habitación</h2>
            {datos.tiposHab.length === 0 ? (
              <p className="text-muted mb-0">No hay tipos de habitación registrados.</p>
            ) : (
              <ul className="lista-divisoria">
                {datos.tiposHab
                  .filter((t) => t.activo !== false)
                  .map((t) => (
                    <li key={t.idTipoHabitacion} className="d-flex justify-content-between">
                      <span>{t.descripcion}</span>
                      <span className="font-weight-bold">{formatearMoneda(t.costoDia)} / día</span>
                    </li>
                  ))}
              </ul>
            )}
          </div>
        </Col>
      </Row>
    </>
  )
}
