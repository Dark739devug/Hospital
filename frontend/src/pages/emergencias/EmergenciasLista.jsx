import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { listarEmergencias } from '../../api/emergencias.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge from '../../components/Badge.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import TriajeBadge from '../../components/TriajeBadge.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosEmergencia } from '../../hooks/useDatosEmergencia.js'
import { acortar, formatearFechaHora } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import { EmergenciasActividad, EmergenciasIndicadores } from './EmergenciasResumen.jsx'
import { estadoEmergencia, etiquetaPacienteEmergencia } from './utilsEmergencia.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'

export default function EmergenciasLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const puedeCrear = puede(rol, 'emergencia', 'crear')
  const puedeEditar = puede(rol, 'emergencia', 'editar')

  const [idPaciente, setIdPaciente] = useState('')
  const [idMedico, setIdMedico] = useState('')
  const [idTriaje, setIdTriaje] = useState('')
  const [params] = useSearchParams()
  // ?estado=abierta (enlace de "Emergencias en curso" en Inicio)
  const [estado, setEstado] = useState(['abierta', 'cerrada'].includes(params.get('estado')) ? params.get('estado') : '')
  const [texto, setTexto] = useState('')
  const [incluirInactivas, setIncluirInactivas] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')

  const datos = useDatosEmergencia()
  // idPaciente y soloActivas se combinan en el servidor.
  const { datos: lista, error, cargando } = useApi(`emergencias|${idPaciente}|${incluirInactivas}`, () =>
    listarEmergencias({ idPaciente, soloActivas: !incluirInactivas }),
  )

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    return (lista ?? [])
      .filter((e) => incluirInactivas || e.activo !== false)
      .filter((e) => !idMedico || String(e.idMedico) === idMedico)
      .filter((e) => !idTriaje || String(e.idTriaje) === idTriaje)
      .filter((e) => !estado || (estado === 'abierta' ? !e.fechaHoraAlta : !!e.fechaHoraAlta))
      .filter((e) => {
        if (!busqueda) return true
        return `${etiquetaPacienteEmergencia(e, datos.pacientesPorId)} ${e.motivo ?? ''}`.toLowerCase().includes(busqueda)
      })
      .sort((a, b) => String(b.fechaHoraIngreso).localeCompare(String(a.fechaHoraIngreso)))
  }, [lista, texto, incluirInactivas, idMedico, idTriaje, estado, datos])

  const alCambiar = (setter) => (e) => {
    setter(e.target.value)
    setPagina(1)
  }

  const columnas = [
    { clave: 'ingreso', titulo: 'Ingreso', render: (e) => formatearFechaHora(e.fechaHoraIngreso) },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (e) => (
        <Link to={`/emergencia/${e.idEmergencia}`} className="font-weight-bold">
          {etiquetaPacienteEmergencia(e, datos.pacientesPorId)}
        </Link>
      ),
    },
    { clave: 'motivo', titulo: 'Motivo', render: (e) => acortar(e.motivo, 45) },
    { clave: 'signos', titulo: 'Signos vitales', render: (e) => (e.signosVitales ? <span className="small">{acortar(e.signosVitales, 40)}</span> : '—') },
    { clave: 'habitacion', titulo: 'Habitación', render: (e) => (e.idHabitacion ? (datos.habitacionesPorId[e.idHabitacion]?.numero ?? `#${e.idHabitacion}`) : '—') },
    { clave: 'triaje', titulo: 'Triaje', render: (e) => <TriajeBadge triaje={datos.triajesPorId[e.idTriaje]} niveles={datos.niveles} fallback={`#${e.idTriaje}`} /> },
    { clave: 'medico', titulo: 'Médico', render: (e) => (e.idMedico ? nombreMedico(datos.medicosPorId[e.idMedico], e.idMedico) : '—') },
    {
      clave: 'estado',
      titulo: 'Estado',
      render: (e) => {
        const est = estadoEmergencia(e)
        return <Badge tono={est.tono}>{est.texto}</Badge>
      },
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (e) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/emergencia/${e.idEmergencia}`}>
            Ver
          </Link>
          {puedeEditar && e.activo !== false && (
            <Link to={`/emergencia/${e.idEmergencia}/editar`}>
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
          <h1 className="h3 mb-0">Emergencias</h1>
          <p className="text-muted mb-0">Ingresos, clasificación por triaje y atención en el área de emergencias.</p>
        </div>
        {puedeCrear && (
          <Link
            to="/emergencia/nueva"
            className="btn btn-primary btn-round"
          >
            Registrar ingreso
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <EmergenciasIndicadores
        datos={datos}
        emergencias={lista}
        idTriaje={idTriaje}
        onFiltrarTriaje={(id) => {
          setIdTriaje(id)
          setPagina(1)
        }}
      />

      <h2 className="h5 mb-3">Pacientes en emergencia</h2>
      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(e) => e.idEmergencia}
        cargando={cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por paciente o motivo"
        acciones={
          <>
            <select aria-label="Filtrar por estado" className={claseSelect} value={estado} onChange={alCambiar(setEstado)}>
              <option value="">Todos los estados</option>
              <option value="abierta">En atención</option>
              <option value="alta">Con alta</option>
            </select>
            <select aria-label="Filtrar por triaje" className={claseSelect} value={idTriaje} onChange={alCambiar(setIdTriaje)}>
              <option value="">Todos los niveles</option>
              {datos.triajes.map((t) => (
                <option key={t.idTriaje} value={t.idTriaje}>
                  {t.nivelDeEmergencia}
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
            <select aria-label="Filtrar por paciente" className={claseSelect} value={idPaciente} onChange={alCambiar(setIdPaciente)}>
              <option value="">Todos los pacientes</option>
              {datos.pacientes
                .filter((p) => p.activo !== false)
                .map((p) => (
                  <option key={p.idPaciente} value={p.idPaciente}>
                    {nombrePaciente(p)}
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
        vacio={<EmptyState titulo="No hay ingresos" descripcion="Ajuste los filtros o registre un ingreso a emergencia." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />

      <EmergenciasActividad datos={datos} emergencias={lista} />
    </div>
  )
}
