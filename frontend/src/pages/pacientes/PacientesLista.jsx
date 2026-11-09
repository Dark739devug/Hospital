import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { desactivarPaciente, listarPacientes } from '../../api/pacientes.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { normalizarError } from '../../utils/errores.js'
import { calcularEdad, formatearFecha } from '../../utils/formato.js'
import { nombreCompleto } from './utilsPaciente.js'

const TAMANO_PAGINA = 10

export default function PacientesLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const puedeCrear = puede(rol, 'pacientes', 'crear')
  const puedeEditar = puede(rol, 'pacientes', 'editar')
  const puedeDesactivar = puede(rol, 'pacientes', 'desactivar')

  const [texto, setTexto] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [incluirInactivos, setIncluirInactivos] = useState(false)
  const [pacientes, setPacientes] = useState([])
  const [pagina, setPagina] = useState(1)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [aDesactivar, setADesactivar] = useState(null)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')
  const [recarga, setRecarga] = useState(0)

  // Limpia el mensaje de éxito heredado de la navegación para que no reaparezca al recargar.
  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  // Búsqueda con retardo para no llamar a la API en cada tecla.
  useEffect(() => {
    const t = setTimeout(() => {
      setBusqueda(texto)
      setPagina(1)
    }, 350)
    return () => clearTimeout(t)
  }, [texto])

  useEffect(() => {
    let cancelado = false
    // El estado se reinicia dentro de la promesa y no de forma síncrona en el efecto (igual que useApi).
    Promise.resolve()
      .then(() => {
        if (cancelado) return null
        setCargando(true)
        setError('')
        return listarPacientes({ q: busqueda, todos: incluirInactivos })
      })
      .then((datos) => {
        if (!cancelado && datos) setPacientes(datos)
      })
      .catch((e) => {
        if (!cancelado) {
          setError(normalizarError(e).message)
          setPacientes([])
        }
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [busqueda, incluirInactivos, recarga])

  const confirmarDesactivar = useCallback(async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarPaciente(aDesactivar.idPaciente)
      setExito(`El paciente ${nombreCompleto(aDesactivar)} fue desactivado.`)
      setADesactivar(null)
      setRecarga((n) => n + 1)
    } catch (e) {
      // 404: ya no existe; 422: tiene citas/hospitalización/emergencia/cuentas activas.
      setErrorDesactivar(normalizarError(e).message)
    } finally {
      setDesactivando(false)
    }
  }, [aDesactivar])

  const columnas = [
    {
      clave: 'nombre',
      titulo: 'Paciente',
      render: (p) => (
        <Link
          to={`/pacientes/${p.idPaciente}`}
          className={`font-weight-bold${p.activo === false ? ' text-muted' : ''}`}
        >
          {nombreCompleto(p)}
        </Link>
      ),
    },
    { clave: 'dpiPaciente', titulo: 'DPI' },
    { clave: 'sexo', titulo: 'Sexo', render: (p) => (p.sexo === 'M' ? 'Masculino' : p.sexo === 'F' ? 'Femenino' : '—') },
    {
      clave: 'fechaNacimiento',
      titulo: 'Nacimiento',
      render: (p) =>
        p.fechaNacimiento ? `${formatearFecha(p.fechaNacimiento)} (${calcularEdad(p.fechaNacimiento)} años)` : '—',
    },
    { clave: 'telefono', titulo: 'Teléfono' },
    {
      clave: 'activo',
      titulo: 'Estado',
      render: (p) => (
        <BadgeActivo activo={p.activo} />
      ),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (p) => {
        const activo = p.activo !== false
        return (
          <div className="tabla-acciones d-flex justify-content-end">
            <Link to={`/pacientes/${p.idPaciente}`}>
              Ver
            </Link>
            {activo && puedeEditar && (
              <Link to={`/pacientes/${p.idPaciente}/editar`}>
                Editar
              </Link>
            )}
            {activo && puedeDesactivar && (
              <button
                type="button"
                onClick={() => {
                  setErrorDesactivar('')
                  setADesactivar(p)
                }}
                className="btn btn-link text-danger p-0"
              >
                Desactivar
              </button>
            )}
          </div>
        )
      },
    },
  ]

  const visibles = pacientes.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Pacientes</h1>
          <p className="text-muted mb-0">Búsqueda por nombre, apellidos o DPI.</p>
        </div>
        {puedeCrear && (
          <Link
            to="/pacientes/nuevo"
            className="btn btn-primary btn-round"
          >
            Nuevo paciente
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error} />

      <Table
        columnas={columnas}
        filas={visibles}
        claveFila={(p) => p.idPaciente}
        cargando={cargando}
        busqueda={texto}
        onBuscar={setTexto}
        placeholderBusqueda="Buscar por nombre, apellidos o DPI"
        acciones={
          <label className="d-flex align-items-center small mb-0">
            <input
              type="checkbox"
              checked={incluirInactivos}
              onChange={(e) => {
                setIncluirInactivos(e.target.checked)
                setPagina(1)
              }}
              className="mr-2"
            />
            Mostrar inactivos
          </label>
        }
        vacio={
          <EmptyState
            titulo={busqueda ? 'Sin coincidencias' : 'No hay pacientes registrados'}
            descripcion={busqueda ? 'Pruebe con otro nombre, apellido o DPI.' : 'Cuando se registren pacientes aparecerán aquí.'}
          />
        }
        pie={
          <Pagination
            pagina={pagina}
            totalElementos={pacientes.length}
            tamano={TAMANO_PAGINA}
            onCambiarPagina={setPagina}
          />
        }
      />

      <ConfirmDialog
        abierto={!!aDesactivar}
        titulo="Desactivar paciente"
        mensaje={
          aDesactivar
            ? `¿Desea desactivar a ${nombreCompleto(aDesactivar)}? El registro no se elimina, pero dejará de aparecer en el listado.`
            : ''
        }
        textoConfirmar="Desactivar"
        peligro
        cargando={desactivando}
        error={errorDesactivar}
        onConfirmar={confirmarDesactivar}
        onCancelar={() => setADesactivar(null)}
      />
    </div>
  )
}
