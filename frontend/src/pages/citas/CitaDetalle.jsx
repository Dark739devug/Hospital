import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap'
import { validarTransicionCita } from '../../api/catalogos.js'
import { cambiarEstadoCita, desactivarCita, obtenerCita } from '../../api/citas.js'
import { obtenerConsultaDeCita } from '../../api/consultas.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import EstadoCitaBadge from '../../components/EstadoCitaBadge.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosCita } from '../../hooks/useDatosCita.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFecha, formatearFechaHora, formatearHora, hoyISO } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from './utilsCita.js'

/** Diálogo para cambiar el estado: solo ofrece las transiciones que el backend permite. */
function CambiarEstadoDialog({ cita, estados, estadoActual, onCerrar, onCambiado }) {
  const [seleccionado, setSeleccionado] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const { datos: permitidos, cargando } = useApi(`transiciones|${cita.idCita}|${cita.idEstado}`, async () => {
    const candidatos = estados.filter((e) => e.idEstado !== cita.idEstado && e.activo !== false)
    const respuestas = await Promise.all(
      candidatos.map((e) => validarTransicionCita(estadoActual.descripcion, e.descripcion).catch(() => false)),
    )
    return candidatos.filter((_, i) => respuestas[i])
  })

  const guardar = async () => {
    setGuardando(true)
    setError('')
    try {
      await cambiarEstadoCita(cita.idCita, seleccionado)
      onCambiado()
    } catch (e) {
      setError(normalizarError(e).message)
      setGuardando(false)
    }
  }

  return (
    <Modal isOpen toggle={guardando ? undefined : onCerrar} centered fade={false}>
      <ModalHeader closeAriaLabel="Cerrar" toggle={guardando ? undefined : onCerrar}>Cambiar estado de la cita</ModalHeader>
      <ModalBody>
        <p>
          Estado actual: <strong>{estadoActual.descripcion}</strong>
        </p>
        {cargando ? (
          <p className="text-muted small mb-0">Consultando transiciones permitidas...</p>
        ) : permitidos.length === 0 ? (
          <Alert color="warning" fade={false} className="mb-0">
            Esta cita está en un estado final: no admite más cambios.
          </Alert>
        ) : (
          <div>
            <label htmlFor="nuevo-estado" className="d-block font-weight-bold small mb-1">
              Nuevo estado
            </label>
            <select id="nuevo-estado" value={seleccionado} onChange={(e) => setSeleccionado(e.target.value)} className="form-control">
              <option value="">Seleccione...</option>
              {permitidos.map((e) => (
                <option key={e.idEstado} value={e.idEstado}>
                  {e.descripcion}
                </option>
              ))}
            </select>
          </div>
        )}
        {error && (
          <Alert color="danger" fade={false} role="alert" className="mt-3 mb-0">
            {error}
          </Alert>
        )}
      </ModalBody>
      <ModalFooter>
        <Button color="secondary" outline onClick={onCerrar} disabled={guardando}>
          Cerrar
        </Button>
        {!cargando && permitidos.length > 0 && (
          <Button color="primary" onClick={guardar} disabled={!seleccionado || guardando}>
            {guardando ? 'Guardando...' : 'Cambiar estado'}
          </Button>
        )}
      </ModalFooter>
    </Modal>
  )
}

