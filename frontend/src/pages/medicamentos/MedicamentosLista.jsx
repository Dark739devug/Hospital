import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { Col, Row } from 'reactstrap'
import { listarMedicamentos } from '../../api/medicamentos.js'
import { listarRecetas } from '../../api/recetas.js'
import { listarTratamientos } from '../../api/tratamientos.js'
import { useAuth } from '../../auth/useAuth.js'
import AjusteStockDialog from '../../components/AjusteStockDialog.jsx'
import Badge, { BadgeActivo } from '../../components/Badge.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pagination from '../../components/Pagination.jsx'
import StockBadge from '../../components/StockBadge.jsx'
import Table from '../../components/Table.jsx'
import TarjetaKpi from '../../components/TarjetaKpi.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { formatearFecha, formatearMoneda } from '../../utils/formato.js'
import { estadoStock } from '../../utils/stock.js'
import { tratamientoVigente } from '../pacientes/utilsPaciente.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'
const FILTROS_STOCK = ['sin', 'bajo', 'alerta', 'suficiente']

/** Medicamentos en el mínimo o por debajo, del más urgente (mayor faltante) al menos urgente. */
function PorReponer({ medicamentos, puedeAjustar, onAjustar }) {
  const lista = medicamentos
    .filter((m) => estadoStock(m).clave !== 'suficiente')
    .sort((a, b) => a.stock - (a.stockMinimo ?? 0) - (b.stock - (b.stockMinimo ?? 0)))
    .slice(0, 6)
  return (
    <div className="card card-body">
      <h2 className="h5 mb-3">Por reponer</h2>
      {lista.length === 0 ? (
        <p className="text-muted mb-0">Todos los medicamentos están sobre su stock mínimo.</p>
      ) : (
        <ul className="lista-divisoria">
          {lista.map((m) => (
            <li key={m.idMedicamento} className="d-flex justify-content-between align-items-center">
              <div>
                <Link to={`/medicamentos/${m.idMedicamento}`} className="font-weight-bold">
                  {m.nombreComercial}
                </Link>
                <div className="small text-muted">
                  Stock {m.stock} · mínimo {m.stockMinimo ?? 0}
                </div>
              </div>
              <div className="d-flex align-items-center acciones-grupo">
                <StockBadge medicamento={m} />
                {puedeAjustar && (
                  <button type="button" className="btn btn-link text-primary p-0" onClick={() => onAjustar(m)}>
                    Ajustar
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Tratamientos vigentes con medicamentos recetados y si hay existencia de cada uno. */
function RecetasVigentes({ datos, medicamentosPorId }) {
  if (!datos) return null
  const tratamientosPorId = mapaPor(datos.tratamientos, 'idTratamiento')
  const porTratamiento = new Map()
  for (const r of datos.recetas) {
    const t = tratamientosPorId[r.idTratamiento]
    if (!t || !tratamientoVigente(t)) continue
    if (!porTratamiento.has(t.idTratamiento)) porTratamiento.set(t.idTratamiento, { tratamiento: t, medicamentos: [] })
    porTratamiento.get(t.idTratamiento).medicamentos.push(medicamentosPorId[r.idMedicamento] ?? { idMedicamento: r.idMedicamento })
  }
  const lista = [...porTratamiento.values()]
    .sort((a, b) => String(b.tratamiento.fechaInicio ?? '').localeCompare(String(a.tratamiento.fechaInicio ?? '')))
    .slice(0, 6)

  return (
    <div className="card card-body">
      <div className="d-flex align-items-baseline justify-content-between mb-3">
        <h2 className="h5 mb-0">Recetas vigentes</h2>
        <Link to="/recetas" className="small">
          Ver todas
        </Link>
      </div>
      {lista.length === 0 ? (
        <p className="text-muted mb-0">No hay tratamientos vigentes con medicamentos.</p>
      ) : (
        <ul className="lista-divisoria">
          {lista.map(({ tratamiento: t, medicamentos }) => (
            <li key={t.idTratamiento}>
              <div className="d-flex justify-content-between align-items-start">
                <Link to={`/tratamientos/${t.idTratamiento}`} className="font-weight-bold">
                  {t.descripcion}
                </Link>
                {t.fechaInicio && <span className="small text-muted text-nowrap ml-2">{formatearFecha(t.fechaInicio)}</span>}
              </div>
              {medicamentos.map((m) => (
                <div key={m.idMedicamento} className="d-flex justify-content-between align-items-center small mt-1">
                  <span>{m.nombreComercial ?? `Medicamento #${m.idMedicamento}`}</span>
                  {m.nombreComercial && (m.stock > 0 ? <span className="text-muted">{m.stock} en stock</span> : <Badge tono="rojo">Sin stock</Badge>)}
                </div>
              ))}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default function MedicamentosLista() {
  const { rol } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const puedeCrear = puede(rol, 'medicamentos', 'crear')
  const puedeEditar = puede(rol, 'medicamentos', 'editar')
  const puedeAjustar = puede(rol, 'medicamentos', 'ajustarStock')

  const [texto, setTexto] = useState('')
  // ?stock=alerta: la tarjeta "Stock bajo o agotado" y el enlace de Inicio filtran la tabla
  const [params, setParams] = useSearchParams()
  const filtroStock = FILTROS_STOCK.includes(params.get('stock')) ? params.get('stock') : ''
  const [filtroReceta, setFiltroReceta] = useState('')
  const [incluirInactivos, setIncluirInactivos] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [aAjustar, setAAjustar] = useState(null)

  // El texto se envía al servidor (criterio) con una pequeña espera para no pedir en cada tecla
  const [criterio, setCriterio] = useState('')
  useEffect(() => {
    const t = setTimeout(() => setCriterio(texto.trim()), 300)
    return () => clearTimeout(t)
  }, [texto])

  // Filtros enviados al servidor: criterio, bajoStock (sin stock / bajo) y soloActivos
  const bajoStock = filtroStock === 'sin' || filtroStock === 'bajo' || filtroStock === 'alerta'
  const soloActivos = !incluirInactivos
  const { datos, error, cargando, recargar } = useApi(`medicamentos|${criterio}|${bajoStock}|${soloActivos}`, () =>
    listarMedicamentos({ criterio, bajoStock, soloActivos }),
  )
  // Inventario activo completo para los indicadores y el panel "Por reponer"
  const { datos: activos, recargar: recargarActivos } = useApi('medicamentos-activos', () => listarMedicamentos({ soloActivos: true }))
  const { datos: recetas } = useApi('farmacia-recetas', async () => {
    const [lista, tratamientos] = await Promise.all([listarRecetas(), listarTratamientos({ soloActivos: true })])
    return { recetas: lista, tratamientos }
  })

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const filas = useMemo(() => {
    return (datos ?? [])
      .filter((m) => !filtroStock || (filtroStock === 'alerta' ? estadoStock(m).clave !== 'suficiente' : estadoStock(m).clave === filtroStock))
      .filter((m) => !filtroReceta || (filtroReceta === 'si' ? m.requiereReceta : !m.requiereReceta))
      .sort((a, b) => a.nombreComercial.localeCompare(b.nombreComercial, 'es'))
  }, [datos, filtroStock, filtroReceta])

  const inventario = activos ?? []
  const enAlerta = inventario.filter((m) => estadoStock(m).clave !== 'suficiente').length
  const conReceta = inventario.filter((m) => m.requiereReceta).length
  const valorInventario = inventario.reduce((total, m) => total + (m.stock ?? 0) * Number(m.precio ?? 0), 0)

  const cambiarFiltroStock = (valor) => {
    const siguiente = new URLSearchParams(params)
    if (valor) siguiente.set('stock', valor)
    else siguiente.delete('stock')
    setParams(siguiente, { replace: true })
    setPagina(1)
  }

  const columnas = [
    {
      clave: 'nombre',
      titulo: 'Medicamento',
      render: (m) => (
        <>
          <Link to={`/medicamentos/${m.idMedicamento}`} className="font-weight-bold">
            {m.nombreComercial}
          </Link>
          {m.nombreGenerico && <div className="small">{m.nombreGenerico}</div>}
          {m.laboratorio && <div className="text-muted small">{m.laboratorio}</div>}
        </>
      ),
    },
    { clave: 'presentacion', titulo: 'Presentación', render: (m) => m.presentacion || '—' },
    { clave: 'precio', titulo: 'Precio', className: 'text-nowrap', render: (m) => formatearMoneda(m.precio) },
    {
      clave: 'stock',
      titulo: 'Stock',
      render: (m) => (
        <>
          <span className="d-flex align-items-center acciones-grupo">
            <strong>{m.stock}</strong>
            <StockBadge medicamento={m} />
          </span>
          <div className="text-muted small">Mínimo {m.stockMinimo ?? 0}</div>
        </>
      ),
    },
    { clave: 'receta', titulo: 'Receta', render: (m) => (m.requiereReceta ? <Badge tono="azul">Requiere</Badge> : <span className="text-muted">No</span>) },
    { clave: 'estado', titulo: 'Estado', render: (m) => <BadgeActivo activo={m.activo} /> },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (m) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/medicamentos/${m.idMedicamento}`}>Ver</Link>
          {puedeEditar && m.activo !== false && <Link to={`/medicamentos/${m.idMedicamento}/editar`}>Editar</Link>}
          {puedeAjustar && m.activo !== false && (
            <button type="button" className="btn btn-link text-primary p-0" onClick={() => setAAjustar(m)}>
              Ajustar stock
            </button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Medicamentos</h1>
          <p className="text-muted mb-0">Inventario de farmacia y recetas vigentes.</p>
        </div>
        {puedeCrear && (
          <Link to="/medicamentos/nuevo" className="btn btn-primary btn-round">
            Nuevo medicamento
          </Link>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Row>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'design_app' }} titulo="Medicamentos activos" valor={activos ? inventario.length : null} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'ui-1_bell-53', alerta: true }} titulo="Stock bajo o agotado" valor={activos ? enAlerta : null} ruta="/medicamentos?stock=alerta" />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'files_paper' }} titulo="Requieren receta" valor={activos ? conReceta : null} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'business_money-coins' }} titulo="Valor del inventario" valor={activos ? formatearMoneda(valorInventario) : null} />
        </Col>
      </Row>

      <Row>
        <Col xl="8">
          <Table
            columnas={columnas}
            filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
            claveFila={(m) => m.idMedicamento}
            cargando={cargando}
            busqueda={texto}
            onBuscar={(v) => {
              setTexto(v)
              setPagina(1)
            }}
            placeholderBusqueda="Buscar por nombre, genérico o laboratorio"
            acciones={
              <>
                <select aria-label="Filtrar por stock" className={claseSelect} value={filtroStock} onChange={(e) => cambiarFiltroStock(e.target.value)}>
                  <option value="">Todo el stock</option>
                  <option value="alerta">Stock bajo o agotado</option>
                  <option value="sin">Sin stock</option>
                  <option value="bajo">Stock bajo</option>
                  <option value="suficiente">Stock suficiente</option>
                </select>
                <select
                  aria-label="Filtrar por receta"
                  className={claseSelect}
                  value={filtroReceta}
                  onChange={(e) => {
                    setFiltroReceta(e.target.value)
                    setPagina(1)
                  }}
                >
                  <option value="">Con y sin receta</option>
                  <option value="si">Requieren receta</option>
                  <option value="no">Sin receta</option>
                </select>
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
              </>
            }
            vacio={<EmptyState titulo="No hay medicamentos" descripcion="Ajuste los filtros o registre un medicamento." />}
            pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
          />
        </Col>
        <Col xl="4">
          <PorReponer medicamentos={inventario} puedeAjustar={puedeAjustar} onAjustar={setAAjustar} />
          <RecetasVigentes datos={recetas} medicamentosPorId={mapaPor(inventario, 'idMedicamento')} />
        </Col>
      </Row>

      {aAjustar && (
        <AjusteStockDialog
          medicamento={aAjustar}
          onCerrar={() => setAAjustar(null)}
          onListo={(cambio) => {
            setExito(`Stock de ${aAjustar.nombreComercial} ${cambio > 0 ? 'aumentado' : 'reducido'} en ${Math.abs(cambio)}.`)
            setAAjustar(null)
            recargar()
            recargarActivos()
          }}
        />
      )}
    </div>
  )
}
