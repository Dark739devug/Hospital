import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { cambiarEstadoCita, listarCitasPendientes } from '../../api/citas.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge from '../../components/Badge.jsx'
import CalendarioMes from '../../components/CalendarioMes.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import EstadoCitaBadge from '../../components/EstadoCitaBadge.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosCita } from '../../hooks/useDatosCita.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFecha, formatearHora, hoyISO } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from './utilsCita.js'

// Estados desde los que se puede pasar a "Confirmada" (máquina de estados de estado-cita).
const CONFIRMABLES = ['programada', 'reprogramada']

/** 'YYYY-MM-DD' -> 'lunes 9 de noviembre' (sin conversión de zona horaria). */
function nombreDia(fecha) {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  const texto = new Date(anio, mes - 1, dia).toLocaleDateString('es-GT', { weekday: 'long', day: 'numeric', month: 'long' })
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

function sumarDias(fecha, dias) {
  const [anio, mes, dia] = fecha.split('-').map(Number)
  const d = new Date(anio, mes - 1, dia + dias)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function etiquetaDia(fecha, hoy) {
  if (fecha === hoy) return 'Hoy'
  if (fecha === sumarDias(hoy, 1)) return 'Mañana'
  if (fecha < hoy) return 'Vencida'
  return null
}

export default function AgendaCitas() {
  const { rol } = useAuth()
  const puedeConfirmar = puede(rol, 'agenda', 'confirmar')
  const hoy = hoyISO()

  const [idMedico, setIdMedico] = useState('')
  const [hasta, setHasta] = useState('')
  const [incluirVencidas, setIncluirVencidas] = useState(true)
  const [texto, setTexto] = useState('')
  const [confirmando, setConfirmando] = useState(null)
  const [exito, setExito] = useState('')
  const [errorAccion, setErrorAccion] = useState('')
  const [mes, setMes] = useState(hoy.slice(0, 7))
  const [diaSeleccionado, setDiaSeleccionado] = useState('')

  const datosCita = useDatosCita()
  // Sin "desde" el backend incluye las pendientes de días anteriores (vencidas).
  const desde = incluirVencidas ? '' : hoy
  const { datos, error, cargando, recargar } = useApi(`citas-pendientes|${idMedico}|${desde}|${hasta}`, () =>
    listarCitasPendientes({ idMedico, desde, hasta }),
  )

  const idConfirmada = datosCita.estados.find((e) => e.descripcion?.toLowerCase() === 'confirmada')?.idEstado

  const citas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    return (datos ?? []).filter((c) => {
      if (!busqueda) return true
      const paciente = nombrePaciente(datosCita.pacientesPorId[c.idPaciente], c.idPaciente)
      const medico = nombreMedico(datosCita.medicosPorId[c.idMedico], c.idMedico)
      return `${paciente} ${medico} ${c.motivo ?? ''}`.toLowerCase().includes(busqueda)
    })
  }, [datos, texto, datosCita])

  // Citas por día para marcar el calendario.
  const conteos = useMemo(() => {
    const mapa = {}
    for (const c of citas) mapa[c.fecha] = (mapa[c.fecha] ?? 0) + 1
    return mapa
  }, [citas])

  // El backend ya las entrega en orden cronológico: se agrupan por día conservando ese orden.
  // Si se eligió un día en el calendario, solo se muestra ese.
  const dias = useMemo(() => {
    const grupos = []
    for (const c of citas) {
      if (diaSeleccionado && c.fecha !== diaSeleccionado) continue
      const ultimo = grupos[grupos.length - 1]
      if (ultimo?.fecha === c.fecha) ultimo.citas.push(c)
      else grupos.push({ fecha: c.fecha, citas: [c] })
    }
    return grupos
  }, [citas, diaSeleccionado])

  function seleccionarDia(fecha) {
    setDiaSeleccionado(fecha)
    if (fecha) setMes(fecha.slice(0, 7))
  }

  const resumen = useMemo(
    () => ({
      vencidas: citas.filter((c) => c.fecha < hoy).length,
      hoy: citas.filter((c) => c.fecha === hoy).length,
      proximas: citas.filter((c) => c.fecha > hoy).length,
    }),
    [citas, hoy],
  )

  async function confirmar(cita) {
    setConfirmando(cita.idCita)
    setErrorAccion('')
    setExito('')
    try {
      await cambiarEstadoCita(cita.idCita, idConfirmada)
      setExito(`Cita de ${nombrePaciente(datosCita.pacientesPorId[cita.idPaciente], cita.idPaciente)} confirmada.`)
      recargar()
    } catch (e) {
      // 422: transición no permitida por la máquina de estados; 403: rol sin permiso
      setErrorAccion(normalizarError(e).message)
    } finally {
      setConfirmando(null)
    }
  }

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Agenda</h1>
          <p className="text-muted mb-0">Citas pendientes de atender: programadas, confirmadas y reprogramadas.</p>
        </div>
        <Link to="/citas" className="btn btn-outline-primary btn-round mt-3 mt-md-0">
          Ver todas las citas
        </Link>
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message || errorAccion} onCerrar={errorAccion ? () => setErrorAccion('') : undefined} />

      <div className="card card-body">
        <div className="agenda-filtros">
          <input
            type="search"
            aria-label="Buscar en la agenda"
            className="form-control"
            placeholder="Buscar por paciente, médico o motivo"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
          />
          <select aria-label="Filtrar por médico" className="form-control" value={idMedico} onChange={(e) => setIdMedico(e.target.value)}>
            <option value="">Todos los médicos</option>
            {datosCita.medicos
              .filter((m) => m.estado !== false)
              .map((m) => (
                <option key={m.idMedico} value={m.idMedico}>
                  {nombreMedico(m)}
                </option>
              ))}
          </select>
          <label className="agenda-filtro-fecha mb-0">
            <span className="small text-muted">Hasta</span>
            <input type="date" className="form-control" value={hasta} min={incluirVencidas ? undefined : hoy} onChange={(e) => setHasta(e.target.value)} />
          </label>
          <label className="d-flex align-items-center small mb-0">
            <input type="checkbox" className="mr-2" checked={incluirVencidas} onChange={(e) => setIncluirVencidas(e.target.checked)} />
            Incluir vencidas
          </label>
        </div>

        {!cargando && !error && (
          <div className="agenda-resumen">
            {incluirVencidas && (
              <span>
                <strong className={resumen.vencidas ? 'text-danger' : ''}>{resumen.vencidas}</strong> vencidas
              </span>
            )}
            <span>
              <strong>{resumen.hoy}</strong> hoy
            </span>
            <span>
              <strong>{resumen.proximas}</strong> próximas
            </span>
          </div>
        )}
      </div>

      <div className="row">
        <div className="col-lg-4">
          <div className="card card-body agenda-calendario">
            <CalendarioMes mes={mes} onCambiarMes={setMes} conteos={conteos} hoy={hoy} seleccionado={diaSeleccionado} onSeleccionar={seleccionarDia} />
          </div>
        </div>
        <div className="col-lg-8">
      {diaSeleccionado && (
        <div className="agenda-dia-filtro">
          <span>
            Mostrando las citas del <strong>{formatearFecha(diaSeleccionado)}</strong>
          </span>
          <button type="button" className="btn btn-link btn-sm p-0 m-0" onClick={() => setDiaSeleccionado('')}>
            Ver todos los días
          </button>
        </div>
      )}
      {cargando || datosCita.cargando ? (
        <div className="card card-body">
          <Loading texto="Cargando agenda..." />
        </div>
      ) : dias.length === 0 ? (
        !error && (
          <div className="card card-body">
            <EmptyState
              titulo="No hay citas pendientes"
              descripcion={diaSeleccionado ? 'Ese día no tiene citas pendientes con estos filtros.' : 'No hay citas programadas, confirmadas ni reprogramadas con estos filtros.'}
            />
          </div>
        )
      ) : (
        dias.map((d) => {
          const etiqueta = etiquetaDia(d.fecha, hoy)
          return (
            <section key={d.fecha} className={`card agenda-dia${d.fecha < hoy ? ' agenda-dia-vencida' : ''}`} aria-labelledby={`dia-${d.fecha}`}>
              <header className="agenda-dia-encabezado">
                <h2 id={`dia-${d.fecha}`} className="h6 mb-0">
                  <span className="agenda-dia-nombre">{nombreDia(d.fecha)}</span>
                  <span className="text-muted ml-2">{formatearFecha(d.fecha)}</span>
                </h2>
                <span className="d-flex align-items-center">
                  {etiqueta && <Badge tono={etiqueta === 'Vencida' ? 'rojo' : etiqueta === 'Hoy' ? 'azul' : 'gris'}>{etiqueta}</Badge>}
                  <span className="small text-muted ml-2">
                    {d.citas.length} {d.citas.length === 1 ? 'cita' : 'citas'}
                  </span>
                </span>
              </header>
              <ul className="agenda-lista">
                {d.citas.map((c) => {
                  const estado = datosCita.estadosPorId[c.idEstado]
                  const confirmable = puedeConfirmar && idConfirmada && CONFIRMABLES.includes(String(estado?.descripcion ?? '').toLowerCase())
                  return (
                    <li key={c.idCita} className="agenda-cita">
                      <span className="agenda-hora">{formatearHora(c.hora)}</span>
                      <div className="agenda-detalle">
                        <Link to={`/citas/${c.idCita}`} className="font-weight-bold">
                          {nombrePaciente(datosCita.pacientesPorId[c.idPaciente], c.idPaciente)}
                        </Link>
                        <div className="small text-muted">
                          {nombreMedico(datosCita.medicosPorId[c.idMedico], c.idMedico)}
                          {c.motivo ? ` · ${c.motivo}` : ''}
                        </div>
                      </div>
                      <div className="agenda-acciones">
                        <EstadoCitaBadge estado={estado ?? { descripcion: `#${c.idEstado}` }} />
                        {confirmable && (
                          <button type="button" className="btn btn-sm btn-outline-primary btn-round mb-0" disabled={confirmando === c.idCita} onClick={() => confirmar(c)}>
                            {confirmando === c.idCita ? 'Confirmando...' : 'Confirmar'}
                          </button>
                        )}
                        <Link to={`/citas/${c.idCita}`} className="small">
                          Ver
                        </Link>
                      </div>
                    </li>
                  )
                })}
              </ul>
            </section>
          )
        })
      )}
        </div>
      </div>
    </div>
  )
}
