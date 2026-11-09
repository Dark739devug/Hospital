import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listarFacturas } from '../../api/facturas.js'
import { anularPago, listarPagos } from '../../api/pagos.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pagination from '../../components/Pagination.jsx'
import Table from '../../components/Table.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { useDatosFacturacion } from '../../hooks/useDatosFacturacion.js'
import { aCentavos, deCentavos } from '../../utils/dinero.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFechaHora, formatearMoneda } from '../../utils/formato.js'
import { nombrePaciente } from '../citas/utilsCita.js'

const TAMANO_PAGINA = 10
const claseSelect = 'form-control'

export default function PagosLista() {
  const { rol } = useAuth()
  const puedeAnular = puede(rol, 'pagos', 'desactivar')

  const [texto, setTexto] = useState('')
  const [metodo, setMetodo] = useState('')
  const [estado, setEstado] = useState('')
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState('')
  const [aAnular, setAAnular] = useState(null)
  const [anulando, setAnulando] = useState(false)
  const [errorAnular, setErrorAnular] = useState('')

  const { datos: carga, error, cargando, recargar } = useApi('pagos', async () => {
    const [pagos, facturas] = await Promise.all([listarPagos(), listarFacturas().catch(() => [])])
    return { pagos, facturasPorId: mapaPor(facturas, 'idFactura') }
  })

  // Nombre del paciente de cada pago (vía su factura) con GET /api/paciente/{id}/resumen
  const datos = useDatosFacturacion(Object.values(carga?.facturasPorId ?? {}).map((f) => f.idPaciente))
  const estadoCancelada = datos.idEstado('Cancelada')

  const filas = useMemo(() => {
    if (!carga) return []
    const busqueda = texto.trim().toLowerCase().replace(/^#/, '')
    return carga.pagos
      .filter((p) => !metodo || String(p.idMetodoPago) === metodo)
      .filter((p) => !estado || (estado === 'anulado' ? p.activo === false : p.activo !== false))
      // Fechas como texto ISO (YYYY-MM-DD) para no depender de la zona horaria
      .filter((p) => !desde || String(p.fechaPago).slice(0, 10) >= desde)
      .filter((p) => !hasta || String(p.fechaPago).slice(0, 10) <= hasta)
      .filter((p) => !busqueda || String(p.idFactura) === busqueda || (p.referencia ?? '').toLowerCase().includes(busqueda))
      .sort((a, b) => String(b.fechaPago).localeCompare(String(a.fechaPago)) || b.idPago - a.idPago)
  }, [carga, texto, metodo, estado, desde, hasta])

  const totalCent = filas.filter((p) => p.activo !== false).reduce((suma, p) => suma + (aCentavos(p.monto) ?? 0), 0)

  const alCambiar = (setter) => (e) => {
    setter(e.target.value)
    setPagina(1)
  }

  const anular = async () => {
    setAnulando(true)
    setErrorAnular('')
    try {
      await anularPago(aAnular.idPago)
      setAAnular(null)
      setExito('Pago anulado.')
      recargar()
    } catch (e) {
      // 404: no existe; 403: solo ADMIN; 422: factura Cancelada o pago ya anulado
      setErrorAnular(normalizarError(e).message)
    } finally {
      setAnulando(false)
    }
  }

  // Regla del backend: se anula con la factura Pendiente o Pagada (una Pagada vuelve a Pendiente); con la Cancelada, no
  const sePuedeAnular = (p) => {
    const f = carga.facturasPorId[p.idFactura]
    return puedeAnular && p.activo !== false && f && f.idEstadoFactura !== estadoCancelada
  }

  const columnas = [
    { clave: 'fecha', titulo: 'Fecha', render: (p) => formatearFechaHora(p.fechaPago) },
    {
      clave: 'factura',
      titulo: 'Factura',
      render: (p) => (
        <Link to={`/facturas/${p.idFactura}`} className="font-weight-bold">
          #{p.idFactura}
        </Link>
      ),
    },
    {
      clave: 'paciente',
      titulo: 'Paciente',
      render: (p) => {
        const f = carga.facturasPorId[p.idFactura]
        return f ? nombrePaciente(datos.pacientesPorId[f.idPaciente], f.idPaciente) : '—'
      },
    },
    { clave: 'cuenta', titulo: 'Cuenta', render: (p) => (p.idCuenta ? <Link to={`/cuentas/${p.idCuenta}`}>#{p.idCuenta}</Link> : '—') },
    { clave: 'metodo', titulo: 'Método', render: (p) => datos.metodosPorId[p.idMetodoPago]?.descripcion ?? '—' },
    { clave: 'referencia', titulo: 'Referencia', render: (p) => p.referencia || '—' },
    { clave: 'monto', titulo: 'Monto', className: 'text-right', render: (p) => <strong className="text-nowrap">{formatearMoneda(p.monto)}</strong> },
    { clave: 'estado', titulo: 'Estado', render: (p) => (p.activo === false ? <Badge tono="rojo">Anulado</Badge> : <Badge tono="verde">Aplicado</Badge>) },
    {
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (p) => (
        <div className="tabla-acciones d-flex justify-content-end">
          <Link to={`/facturas/${p.idFactura}`}>Ver factura</Link>
          {sePuedeAnular(p) && (
            <button type="button" className="enlace-boton text-danger" onClick={() => { setErrorAnular(''); setAAnular(p) }}>
              Anular
            </button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <div className="card card-body">
        <h1 className="h3 mb-0">Pagos</h1>
        <p className="text-muted mb-0">Pagos registrados. Para registrar uno nuevo, abra la factura correspondiente.</p>
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(p) => p.idPago}
        cargando={cargando || datos.cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar por N.º de factura o referencia"
        acciones={
          <>
            <select aria-label="Filtrar por método" className={claseSelect} value={metodo} onChange={alCambiar(setMetodo)}>
              <option value="">Todos los métodos</option>
              {datos.metodos.map((m) => (
                <option key={m.idMetodoPago} value={m.idMetodoPago}>
                  {m.descripcion}
                </option>
              ))}
            </select>
            <select aria-label="Filtrar por estado" className={claseSelect} value={estado} onChange={alCambiar(setEstado)}>
              <option value="">Aplicados y anulados</option>
              <option value="aplicado">Aplicados</option>
              <option value="anulado">Anulados</option>
            </select>
            <label className="d-flex align-items-center small mb-0">
              Desde
              <input type="date" aria-label="Pagados desde" className={`${claseSelect} ml-1`} value={desde} max={hasta || undefined} onChange={alCambiar(setDesde)} />
            </label>
            <label className="d-flex align-items-center small mb-0">
              Hasta
              <input type="date" aria-label="Pagados hasta" className={`${claseSelect} ml-1`} value={hasta} min={desde || undefined} onChange={alCambiar(setHasta)} />
            </label>
          </>
        }
        vacio={<EmptyState titulo="No hay pagos" descripcion="Ajuste los filtros o registre un pago desde una factura." />}
        pie={
          <>
            <p className="small text-muted mt-3 mb-0">
              Total aplicado en la selección: <strong>{formatearMoneda(deCentavos(totalCent))}</strong> ({filas.filter((p) => p.activo !== false).length} pagos)
            </p>
            <Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />
          </>
        }
      />

      <ConfirmDialog
        abierto={Boolean(aAnular)}
        titulo="Anular pago"
        mensaje={aAnular ? `¿Anular el pago de ${formatearMoneda(aAnular.monto)} de la factura #${aAnular.idFactura}? El saldo pendiente de la factura aumentará (si estaba pagada volverá a Pendiente).` : ''}
        textoConfirmar="Anular pago"
        peligro
        cargando={anulando}
        error={errorAnular}
        onConfirmar={anular}
        onCancelar={() => setAAnular(null)}
      />
    </div>
  )
}
