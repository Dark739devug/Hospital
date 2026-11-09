import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap'
import { crearAsignacion, listarAsignacionesDeHospitalizacion, quitarAsignacion } from '../../api/asignaciones.js'
import { listarEnfermeros, listarTurnos } from '../../api/catalogos.js'
import { darAltaHospitalizacion, desactivarHospitalizacion, obtenerHospitalizacion } from '../../api/hospitalizaciones.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import { EstadoHospBadge } from '../../components/EstadosBadge.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { useDatosHospitalizacion } from '../../hooks/useDatosHospitalizacion.js'
import { normalizarError } from '../../utils/errores.js'
import { deInputFechaHora, formatearFecha, formatearFechaHora, hoyISO } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import { estaActiva, nombreHabitacion } from './utilsHospitalizacion.js'

const claseSelect = 'form-control'

const nombreEnfermero = (e, id) =>
  e ? [e.nombre, e.apellidoPaterno, e.apellidoMaterno].filter(Boolean).join(' ') : `Enfermero #${id}`

/** Diálogo para dar de alta: la fecha y hora son opcionales (el servidor usa la actual). */
function AltaDialog({ hospitalizacion, onCerrar, onListo }) {
  const [fechaHora, setFechaHora] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const confirmar = async () => {
    setGuardando(true)
    setError('')
    try {
      await darAltaHospitalizacion(hospitalizacion.idHospitalizacion, deInputFechaHora(fechaHora))
      onListo()
    } catch (e) {
      // 400: solo se da de alta una hospitalización Activa / alta anterior al ingreso
      setError(normalizarError(e).message)
      setGuardando(false)
    }
  }

  return (
    <Modal isOpen toggle={guardando ? undefined : onCerrar} centered fade={false}>
      <ModalHeader closeAriaLabel="Cerrar" toggle={guardando ? undefined : onCerrar}>Dar de alta</ModalHeader>
      <ModalBody>
        <p>Ingreso: {formatearFechaHora(hospitalizacion.fechaIngreso)}. Si no indica fecha y hora se usa el momento actual. Al dar el alta se libera la habitación si queda sin pacientes.</p>
        <label htmlFor="fecha-alta-hosp" className="d-block font-weight-bold small mb-1">
          Fecha y hora de alta (opcional)
        </label>
        <input
          id="fecha-alta-hosp"
          type="datetime-local"
          value={fechaHora}
          min={hospitalizacion.fechaIngreso?.slice(0, 16)}
          onChange={(e) => setFechaHora(e.target.value)}
          className="form-control"
        />
        {error && (
          <Alert color="danger" fade={false} role="alert" className="mt-3 mb-0">
            {error}
          </Alert>
        )}
      </ModalBody>
      <ModalFooter>
        <Button color="secondary" outline onClick={onCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button color="primary" onClick={confirmar} disabled={guardando}>
          {guardando ? 'Guardando...' : 'Confirmar alta'}
        </Button>
      </ModalFooter>
    </Modal>
  )
}

export default function HospitalizacionDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'hospitalizaciones', 'editar')
  const puedeDarAlta = puede(rol, 'hospitalizaciones', 'darAlta')
  const puedeDesactivar = puede(rol, 'hospitalizaciones', 'desactivar')
  const puedeAsignar = puede(rol, 'hospitalizaciones', 'asignarEnfermeros')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [dandoAlta, setDandoAlta] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')
  const [asignacion, setAsignacion] = useState({ idEnfermero: '', idTurno: '', fechaAsignacion: hoyISO() })
  const [asignando, setAsignando] = useState(false)
  const [errorAsignar, setErrorAsignar] = useState('')
  const [erroresAsignar, setErroresAsignar] = useState({})
  const [aQuitar, setAQuitar] = useState(null)
  const [quitando, setQuitando] = useState(false)
  const [errorQuitar, setErrorQuitar] = useState('')

  const datos = useDatosHospitalizacion()
  const { datos: hospitalizacion, error, cargando, recargar } = useApi(`hospitalizacion|${id}`, () => obtenerHospitalizacion(id))
  const { datos: extra, recargar: recargarAsignaciones } = useApi(`asignaciones|${id}`, async () => {
    const [asignaciones, enfermeros, turnos] = await Promise.all([
      listarAsignacionesDeHospitalizacion(id, { soloActivas: true }).catch(() => []),
      listarEnfermeros().catch(() => []),
      listarTurnos().catch(() => []),
    ])
    return { asignaciones, enfermerosPorId: mapaPor(enfermeros, 'idEnfermero'), enfermeros, turnos, turnosPorId: mapaPor(turnos, 'idTurno') }
  })

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const desactivar = async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarHospitalizacion(id)
      navigate('/hospitalizaciones', { replace: true, state: { mensaje: 'La hospitalización fue desactivada.' } })
    } catch (e) {
      // 409: la hospitalización está Activa (hay que dar el alta primero)
      setErrorDesactivar(normalizarError(e).message)
      setDesactivando(false)
    }
  }

  const asignar = async () => {
    setErrorAsignar('')
    setErroresAsignar({})
    if (!asignacion.idEnfermero || !asignacion.idTurno || !asignacion.fechaAsignacion) {
      setErrorAsignar('Seleccione enfermero, turno y fecha.')
      return
    }
    setAsignando(true)
    try {
      await crearAsignacion({
        idEnfermero: Number(asignacion.idEnfermero),
        idHospitalizacion: Number(id),
        idTurno: Number(asignacion.idTurno),
        fechaAsignacion: asignacion.fechaAsignacion,
      })
      setAsignacion((a) => ({ ...a, idEnfermero: '' }))
      setExito('Enfermero asignado.')
      recargarAsignaciones()
    } catch (e) {
      // 400 con `detalles` por campo; 409 (asignación repetida) y 422 (reglas) con `message`
      const { message, detalles } = normalizarError(e)
      setErroresAsignar(detalles)
      setErrorAsignar(Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
    } finally {
      setAsignando(false)
    }
  }

  const quitar = async () => {
    setQuitando(true)
    setErrorQuitar('')
    try {
      await quitarAsignacion(aQuitar.idAsignacion)
      setAQuitar(null)
      setExito('Asignación quitada.')
      recargarAsignaciones()
    } catch (e) {
      setErrorQuitar(normalizarError(e).message)
    } finally {
      setQuitando(false)
    }
  }

  if (cargando || datos.cargando) return <Loading texto="Cargando hospitalización..." />

  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Hospitalización no encontrada' : 'No se pudo cargar la hospitalización'} mensaje={error.message} />
        <Link to="/hospitalizaciones" className="volver-enlace">
          &larr; Volver a hospitalizaciones
        </Link>
      </div>
    )
  }

  const h = hospitalizacion
  const registroActivo = h.activo !== false
  const abierta = registroActivo && estaActiva(h, datos.idEstadoActiva)
  const asignaciones = extra?.asignaciones ?? []
  const paciente = datos.pacientesPorId[h.idPaciente]

  return (
    <div>
      <Link to="/hospitalizaciones" className="volver-enlace">
        &larr; Volver a hospitalizaciones
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">{nombrePaciente(paciente, h.idPaciente)}</h1>
          <p className="d-flex align-items-center acciones-grupo text-muted small mt-1">
            <EstadoHospBadge estado={datos.estadosHospPorId[h.idEstadoHosp]} fallback={`#${h.idEstadoHosp}`} />
            {!registroActivo && <BadgeActivo activo={false} />}
          </p>
        </div>
        <div className="acciones-grupo d-flex flex-wrap">
          {puede(rol, 'facturas', 'ver') && (
            <Link to={`/facturas?idHospitalizacion=${h.idHospitalizacion}`} className="btn btn-outline-secondary btn-round">
              Facturas
            </Link>
          )}
          {abierta && puedeDarAlta && (
            <button
              type="button"
              onClick={() => setDandoAlta(true)}
              className="btn btn-primary btn-round"
            >
              Dar de alta
            </button>
          )}
          {abierta && puedeEditar && (
            <Link
              to={`/hospitalizaciones/${h.idHospitalizacion}/editar`}
              className="btn btn-secondary btn-round"
            >
              Editar
            </Link>
          )}
          {registroActivo && puedeDesactivar && abierta && (
            <span className="align-self-center text-muted small">Para desactivarla, primero registre el alta.</span>
          )}
          {registroActivo && puedeDesactivar && !abierta && (
            <button
              type="button"
              onClick={() => {
                setErrorDesactivar('')
                setConfirmando(true)
              }}
              className="btn btn-danger btn-round"
            >
              Desactivar
            </button>
          )}
        </div>
      </div>

      <div className="card card-body">
        <div className="row">
          <Dato etiqueta="Paciente">
            <Link to={`/pacientes/${h.idPaciente}`}>
              {nombrePaciente(paciente, h.idPaciente)}
            </Link>
          </Dato>
          <Dato etiqueta="Habitación">
            <Link to={`/habitaciones/${h.idHabitacion}`}>
              {nombreHabitacion(datos.habitacionesPorId[h.idHabitacion], h.idHabitacion)}
            </Link>
          </Dato>
          <Dato etiqueta="Médico responsable">{nombreMedico(datos.medicosPorId[h.idMedicoResponsable], h.idMedicoResponsable)}</Dato>
          <Dato etiqueta="Ingreso">{formatearFechaHora(h.fechaIngreso)}</Dato>
          <Dato etiqueta="Alta">{h.fechaAlta ? formatearFechaHora(h.fechaAlta) : ''}</Dato>
          <Dato etiqueta="Clínica">{h.idClinica ? (datos.clinicasPorId[h.idClinica]?.nombre ?? `#${h.idClinica}`) : ''}</Dato>
          <Dato etiqueta="Motivo de ingreso" className="col-12">
            {h.motivoIngreso}
          </Dato>
          <Dato etiqueta="Diagnóstico de ingreso" className="col-12">
            {h.diagnosticoIngreso}
          </Dato>
        </div>
      </div>

      <div className="card card-body">
        <h2 className="h5 mb-3">Enfermeros asignados</h2>
        {asignaciones.length === 0 ? (
          <p className="text-muted mb-0">No hay enfermeros asignados.</p>
        ) : (
          <ul className="lista-divisoria">
            {asignaciones.map((a) => (
              <li key={a.idAsignacion} className="d-flex align-items-center justify-content-between">
                <span>
                  <span className="font-weight-bold">{nombreEnfermero(extra.enfermerosPorId[a.idEnfermero], a.idEnfermero)}</span>
                  <span className="ml-2 text-muted small">
                    {formatearFecha(a.fechaAsignacion)} · {extra.turnosPorId[a.idTurno]?.descripcion ?? `Turno #${a.idTurno}`}
                  </span>
                </span>
                {puedeAsignar && (
                  <button
                    type="button"
                    onClick={() => {
                      setErrorQuitar('')
                      setAQuitar(a)
                    }}
                    className="btn btn-link text-danger p-0"
                  >
                    Quitar
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}

        {abierta && puedeAsignar && (
          <div className="border-top pt-3 mt-3">
            <ErrorAlert mensaje={errorAsignar} onCerrar={() => setErrorAsignar('')} />
            <div className="row align-items-end mt-2">
              <div className="col-md-3 mb-2">
                <label htmlFor="asig-enfermero" className="d-block font-weight-bold small mb-1">
                  Enfermero
                </label>
                <select
                  id="asig-enfermero"
                  value={asignacion.idEnfermero}
                  onChange={(e) => setAsignacion((a) => ({ ...a, idEnfermero: e.target.value }))}
                  className={claseSelect}
                >
                  <option value="">{(extra?.enfermeros ?? []).length ? 'Seleccione...' : 'No hay enfermeros registrados'}</option>
                  {(extra?.enfermeros ?? [])
                    .filter((e) => e.activo !== false)
                    .map((e) => (
                      <option key={e.idEnfermero} value={e.idEnfermero}>
                        {nombreEnfermero(e)}
                      </option>
                    ))}
                </select>
                {erroresAsignar.idEnfermero && <p className="text-danger small mt-1">{erroresAsignar.idEnfermero}</p>}
              </div>
              <div className="col-md-3 mb-2">
                <label htmlFor="asig-turno" className="d-block font-weight-bold small mb-1">
                  Turno
                </label>
                <select
                  id="asig-turno"
                  value={asignacion.idTurno}
                  onChange={(e) => setAsignacion((a) => ({ ...a, idTurno: e.target.value }))}
                  className={claseSelect}
                >
                  <option value="">Seleccione...</option>
                  {(extra?.turnos ?? [])
                    .filter((t) => t.activo !== false)
                    .map((t) => (
                      <option key={t.idTurno} value={t.idTurno}>
                        {t.descripcion}
                      </option>
                    ))}
                </select>
                {erroresAsignar.idTurno && <p className="text-danger small mt-1">{erroresAsignar.idTurno}</p>}
              </div>
              <div className="col-md-3 mb-2">
                <label htmlFor="asig-fecha" className="d-block font-weight-bold small mb-1">
                  Fecha
                </label>
                <input
                  id="asig-fecha"
                  type="date"
                  value={asignacion.fechaAsignacion}
                  min={h.fechaIngreso?.slice(0, 10)}
                  onChange={(e) => setAsignacion((a) => ({ ...a, fechaAsignacion: e.target.value }))}
                  className={claseSelect}
                />
                {erroresAsignar.fechaAsignacion && <p className="text-danger small mt-1">{erroresAsignar.fechaAsignacion}</p>}
              </div>
              <button
                type="button"
                onClick={asignar}
                disabled={asignando}
                className="btn btn-primary btn-round ml-3 mb-2"
              >
                {asignando ? 'Asignando...' : 'Asignar'}
              </button>
            </div>
          </div>
        )}
      </div>

      {dandoAlta && (
        <AltaDialog
          hospitalizacion={h}
          onCerrar={() => setDandoAlta(false)}
          onListo={() => {
            setDandoAlta(false)
            setExito('El paciente fue dado de alta.')
            recargar()
          }}
        />
      )}

      <ConfirmDialog
        abierto={!!aQuitar}
        titulo="Quitar asignación"
        mensaje="¿Desea quitar esta asignación de enfermero? El registro se conserva como inactivo."
        textoConfirmar="Quitar"
        peligro
        cargando={quitando}
        error={errorQuitar}
        onConfirmar={quitar}
        onCancelar={() => setAQuitar(null)}
      />
      <ConfirmDialog
        abierto={confirmando}
        titulo="Desactivar hospitalización"
        mensaje="¿Desea desactivar este registro de hospitalización? El registro se conserva en el historial."
        textoConfirmar="Desactivar"
        peligro
        cargando={desactivando}
        error={errorDesactivar}
        onConfirmar={desactivar}
        onCancelar={() => setConfirmando(false)}
      />
    </div>
  )
}
