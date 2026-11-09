import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { listarFacturas } from '../../api/facturas.js'
import { useAuth } from '../../auth/useAuth.js'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import EstadoFacturaBadge from '../../components/EstadoFacturaBadge.jsx'
import PacienteSelector from '../../components/PacienteSelector.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosFacturacion } from '../../hooks/useDatosFacturacion.js'
import { formatearFechaHora, formatearMoneda } from '../../utils/formato.js'
import { nombrePaciente } from '../citas/utilsCita.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'

export default function FacturasLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const puedeCrear = puede(rol, 'facturas', 'crear')

  const [texto, setTexto] = useState('')
  const [idPaciente, setIdPaciente] = useState(params.get('idPaciente') ?? '')
  const idHospitalizacion = params.get('idHospitalizacion') ?? ''
  // ?estado=<idEstadoFactura> (enlace de "Facturas pendientes" en Inicio)
  const [estado, setEstado] = useState(/^\d+$/.test(params.get('estado') ?? '') ? params.get('estado') : '')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [incluirAnuladas, setIncluirAnuladas] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')

  // Todos los filtros (paciente, hospitalización, estado, fechas de emisión y activas) se envían al servidor y son combinables.
  // Solo la búsqueda por texto (N.º de factura o nombre del paciente) se resuelve aquí.
  const idPacienteNum = idPaciente && /^\d+$/.test(idPaciente) ? idPaciente : ''
  const filtrosServidor = {
    idPaciente: idPacienteNum,
    idHospitalizacion,
    idEstadoFactura: estado,
    desde,
    hasta,
    soloActivas: !incluirAnuladas,
  }
  const { datos: facturas, error, cargando } = useApi(
    `facturas|${idPacienteNum}|${idHospitalizacion}|${estado}|${desde}|${hasta}|${incluirAnuladas}`,
    () => listarFacturas(filtrosServidor),
  )

  // Nombres de los pacientes de las facturas listadas (GET /api/paciente/{id}/resumen, también permitido a CAJERO)
  const datos = useDatosFacturacion((facturas ?? []).map((f) => f.idPaciente))

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase().replace(/^#/, '')
    return (facturas ?? [])
      .filter((f) => {
        if (!busqueda) return true
        const paciente = nombrePaciente(datos.pacientesPorId[f.idPaciente], f.idPaciente).toLowerCase()
        return String(f.idFactura) === busqueda || paciente.includes(busqueda)
      })
      .sort((a, b) => String(b.fechaEmision).localeCompare(String(a.fechaEmision)) || b.idFactura - a.idFactura)
  }, [facturas, texto, datos.pacientesPorId])

  const alCambiar = (setter) => (e) => {
    setter(e.target.value)
    setPagina(1)
  }

  const columnas = [
    {
      clave: 'numero',
      titulo: 'Factura',
      render: (f) => (
        <Link to={`/facturas/${f.idFactura}`} className="font-weight-bold">
          #{f.idFactura}
        </Link>
      ),
    },
    { clave: 'fecha', titulo: 'Emisión', render: (f) => formatearFechaHora(f.fechaEmision) },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (f) => (
        <>
          {nombrePaciente(datos.pacientesPorId[f.idPaciente], f.idPaciente)}
          {f.idHospitalizacion && <div className="text-muted small">Hospitalización #{f.idHospitalizacion}</div>}
        </>
      ),
    },
    { clave: 'metodo', titulo: 'Método de pago', render: (f) => datos.metodosPorId[f.idMetodoPago]?.descripcion ?? '—' },
    { clave: 'total', titulo: 'Total', className: 'text-right', render: (f) => <strong className="text-nowrap">{formatearMoneda(f.total)}</strong> },
    {
      clave: 'estado',
      titulo: 'Estado',
      render: (f) => <EstadoFacturaBadge estado={datos.estadosPorId[f.idEstadoFactura]} activo={f.activo} />,
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (f) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/facturas/${f.idFactura}`}>Ver</Link>
        </div>
      ),
    },
  ]

  const filtroPaciente = (
    <PacienteSelector
      compacto
      etiqueta="Filtrar por paciente"
      valor={idPaciente}
      onChange={(v) => {
        setIdPaciente(v)
        setPagina(1)
      }}
    />
  )

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Facturas</h1>
          <p className="text-muted mb-0">
            Facturación de servicios a pacientes.
            {idHospitalizacion && ` Filtrando por la hospitalización #${idHospitalizacion}.`}
          </p>
        </div>
        {puedeCrear && (
          <Link
            to={`/facturas/nueva${idPacienteNum ? `?idPaciente=${idPacienteNum}` : ''}${idHospitalizacion ? `${idPacienteNum ? '&' : '?'}idHospitalizacion=${idHospitalizacion}` : ''}`}
            className="btn btn-primary btn-round"
          >
            Nueva factura
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(f) => f.idFactura}
        cargando={cargando || datos.cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por N.º de factura o paciente"
        acciones={
          <>
            {filtroPaciente}
            <select aria-label="Filtrar por estado" className={claseSelect} value={estado} onChange={alCambiar(setEstado)}>
              <option value="">Todos los estados</option>
              {datos.estados.map((e) => (
                <option key={e.idEstadoFactura} value={e.idEstadoFactura}>
                  {e.descripcion}
                </option>
              ))}
            </select>
            <label className="d-flex align-items-center small mb-0">
              Desde
              <input type="date" aria-label="Emitida desde" className={`${claseSelect} ml-1`} value={desde} max={hasta || undefined} onChange={alCambiar(setDesde)} />
            </label>
            <label className="d-flex align-items-center small mb-0">
              Hasta
              <input type="date" aria-label="Emitida hasta" className={`${claseSelect} ml-1`} value={hasta} min={desde || undefined} onChange={alCambiar(setHasta)} />
            </label>
            <label className="d-flex align-items-center small mb-0">
              <input
                type="checkbox"
                checked={incluirAnuladas}
                onChange={(e) => {
                  setIncluirAnuladas(e.target.checked)
                  setPagina(1)
                }}
                className="mr-2"
              />
              Mostrar anuladas
            </label>
          </>
        }
        vacio={<EmptyState titulo="No hay facturas" descripcion="Ajuste los filtros o emita una factura." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />
    </div>
  )
}
