import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { listarCitas } from '../../api/citas.js'
import { listarConsultas } from '../../api/consultas.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { useDatosCita } from '../../hooks/useDatosCita.js'
import { acortar, formatearFechaHora } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import { consultaEditable } from './utilsConsulta.js'

const TAMANO_PAGINA = 10

export default function ConsultasLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const puedeCrear = puede(rol, 'consultas', 'crear')
  const puedeEditar = puede(rol, 'consultas', 'editar')

  const [texto, setTexto] = useState('')
  const [incluirInactivas, setIncluirInactivas] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')

  const datosCita = useDatosCita()
  const { datos, error, cargando } = useApi('consultas', async () => {
    // Las citas permiten mostrar paciente y médico de cada consulta.
    const [consultas, citas] = await Promise.all([listarConsultas(), listarCitas().catch(() => [])])
    return { consultas, citasPorId: mapaPor(citas, 'idCita') }
  })

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const describirCita = (idCita) => {
    const cita = datos?.citasPorId[idCita]
    if (!cita) return { paciente: `Cita #${idCita}`, medico: '' }
    return {
      paciente: nombrePaciente(datosCita.pacientesPorId[cita.idPaciente], cita.idPaciente),
      medico: nombreMedico(datosCita.medicosPorId[cita.idMedico], cita.idMedico),
    }
  }

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    const consultas = (datos?.consultas ?? []).filter((c) => incluirInactivas || c.activo !== false)
    return consultas
      .filter((c) => {
        if (!busqueda) return true
        const cita = datos.citasPorId[c.idCita]
        const paciente = cita ? nombrePaciente(datosCita.pacientesPorId[cita.idPaciente], cita.idPaciente) : ''
        return `${paciente} ${c.sintomas ?? ''}`.toLowerCase().includes(busqueda)
      })
      .sort((a, b) => String(b.fechaHora).localeCompare(String(a.fechaHora)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [datos, texto, incluirInactivas, datosCita])

  const columnas = [
    { clave: 'fechaHora', titulo: 'Fecha y hora', render: (c) => formatearFechaHora(c.fechaHora) },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (c) => (
        <Link to={`/consultas/${c.idConsulta}`} className="font-weight-bold">
          {describirCita(c.idCita).paciente}
        </Link>
      ),
    },
    { clave: 'medico', titulo: 'Médico', render: (c) => describirCita(c.idCita).medico || '—' },
    { clave: 'sintomas', titulo: 'Síntomas', render: (c) => acortar(c.sintomas, 50) },
    { clave: 'activo', titulo: 'Estado', render: (c) => <BadgeActivo activo={c.activo} /> },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (c) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/consultas/${c.idConsulta}`}>
            Ver
          </Link>
          {puedeEditar && consultaEditable(c) && (
            <Link to={`/consultas/${c.idConsulta}/editar`}>
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
          <h1 className="h3 mb-0">Consultas</h1>
          <p className="text-muted mb-0">Atención médica registrada a partir de una cita.</p>
        </div>
        {puedeCrear && (
          <Link
            to="/consultas/nueva"
            className="btn btn-primary btn-round"
          >
            Nueva consulta
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(c) => c.idConsulta}
        cargando={cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por paciente o síntomas"
        acciones={
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
        }
        vacio={<EmptyState titulo="No hay consultas" descripcion="Las consultas se registran desde una cita." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />
    </div>
  )
}
