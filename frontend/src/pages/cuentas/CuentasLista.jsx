import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { listarCuentas } from '../../api/cuentas.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge, { BadgeActivo } from '../../components/Badge.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import PacienteSelector from '../../components/PacienteSelector.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosFacturacion } from '../../hooks/useDatosFacturacion.js'
import { formatearFechaHora, formatearMoneda } from '../../utils/formato.js'
import { nombrePaciente } from '../citas/utilsCita.js'
import { ESTADOS_CUENTA, tonoEstadoCuenta } from './utilsCuenta.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'

export default function CuentasLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const puedeCrear = puede(rol, 'cuentas', 'crear')

  const [texto, setTexto] = useState('')
  const [idPaciente, setIdPaciente] = useState(params.get('idPaciente') ?? '')
  const idHospitalizacion = params.get('idHospitalizacion') ?? ''
  const [estado, setEstado] = useState('')
  const [incluirInactivas, setIncluirInactivas] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')

  // Paciente, hospitalización, estado y activas se envían al servidor (combinables); la búsqueda por texto se resuelve aquí
  const idPacienteNum = idPaciente && /^\d+$/.test(idPaciente) ? idPaciente : ''
  const { datos: cuentas, error, cargando } = useApi(`cuentas|${idPacienteNum}|${idHospitalizacion}|${estado}|${incluirInactivas}`, () =>
    listarCuentas({ idPaciente: idPacienteNum, idHospitalizacion, estado, soloActivas: !incluirInactivas }),
  )

  // Nombres de los pacientes de las cuentas listadas (GET /api/paciente/{id}/resumen, también permitido a CAJERO)
  const datos = useDatosFacturacion((cuentas ?? []).map((c) => c.idPaciente))

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase().replace(/^#/, '')
    return (cuentas ?? [])
      .filter((c) => {
        if (!busqueda) return true
        return String(c.idCuenta) === busqueda || nombrePaciente(datos.pacientesPorId[c.idPaciente], c.idPaciente).toLowerCase().includes(busqueda)
      })
      .sort((a, b) => b.idCuenta - a.idCuenta)
  }, [cuentas, texto, datos.pacientesPorId])

  const alCambiar = (setter) => (e) => {
    setter(e.target.value)
    setPagina(1)
  }

  const columnas = [
    {
      clave: 'numero',
      titulo: 'Cuenta',
      render: (c) => (
        <Link to={`/cuentas/${c.idCuenta}`} className="font-weight-bold">
          #{c.idCuenta}
        </Link>
      ),
    },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (c) => (
        <>
          {nombrePaciente(datos.pacientesPorId[c.idPaciente], c.idPaciente)}
          {c.idHospitalizacion && <div className="text-muted small">Hospitalización #{c.idHospitalizacion}</div>}
        </>
      ),
    },
    { clave: 'apertura', titulo: 'Apertura', render: (c) => formatearFechaHora(c.fechaApertura) },
    { clave: 'saldo', titulo: 'Saldo', className: 'text-right', render: (c) => <strong className="text-nowrap">{formatearMoneda(c.saldo)}</strong> },
    {
      clave: 'estado',
      titulo: 'Estado',
      render: (c) => (
        <span className="d-inline-flex align-items-center acciones-grupo">
          <Badge tono={tonoEstadoCuenta(c.estado)}>{c.estado}</Badge>
          {c.activo === false && <BadgeActivo activo={false} />}
        </span>
      ),
    },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (c) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/cuentas/${c.idCuenta}`}>Ver</Link>
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
          <h1 className="h3 mb-0">Cuentas</h1>
          <p className="text-muted mb-0">
            Cuentas hospitalarias de los pacientes.
            {idHospitalizacion && ` Filtrando por la hospitalización #${idHospitalizacion}.`}
          </p>
        </div>
        {puedeCrear && (
          <Link to="/cuentas/nueva" className="btn btn-primary btn-round">
            Nueva cuenta
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(c) => c.idCuenta}
        cargando={cargando || datos.cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por N.º de cuenta o paciente"
        acciones={
          <>
            {filtroPaciente}
            <select aria-label="Filtrar por estado" className={claseSelect} value={estado} onChange={alCambiar(setEstado)}>
              <option value="">Todos los estados</option>
              {ESTADOS_CUENTA.map((e) => (
                <option key={e} value={e}>
                  {e}
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
        vacio={<EmptyState titulo="No hay cuentas" descripcion="Ajuste los filtros o abra una cuenta." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />
    </div>
  )
}
