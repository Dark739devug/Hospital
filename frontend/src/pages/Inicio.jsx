import { Link } from 'react-router-dom'
import { Card, CardBody, Col, Row } from 'reactstrap'
import { listarEstadosFactura, listarEstadosHabitacion } from '../api/catalogos.js'
import { listarMedicamentos } from '../api/medicamentos.js'
import { obtenerResumen } from '../api/reportes.js'
import { useAuth } from '../auth/useAuth.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import TarjetaKpi from '../components/TarjetaKpi.jsx'
import { ADMIN, CAJERO, ENFERMERO, FARMACEUTICO, MEDICO, RECEPCIONISTA, menuParaRol, puede } from '../config/menu.js'
import { useApi } from '../hooks/useApi.js'
import { ETIQUETAS_ROL, formatearFecha } from '../utils/formato.js'

// Roles que pueden leer GET /api/reportes/resumen (según su @PreAuthorize; FARMACEUTICO no)
const ROLES_RESUMEN = [ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, CAJERO]

// Indicadores del resumen: qué rol ve cada uno (solo los pertinentes a su trabajo), a qué módulo enlazan y con qué filtro.
const KPIS = [
  { clave: 'pacientesActivos', titulo: 'Pacientes activos', icono: 'users_single-02', modulo: 'pacientes', roles: [ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA], ruta: () => '/pacientes' },
  { clave: 'citasDelDia', titulo: 'Citas del día', icono: 'ui-1_calendar-60', modulo: 'citas', roles: [ADMIN, MEDICO, RECEPCIONISTA], ruta: (c) => `/citas?fecha=${c.fecha}` },
  { clave: 'hospitalizacionesActivas', titulo: 'Hospitalizaciones activas', icono: 'business_bank', modulo: 'hospitalizaciones', roles: [ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA], ruta: () => '/hospitalizaciones' },
  { clave: 'emergenciasEnCurso', titulo: 'Emergencias en curso', icono: 'health_ambulance', modulo: 'emergencia', roles: [ADMIN, MEDICO, ENFERMERO], ruta: () => '/emergencia?estado=abierta' },
  {
    clave: 'habitacionesDisponibles',
    titulo: 'Habitaciones disponibles',
    icono: 'shopping_shop',
    modulo: 'habitaciones',
    roles: [ADMIN, ENFERMERO, RECEPCIONISTA],
    ruta: (c) => (c.idEstadoDisponible ? `/habitaciones?idEstado=${c.idEstadoDisponible}` : '/habitaciones'),
  },
  { clave: 'medicamentosBajoStock', titulo: 'Medicamentos con stock bajo', icono: 'shopping_box', modulo: 'medicamentos', roles: [ADMIN, FARMACEUTICO], ruta: () => '/medicamentos?stock=alerta', alerta: true },
  {
    clave: 'facturasPendientes',
    titulo: 'Facturas pendientes',
    icono: 'files_single-copy-04',
    modulo: 'facturas',
    roles: [ADMIN, CAJERO, RECEPCIONISTA],
    ruta: (c) => (c.idEstadoPendiente ? `/facturas?estado=${c.idEstadoPendiente}` : '/facturas'),
    alerta: true,
  },
]

const porDescripcion = (lista, texto) => lista.find((e) => (e.descripcion ?? e.nombre)?.toLowerCase() === texto)

