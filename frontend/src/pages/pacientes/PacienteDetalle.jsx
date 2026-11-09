import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { listarAlergias, listarTiposSangre } from '../../api/catalogos.js'
import { desactivarPaciente, obtenerPaciente } from '../../api/pacientes.js'
import { useAuth } from '../../auth/useAuth.js'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import PacienteHospitalizaciones from '../hospitalizaciones/PacienteHospitalizaciones.jsx'
import { normalizarError } from '../../utils/errores.js'
import { calcularEdad, formatearFecha, formatearFechaHora } from '../../utils/formato.js'
import HistorialPaciente from './HistorialPaciente.jsx'
import { nombreCompleto } from './utilsPaciente.js'

export default function PacienteDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const { rol } = useAuth()
  const puedeEditarRol = puede(rol, 'pacientes', 'editar')
  const puedeDesactivarRol = puede(rol, 'pacientes', 'desactivar')

  const [paciente, setPaciente] = useState(null)
  const [alergia, setAlergia] = useState('')
  const [tipoSangre, setTipoSangre] = useState('')
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [confirmando, setConfirmando] = useState(false)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  useEffect(() => {
    let cancelado = false
    // El estado se reinicia dentro de la promesa y no de forma síncrona en el efecto (igual que useApi).
    Promise.resolve()
      .then(() => {
        if (cancelado) return null
        setCargando(true)
        setError(null)
        return obtenerPaciente(id)
      })
      .then(async (p) => {
        if (cancelado || !p) return
        setPaciente(p)
        // Los catálogos solo sirven para mostrar nombres; si fallan, se muestra el ID.
        const [alergias, tipos] = await Promise.allSettled([listarAlergias(), listarTiposSangre()])
        if (cancelado) return
        if (alergias.status === 'fulfilled') {
          setAlergia(alergias.value.find((a) => a.idAlergias === p.idAlergias)?.nombre ?? '')
        }
        if (tipos.status === 'fulfilled') {
          setTipoSangre(tipos.value.find((t) => t.idTipoDeSangre === p.idTipoDeSangre)?.nombre ?? '')
        }
      })
      .catch((e) => {
        if (!cancelado) setError(normalizarError(e))
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [id])

  const desactivar = async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarPaciente(id)
      navigate('/pacientes', {
        replace: true,
        state: { mensaje: `El paciente ${nombreCompleto(paciente)} fue desactivado.` },
      })
    } catch (e) {
      setErrorDesactivar(normalizarError(e).message)
      setDesactivando(false)
    }
  }

  if (cargando) return <Loading texto="Cargando paciente..." />

  if (error) {
    return (
      <div>
        <ErrorAlert
          titulo={error.status === 404 ? 'Paciente no encontrado' : 'No se pudo cargar el paciente'}
          mensaje={error.message}
        />
        <Link to="/pacientes" className="volver-enlace">
          &larr; Volver a pacientes
        </Link>
      </div>
    )
  }

  const p = paciente
  const estaActivo = p.activo !== false
  const puedeEditar = puedeEditarRol && estaActivo
  const puedeDesactivar = puedeDesactivarRol && estaActivo
  return (
    <div>
      <Link to="/pacientes" className="volver-enlace">
        &larr; Volver a pacientes
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">{nombreCompleto(p)}</h1>
          <p className="text-muted mb-0">
            DPI {p.dpiPaciente}
            {!estaActivo && (
              <span className="badge badge-secondary ml-2">Inactivo</span>
            )}
          </p>
        </div>
        <div className="acciones-grupo d-flex">
          {puede(rol, 'facturas', 'ver') && (
            <Link to={`/facturas?idPaciente=${p.idPaciente}`} className="btn btn-outline-secondary btn-round">
              Facturas
            </Link>
          )}
          {puedeEditar && (
            <Link
              to={`/pacientes/${p.idPaciente}/editar`}
              className="btn btn-secondary btn-round"
            >
              Editar
            </Link>
          )}
          {puedeDesactivar && (
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
          <Dato etiqueta="Nombre">{p.nombre}</Dato>
          <Dato etiqueta="Apellido paterno">{p.apellidoPaterno}</Dato>
          <Dato etiqueta="Apellido materno">{p.apellidoMaterno}</Dato>
          <Dato etiqueta="Fecha de nacimiento">
            {p.fechaNacimiento ? `${formatearFecha(p.fechaNacimiento)} (${calcularEdad(p.fechaNacimiento)} años)` : ''}
          </Dato>
          <Dato etiqueta="Sexo">{p.sexo === 'M' ? 'Masculino' : p.sexo === 'F' ? 'Femenino' : ''}</Dato>
          <Dato etiqueta="Teléfono">{p.telefono}</Dato>
          <Dato etiqueta="Correo electrónico">{p.correoElectronico}</Dato>
          <Dato etiqueta="Dirección">{p.direccion}</Dato>
          <Dato etiqueta="Tipo de sangre">{tipoSangre || (p.idTipoDeSangre ? `ID ${p.idTipoDeSangre}` : '')}</Dato>
          <Dato etiqueta="Alergia">{alergia || (p.idAlergias ? `ID ${p.idAlergias}` : '')}</Dato>
          <Dato etiqueta="Fecha de registro">{formatearFechaHora(p.fechaRegistro)}</Dato>
          <Dato etiqueta="Estado">{p.activo === false ? 'Inactivo' : 'Activo'}</Dato>
        </div>
      </div>

      {puede(rol, 'pacientes', 'historial') && <HistorialPaciente idPaciente={p.idPaciente} />}

      {puede(rol, 'hospitalizaciones', 'ver') && <PacienteHospitalizaciones idPaciente={p.idPaciente} />}

      <ConfirmDialog
        abierto={confirmando}
        titulo="Desactivar paciente"
        mensaje={`¿Desea desactivar a ${nombreCompleto(p)}? El registro no se elimina, pero dejará de aparecer en el listado.`}
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
