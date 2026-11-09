import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { listarHospitalizaciones } from '../../api/hospitalizaciones.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import { EstadoHospBadge } from '../../components/EstadosBadge.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosHospitalizacion } from '../../hooks/useDatosHospitalizacion.js'
import { formatearFechaHora } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import CensoHabitaciones from './CensoHabitaciones.jsx'
import { estaActiva, nombreHabitacion } from './utilsHospitalizacion.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'

export default function HospitalizacionesLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const puedeCrear = puede(rol, 'hospitalizaciones', 'crear')
  const puedeEditar = puede(rol, 'hospitalizaciones', 'editar')

  const [idEstado, setIdEstado] = useState('1')
  const [idMedico, setIdMedico] = useState('')
  const [idHabitacion, setIdHabitacion] = useState('')
  const [texto, setTexto] = useState('')
  const [incluirInactivas, setIncluirInactivas] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')

  const datos = useDatosHospitalizacion()
  const { datos: lista, error, cargando } = useApi('hospitalizaciones', listarHospitalizaciones)

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    return (lista ?? [])
      .filter((h) => incluirInactivas || h.activo !== false)
      .filter((h) => !idEstado || String(h.idEstadoHosp) === idEstado)
      .filter((h) => !idMedico || String(h.idMedicoResponsable) === idMedico)
      .filter((h) => !idHabitacion || String(h.idHabitacion) === idHabitacion)
      .filter((h) => {
        if (!busqueda) return true
        return `${nombrePaciente(datos.pacientesPorId[h.idPaciente], h.idPaciente)} ${h.motivoIngreso ?? ''}`.toLowerCase().includes(busqueda)
      })
      .sort((a, b) => String(b.fechaIngreso).localeCompare(String(a.fechaIngreso)))
  }, [lista, texto, incluirInactivas, idEstado, idMedico, idHabitacion, datos])

  const alCambiar = (setter) => (e) => {
    setter(e.target.value)
    setPagina(1)
  }

  const columnas = [
    { clave: 'ingreso', titulo: 'Ingreso', render: (h) => formatearFechaHora(h.fechaIngreso) },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (h) => (
        <Link to={`/hospitalizaciones/${h.idHospitalizacion}`} className="font-weight-bold">
          {nombrePaciente(datos.pacientesPorId[h.idPaciente], h.idPaciente)}
        </Link>
      ),
    },
    { clave: 'habitacion', titulo: 'Habitación', render: (h) => nombreHabitacion(datos.habitacionesPorId[h.idHabitacion], h.idHabitacion) },
    { clave: 'medico', titulo: 'Médico', render: (h) => nombreMedico(datos.medicosPorId[h.idMedicoResponsable], h.idMedicoResponsable) },
    {
      clave: 'estado',
      titulo: 'Estado',
      render: (h) => (
        <span className="d-flex align-items-center">
          <EstadoHospBadge estado={datos.estadosHospPorId[h.idEstadoHosp]} fallback={`#${h.idEstadoHosp}`} />
          {h.activo === false && <BadgeActivo activo={false} />}
        </span>
      ),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (h) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/hospitalizaciones/${h.idHospitalizacion}`}>
            Ver
          </Link>
          {puedeEditar && estaActiva(h, datos.idEstadoActiva) && h.activo !== false && (
            <Link to={`/hospitalizaciones/${h.idHospitalizacion}/editar`}>
              Editar
            </Link>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Hospitalizaciones</h1>
          <p className="text-muted mb-0">Ocupación de camas por habitación, enfermería asignada, ingresos y altas.</p>
        </div>
        {puedeCrear && (
          <Link
            to="/hospitalizaciones/nueva"
            className="btn btn-primary btn-round"
          >
            Ingresar paciente
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <CensoHabitaciones datos={datos} hospitalizaciones={lista} puedeIngresar={puedeCrear} />

      <h2 className="h5 mb-3">Hospitalizaciones</h2>
      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(h) => h.idHospitalizacion}
        cargando={cargando || datos.cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por paciente o motivo"
        acciones={
          <>
            <select aria-label="Filtrar por estado" className={claseSelect} value={idEstado} onChange={alCambiar(setIdEstado)}>
              <option value="">Todos los estados</option>
              {datos.estadosHosp.map((e) => (
                <option key={e.idEstadoHosp} value={e.idEstadoHosp}>
                  {e.descripcion}
                </option>
              ))}
            </select>
            <select aria-label="Filtrar por habitación" className={claseSelect} value={idHabitacion} onChange={alCambiar(setIdHabitacion)}>
              <option value="">Todas las habitaciones</option>
              {datos.habitaciones.map((h) => (
                <option key={h.idHabitacion} value={h.idHabitacion}>
                  {nombreHabitacion(h)}
                </option>
              ))}
            </select>
            <select aria-label="Filtrar por médico" className={claseSelect} value={idMedico} onChange={alCambiar(setIdMedico)}>
              <option value="">Todos los médicos</option>
              {datos.medicos.map((m) => (
                <option key={m.idMedico} value={m.idMedico}>
                  {nombreMedico(m)}
                </option>
              ))}
            </select>
            <label className="d-flex align-items-center small mb-0">
              <input
                type="checkbox"
                checked={incluirInactivas}
                onChange={(e) => {
                  setIncluirInactivas(e.target.checked)
                  setPagina(1)
                }}
                className="mr-2"
              />
              Mostrar inactivas
            </label>
          </>
        }
        vacio={<EmptyState titulo="No hay hospitalizaciones" descripcion="Ajuste los filtros (por defecto se muestran las activas) o ingrese un paciente." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />
    </div>
  )
}