/** Barras horizontales simples (HTML/CSS) para comparar los indicadores del día. */
function Panorama({ filas }) {
  const maximo = Math.max(1, ...filas.map((f) => f.valor))
  return (
    <Card>
      <CardBody>
        <h2 className="h5 mb-3">Panorama del día</h2>
        {filas.map((f) => (
          <div key={f.clave} className="mb-2">
            <div className="d-flex justify-content-between small">
              <span>{f.titulo}</span>
              <strong>{f.valor}</strong>
            </div>
            <div className="progress" style={{ height: 8 }}>
              <div
                className="progress-bar"
                role="progressbar"
                aria-valuenow={f.valor}
                aria-valuemin={0}
                aria-valuemax={maximo}
                aria-label={f.titulo}
                style={{ width: `${(f.valor / maximo) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </CardBody>
    </Card>
  )
}

export default function Inicio() {
  const { usuario, rol } = useAuth()
  const grupos = menuParaRol(rol)
  const modulos = grupos.flatMap((g) => g.items)
  const kpisRol = KPIS.filter((k) => k.roles.includes(rol))
  const veResumen = ROLES_RESUMEN.includes(rol)

  // Resumen del hospital; un 403 u otro error no impide mostrar el resto del inicio
  const { datos, error, cargando } = useApi(`inicio|${rol}`, async () => {
    const resumen = veResumen ? await obtenerResumen() : null
    // FARMACEUTICO no puede leer el resumen: el stock bajo sale de la lista de medicamentos (lectura abierta)
    const bajoStock = !veResumen && kpisRol.some((k) => k.clave === 'medicamentosBajoStock')
      ? (await listarMedicamentos({ bajoStock: true, soloActivos: true }).catch(() => null))?.length ?? null
      : null
    // Ids de estado para enlazar con el filtro (solo si el KPI se muestra)
    const [estadosHab, estadosFac] = await Promise.all([
      kpisRol.some((k) => k.clave === 'habitacionesDisponibles') ? listarEstadosHabitacion().catch(() => []) : [],
      kpisRol.some((k) => k.clave === 'facturasPendientes') ? listarEstadosFactura().catch(() => []) : [],
    ])
    return {
      resumen,
      bajoStock,
      idEstadoDisponible: porDescripcion(estadosHab, 'disponible')?.idEstadoHabitacion,
      idEstadoPendiente: porDescripcion(estadosFac, 'pendiente')?.idEstadoFactura,
    }
  })

  const resumen = datos?.resumen
  const contexto = { fecha: resumen?.fecha, idEstadoDisponible: datos?.idEstadoDisponible, idEstadoPendiente: datos?.idEstadoPendiente }
  const valorDe = (k) => (k.clave === 'medicamentosBajoStock' && !resumen ? (datos?.bajoStock ?? null) : (resumen?.[k.clave] ?? null))
  const kpisVisibles = kpisRol.filter((k) => valorDe(k) !== null || cargando)
  const filasPanorama = ['citasDelDia', 'hospitalizacionesActivas', 'emergenciasEnCurso', 'habitacionesDisponibles']
    .map((c) => kpisVisibles.find((k) => k.clave === c))
    .filter(Boolean)
    .map((k) => ({ clave: k.clave, titulo: k.titulo, valor: valorDe(k) ?? 0 }))

  return (
    <div>
      <Card>
        <CardBody>
          <h3 className="mb-1">Bienvenido, {usuario?.username}</h3>
          <p className="text-muted mb-0">
            Perfil: {ETIQUETAS_ROL[rol] ?? rol}
            {resumen?.fecha && <> · Resumen del {formatearFecha(resumen.fecha)}</>}
          </p>
        </CardBody>
      </Card>

      {error && <ErrorAlert tipo="aviso" mensaje={`No se pudo cargar el resumen del hospital: ${error.message}`} />}

      {kpisVisibles.length > 0 && (
        <>
          <Row>
            {kpisVisibles.map((k) => (
              <Col key={k.clave} xs="12" sm="6" xl="4">
                <TarjetaKpi kpi={k} titulo={k.titulo} valor={cargando ? null : valorDe(k)} ruta={puede(rol, k.modulo, 'ver') ? k.ruta(contexto) : null} />
              </Col>
            ))}
          </Row>
          {filasPanorama.length > 1 && !cargando && (
            <Row>
              <Col xs="12" lg="6">
                <Panorama filas={filasPanorama} />
              </Col>
            </Row>
          )}
        </>
      )}

      <h2 className="h5 mt-2 mb-3 text-muted">Módulos</h2>
      {modulos.length > 0 ? (
        <Row>
          {modulos.map((m) => (
            <Col key={m.clave} xs="12" sm="6" xl="4">
              <Link to={m.ruta} className="tarjeta-modulo">
                <Card>
                  <CardBody>
                    <div className="d-flex align-items-center">
                      <div className="mr-3 text-primary" style={{ fontSize: '2rem', lineHeight: 1 }}>
                        <i className={`now-ui-icons ${m.icono ?? 'design_app'}`} />
                      </div>
                      <div className="tarjeta-modulo-texto">
                        <h5 className="mb-0">{m.titulo}</h5>
                        <small className="text-muted">Abrir módulo</small>
                      </div>
                    </div>
                  </CardBody>
                </Card>
              </Link>
            </Col>
          ))}
        </Row>
      ) : (
        <Card>
          <CardBody className="text-muted">No hay módulos disponibles para su perfil.</CardBody>
        </Card>
      )}
    </div>
  )
}