export default function CitaDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'citas', 'editar')
  const puedeCambiarEstado = puede(rol, 'citas', 'cambiarEstado')
  const puedeDesactivar = puede(rol, 'citas', 'desactivar')
  const puedeRegistrarConsulta = puede(rol, 'consultas', 'crear')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [cambiandoEstado, setCambiandoEstado] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  const datosCita = useDatosCita()
  const { datos: cita, error, cargando, recargar } = useApi(`cita|${id}`, () => obtenerCita(id))
  // Consulta registrada para la cita (null = todavía no tiene). Recepción no puede leer consultas (403): no se pide.
  const puedeVerConsulta = puede(rol, 'consultas', 'ver')
  const { datos: consulta, recargar: recargarConsulta } = useApi(`cita-consulta|${id}|${puedeVerConsulta}`, () =>
    puedeVerConsulta ? obtenerConsultaDeCita(id) : Promise.resolve(null),
  )

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const desactivar = async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarCita(id)
      navigate('/citas', { replace: true, state: { mensaje: 'La cita fue desactivada.' } })
    } catch (e) {
      // 409: la cita ya tiene una consulta registrada
      setErrorDesactivar(normalizarError(e).message)
      setDesactivando(false)
    }
  }

  if (cargando || datosCita.cargando) return <Loading texto="Cargando cita..." />

  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Cita no encontrada' : 'No se pudo cargar la cita'} mensaje={error.message} />
        <Link to="/citas" className="volver-enlace">
          &larr; Volver a citas
        </Link>
      </div>
    )
  }

  const activa = cita.activo !== false
  const estado = datosCita.estadosPorId[cita.idEstado]
  const paciente = datosCita.pacientesPorId[cita.idPaciente]
  const medico = datosCita.medicosPorId[cita.idMedico]
  // El backend solo admite la consulta de una cita activa, Confirmada o En curso, con fecha <= hoy.
  const estadoAtendible = estado && ['confirmada', 'en curso'].includes(estado.descripcion.toLowerCase())
  const fechaFutura = cita.fecha > hoyISO()
  const puedeAtender = activa && estadoAtendible && !fechaFutura
  const motivoNoAtender = !activa
    ? 'La cita está inactiva.'
    : !estadoAtendible
      ? 'Solo se puede registrar la consulta de una cita Confirmada o En curso.'
      : fechaFutura
        ? `La consulta podrá registrarse a partir del ${formatearFecha(cita.fecha)}.`
        : ''

  return (
    <div>
      <Link to="/citas" className="volver-enlace">
        &larr; Volver a citas
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">
            Cita del {formatearFecha(cita.fecha)} a las {formatearHora(cita.hora)}
          </h1>
          <p className="d-flex align-items-center acciones-grupo text-muted small mt-1">
            <EstadoCitaBadge estado={estado ?? { descripcion: `#${cita.idEstado}` }} />
            {!activa && <BadgeActivo activo={false} />}
          </p>
        </div>
        <div className="acciones-grupo d-flex flex-wrap">
          {activa && puedeCambiarEstado && estado && (
            <button
              type="button"
              onClick={() => setCambiandoEstado(true)}
              className="btn btn-primary btn-round"
            >
              Cambiar estado
            </button>
          )}
          {activa && puedeEditar && (
            <Link
              to={`/citas/${cita.idCita}/editar`}
              className="btn btn-secondary btn-round"
            >
              Editar
            </Link>
          )}
          {activa && puedeDesactivar && (
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
            {paciente ? (
              <Link to={`/pacientes/${paciente.idPaciente}`}>
                {nombrePaciente(paciente)}
              </Link>
            ) : (
              nombrePaciente(null, cita.idPaciente)
            )}
          </Dato>
          <Dato etiqueta="Médico">{nombreMedico(medico, cita.idMedico)}</Dato>
          <Dato etiqueta="Estado">{estado?.descripcion ?? `#${cita.idEstado}`}</Dato>
          <Dato etiqueta="Fecha">{formatearFecha(cita.fecha)}</Dato>
          <Dato etiqueta="Hora">{formatearHora(cita.hora)}</Dato>
          <Dato etiqueta="Creada">{formatearFechaHora(cita.fechaCreacion)}</Dato>
          <Dato etiqueta="Motivo" className="col-12">
            {cita.motivo}
          </Dato>
        </div>
      </div>

      <div className="card card-body">
        <h2 className="h5">Consulta médica</h2>
        {consulta ? (
          <p className="text-muted">
            Esta cita ya tiene la consulta #{consulta.idConsulta}.{' '}
            <Link to={`/consultas/${consulta.idConsulta}`} className="font-weight-bold">
              Ver consulta
            </Link>
          </p>
        ) : (
          <div className="d-flex flex-wrap align-items-center acciones-grupo text-muted">
            <span>Todavía no se registró la consulta de esta cita.</span>
            {puedeRegistrarConsulta &&
              (puedeAtender ? (
                <Link
                  to={`/consultas/nueva?idCita=${cita.idCita}`}
                  className="btn btn-outline-primary btn-sm btn-round"
                >
                  Registrar consulta
                </Link>
              ) : (
                <span className="text-muted small">{motivoNoAtender}</span>
              ))}
          </div>
        )}
      </div>

      {cambiandoEstado && estado && (
        <CambiarEstadoDialog
          cita={cita}
          estados={datosCita.estados}
          estadoActual={estado}
          onCerrar={() => setCambiandoEstado(false)}
          onCambiado={() => {
            setCambiandoEstado(false)
            setExito('El estado de la cita fue actualizado.')
            recargar()
            recargarConsulta()
          }}
        />
      )}

      <ConfirmDialog
        abierto={confirmando}
        titulo="Desactivar cita"
        mensaje="¿Desea desactivar esta cita? El registro no se elimina, pero dejará de contarse como activa."
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
