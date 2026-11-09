import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  anularFactura,
  cambiarEstadoFactura,
  listarLineasFactura,
  obtenerFactura,
  obtenerSaldoFactura,
  quitarLinea,
} from '../../api/facturas.js'
import { anularPago, listarPagosDeFactura } from '../../api/pagos.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import EstadoFacturaBadge from '../../components/EstadoFacturaBadge.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosFacturacion } from '../../hooks/useDatosFacturacion.js'
import { aCentavos, deCentavos } from '../../utils/dinero.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFechaHora, formatearMoneda } from '../../utils/formato.js'
import { nombrePaciente } from '../citas/utilsCita.js'
import { LineaDialog, PagoDialog } from './DialogosFactura.jsx'

const cent = (v) => aCentavos(v) ?? 0

export default function FacturaDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'facturas', 'editar')
  const puedeAnular = puede(rol, 'facturas', 'desactivar')
  const puedeEstado = puede(rol, 'facturas', 'cambiarEstado')
  const puedeAgregarLinea = puede(rol, 'facturas', 'agregarLinea')
  const puedeEditarLinea = puede(rol, 'facturas', 'editarLinea')
  const puedeQuitarLinea = puede(rol, 'facturas', 'quitarLinea')
  const puedeVerPagos = puede(rol, 'pagos', 'ver')
  const puedePagar = puede(rol, 'pagos', 'crear')
  const puedeAnularPago = puede(rol, 'pagos', 'desactivar')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [errorAccion, setErrorAccion] = useState('')
  const [dialogoLinea, setDialogoLinea] = useState(null) // { linea } | null
  const [pagando, setPagando] = useState(false)
  const [confirmacion, setConfirmacion] = useState(null) // { tipo, dato }
  const [procesando, setProcesando] = useState(false)
  const [errorConfirmacion, setErrorConfirmacion] = useState('')

  // Factura, saldo, líneas y pagos se piden juntos: tras cada cambio el backend recalcula totales y saldo
  const { datos: carga, error, cargando, recargar } = useApi(`factura|${id}`, async () => {
    const factura = await obtenerFactura(id)
    const [saldo, lineas, pagos] = await Promise.all([
      obtenerSaldoFactura(id),
      listarLineasFactura(id),
      puedeVerPagos ? listarPagosDeFactura(id) : null,
    ])
    return { factura, saldo, lineas, pagos }
  })

  // Nombre del paciente con GET /api/paciente/{id}/resumen (permitido también a CAJERO)
  const datos = useDatosFacturacion(carga ? [carga.factura.idPaciente] : [])

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  if (cargando || datos.cargando) return <Loading texto="Cargando factura..." />

  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Factura no encontrada' : 'No se pudo cargar la factura'} mensaje={error.message} />
        <Link to="/facturas" className="volver-enlace">
          &larr; Volver a facturas
        </Link>
      </div>
    )
  }

  const { factura: f, saldo, lineas, pagos } = carga
  const estado = datos.estadosPorId[f.idEstadoFactura]
  const nombreEstado = estado?.descripcion?.toLowerCase()
  const activa = f.activo !== false
  const pendiente = nombreEstado === 'pendiente'
  const pagada = nombreEstado === 'pagada'
  const editable = activa && pendiente // las líneas solo se modifican con la factura activa y pendiente (RN-146)
  const saldoCent = cent(saldo.saldoPendiente)
  const totalCent = cent(f.total)
  const pagosActivos = (pagos ?? []).filter((p) => p.activo !== false)
  // Anular un pago: con la factura Pendiente o Pagada (una Pagada vuelve a Pendiente); con la Cancelada no
  const sePuedenAnularPagos = activa && nombreEstado !== 'cancelada'

  const alListo = (mensaje) => {
    setDialogoLinea(null)
    setPagando(false)
    setErrorAccion('')
    setExito(mensaje)
    recargar()
  }

  const ejecutar = async (accion, mensajeExito) => {
    setProcesando(true)
    setErrorConfirmacion('')
    try {
      await accion()
      setConfirmacion(null)
      setExito(mensajeExito)
      recargar()
    } catch (e) {
      setErrorConfirmacion(normalizarError(e).message)
    } finally {
      setProcesando(false)
    }
  }

  const cambiarEstado = (nombre, textoExito) =>
    ejecutar(() => cambiarEstadoFactura(f.idFactura, datos.idEstado(nombre)), textoExito)

  const confirmar = () => {
    const { tipo, dato } = confirmacion
    if (tipo === 'pagada') return cambiarEstado('Pagada', 'La factura quedó marcada como pagada.')
    if (tipo === 'cancelada') return cambiarEstado('Cancelada', 'La factura fue cancelada.')
    if (tipo === 'linea') return ejecutar(() => quitarLinea(dato.idDetalle), 'Línea eliminada; el backend recalculó los totales.')
    if (tipo === 'pago') {
      return ejecutar(
        () => anularPago(dato.idPago),
        pagada ? 'Pago anulado: la factura volvió a estado Pendiente.' : 'Pago anulado.',
      )
    }
    // anular factura: 404 no existe; 422 está Pagada
    return ejecutar(() => anularFactura(f.idFactura), 'La factura fue anulada.')
  }

  const textos = {
    pagada: ['Marcar como pagada', 'La factura pasará a estado Pagada y ya no se podrán modificar sus líneas ni importes.', 'Marcar como pagada', false],
    cancelada: ['Cancelar factura', 'La factura pasará a estado Cancelada y ya no se podrán modificar sus líneas ni importes.', 'Cancelar factura', true],
    linea: ['Quitar línea', confirmacion?.dato ? `¿Quitar "${confirmacion.dato.concepto}" de la factura? Los totales se recalcularán.` : '', 'Quitar', true],
    pago: [
      'Anular pago',
      confirmacion?.dato
        ? `¿Anular el pago de ${formatearMoneda(confirmacion.dato.monto)}? El saldo pendiente aumentará${pagada ? ' y la factura volverá a Pendiente' : ''}.`
        : '',
      'Anular pago',
      true,
    ],
    anular: ['Anular factura', 'La factura se desactiva (borrado lógico). No se permite si está pagada o tiene pagos activos.', 'Anular factura', true],
  }
  const textoConf = textos[confirmacion?.tipo] ?? ['', '', 'Confirmar', false]

  const abrirConfirmacion = (tipo, dato) => {
    setErrorConfirmacion('')
    setConfirmacion({ tipo, dato })
  }

  const columnasLineas = [
    { clave: 'concepto', titulo: 'Concepto' },
    { clave: 'cantidad', titulo: 'Cantidad', className: 'text-right' },
    { clave: 'precio', titulo: 'Precio unitario', className: 'text-right' },
    { clave: 'importe', titulo: 'Importe', className: 'text-right' },
  ]

  return (
    <div>
      <Link to="/facturas" className="volver-enlace">
        &larr; Volver a facturas
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={errorAccion} onCerrar={() => setErrorAccion('')} />

      <div className="card card-body d-flex flex-column flex-lg-row align-items-lg-center justify-content-lg-between">
        <div>
          <h1 className="h3 mb-1">Factura #{f.idFactura}</h1>
          <EstadoFacturaBadge estado={estado} activo={f.activo} />
        </div>
        <div className="acciones-grupo d-flex flex-wrap mt-3 mt-lg-0">
          {activa && pendiente && puedePagar && saldoCent > 0 && (
            <button type="button" className="btn btn-primary btn-round" onClick={() => setPagando(true)}>
              Registrar pago
            </button>
          )}
          {activa && pendiente && puedeEstado && (
            <button
              type="button"
              className="btn btn-success btn-round"
              disabled={saldoCent > 0 || totalCent === 0}
              title={saldoCent > 0 || totalCent === 0 ? 'Disponible cuando el total sea mayor a cero y el saldo pendiente sea cero' : undefined}
              onClick={() => abrirConfirmacion('pagada')}
            >
              Marcar como pagada
            </button>
          )}
          {activa && pendiente && puedeEstado && (
            <button
              type="button"
              className="btn btn-outline-secondary btn-round"
              disabled={saldo.cantidadPagos > 0}
              title={saldo.cantidadPagos > 0 ? 'Tiene pagos aplicados: anúlelos antes de cancelar la factura' : undefined}
              onClick={() => abrirConfirmacion('cancelada')}
            >
              Cancelar factura
            </button>
          )}
          {activa && puedeEditar && (
            <Link to={`/facturas/${f.idFactura}/editar`} className="btn btn-secondary btn-round">
              Editar
            </Link>
          )}
          {activa && puedeAnular && (
            <button
              type="button"
              className="btn btn-danger btn-round"
              disabled={pagada || saldo.cantidadPagos > 0}
              title={pagada || saldo.cantidadPagos > 0 ? 'No se puede anular una factura pagada o con pagos activos: anule primero los pagos' : undefined}
              onClick={() => abrirConfirmacion('anular')}
            >
              Anular
            </button>
          )}
        </div>
      </div>

      <div className="card card-body">
        <div className="row">
          <Dato etiqueta="Paciente">
            {puede(rol, 'pacientes', 'ver') ? (
              <Link to={`/pacientes/${f.idPaciente}`}>{nombrePaciente(datos.pacientesPorId[f.idPaciente], f.idPaciente)}</Link>
            ) : (
              nombrePaciente(datos.pacientesPorId[f.idPaciente], f.idPaciente)
            )}
          </Dato>
          <Dato etiqueta="Hospitalización">
            {f.idHospitalizacion ? (
              puede(rol, 'hospitalizaciones', 'ver') ? (
                <Link to={`/hospitalizaciones/${f.idHospitalizacion}`}>#{f.idHospitalizacion}</Link>
              ) : (
                `#${f.idHospitalizacion}`
              )
            ) : (
              'Sin hospitalización'
            )}
          </Dato>
          <Dato etiqueta="Fecha de emisión">{formatearFechaHora(f.fechaEmision)}</Dato>
          <Dato etiqueta="Método de pago">{datos.metodosPorId[f.idMetodoPago]?.descripcion}</Dato>
        </div>
      </div>

      <div className="card card-body">
        <h2 className="h5 mb-3">Totales y saldo</h2>
        <div className="row">
          <Dato etiqueta="Subtotal">{formatearMoneda(f.subtotal)}</Dato>
          <Dato etiqueta="IVA">{formatearMoneda(f.iva)}</Dato>
          <Dato etiqueta="Total">
            <strong>{formatearMoneda(f.total)}</strong>
          </Dato>
          <Dato etiqueta="Pagado">
            {formatearMoneda(saldo.totalPagado)} <span className="text-muted small">({saldo.cantidadPagos} {saldo.cantidadPagos === 1 ? 'pago' : 'pagos'})</span>
          </Dato>
          <Dato etiqueta="Saldo pendiente">
            <strong className={saldoCent > 0 ? 'text-danger' : 'text-success'}>{formatearMoneda(saldo.saldoPendiente)}</strong>{' '}
            {saldo.liquidada && totalCent > 0 && <Badge tono="verde">Liquidada</Badge>}
          </Dato>
        </div>
        {cent(saldo.totalPagado) > cent(saldo.total) && (
          <ErrorAlert tipo="aviso" mensaje={`Los pagos activos (${formatearMoneda(saldo.totalPagado)}) superan el total de la factura. El backend permite este exceso; revise y anule el pago sobrante.`} />
        )}
        <p className="small text-muted border-top pt-3 mb-0">
          El subtotal sale de las líneas; el servidor calcula el IVA y el total y los recalcula con cada cambio de líneas.
        </p>
      </div>

      <div className="card card-body">
        <div className="d-flex flex-wrap align-items-center justify-content-between mb-3">
          <h2 className="h5 mb-0">Líneas de la factura</h2>
          {editable && puedeAgregarLinea && (
            <button type="button" className="btn btn-primary btn-round btn-sm" onClick={() => setDialogoLinea({ linea: null })}>
              Agregar línea
            </button>
          )}
        </div>
        {!editable && <p className="text-muted small">Las líneas solo se pueden modificar mientras la factura esté activa y pendiente.</p>}
        {lineas.length === 0 ? (
          <EmptyState titulo="Sin líneas" descripcion={editable ? 'Agregue los conceptos cobrados.' : 'Esta factura no tiene líneas.'} />
        ) : (
          <div className="table-responsive">
            <table className="table mb-0">
              <thead className="text-primary">
                <tr>
                  {columnasLineas.map((c) => (
                    <th key={c.clave} scope="col" className={c.className ?? ''}>
                      {c.titulo}
                    </th>
                  ))}
                  {editable && (puedeEditarLinea || puedeQuitarLinea) && (
                    <th scope="col" className="text-right">
                      Acciones
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {lineas.map((l) => (
                  <tr key={l.idDetalle}>
                    <td>{l.concepto}</td>
                    <td className="text-right">{l.cantidad}</td>
                    <td className="text-right">{formatearMoneda(l.precioUnitario)}</td>
                    <td className="text-right">{formatearMoneda(l.importe)}</td>
                    {editable && (puedeEditarLinea || puedeQuitarLinea) && (
                      <td className="text-right">
                        <div className="tabla-acciones d-flex justify-content-end">
                          {puedeEditarLinea && (
                            <button type="button" className="enlace-boton text-primary" onClick={() => setDialogoLinea({ linea: l })}>
                              Editar
                            </button>
                          )}
                          {puedeQuitarLinea && (
                            <button type="button" className="enlace-boton text-danger" onClick={() => abrirConfirmacion('linea', l)}>
                              Quitar
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <th scope="row" colSpan={3} className="text-right">
                    Subtotal de las líneas
                  </th>
                  <td className="text-right font-weight-bold">{formatearMoneda(f.subtotal)}</td>
                  {editable && (puedeEditarLinea || puedeQuitarLinea) && <td />}
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </div>

      {puedeVerPagos && (
        <div className="card card-body">
          <h2 className="h5 mb-3">Pagos</h2>
          {pagos.length === 0 ? (
            <EmptyState titulo="Sin pagos" descripcion="Todavía no se ha registrado ningún pago para esta factura." />
          ) : (
            <div className="table-responsive">
              <table className="table mb-0">
                <thead className="text-primary">
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Método</th>
                    <th scope="col">Referencia</th>
                    <th scope="col">Cuenta</th>
                    <th scope="col" className="text-right">
                      Monto
                    </th>
                    <th scope="col">Estado</th>
                    {puedeAnularPago && sePuedenAnularPagos && (
                      <th scope="col" className="text-right">
                        Acciones
                      </th>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {pagos.map((p) => (
                    <tr key={p.idPago}>
                      <td>{formatearFechaHora(p.fechaPago)}</td>
                      <td>{datos.metodosPorId[p.idMetodoPago]?.descripcion ?? '—'}</td>
                      <td>{p.referencia || '—'}</td>
                      <td>{p.idCuenta ? <Link to={`/cuentas/${p.idCuenta}`}>#{p.idCuenta}</Link> : '—'}</td>
                      <td className="text-right">{formatearMoneda(p.monto)}</td>
                      <td>{p.activo === false ? <Badge tono="rojo">Anulado</Badge> : <Badge tono="verde">Aplicado</Badge>}</td>
                      {puedeAnularPago && sePuedenAnularPagos && (
                        <td className="text-right">
                          {p.activo !== false && (
                            <button type="button" className="enlace-boton text-danger" onClick={() => abrirConfirmacion('pago', p)}>
                              Anular
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {pagosActivos.length > 0 && (
            <p className="small text-muted mt-3 mb-0">
              Total aplicado: {formatearMoneda(deCentavos(pagosActivos.reduce((suma, p) => suma + cent(p.monto), 0)))}
            </p>
          )}
        </div>
      )}

      {dialogoLinea && (
        <LineaDialog idFactura={f.idFactura} linea={dialogoLinea.linea} onCerrar={() => setDialogoLinea(null)} onListo={alListo} />
      )}
      {pagando && (
        <PagoDialog factura={f} saldoCentavos={saldoCent} metodos={datos.metodos} onCerrar={() => setPagando(false)} onListo={alListo} />
      )}

      <ConfirmDialog
        abierto={Boolean(confirmacion)}
        titulo={textoConf[0]}
        mensaje={textoConf[1]}
        textoConfirmar={textoConf[2]}
        peligro={textoConf[3]}
        cargando={procesando}
        error={errorConfirmacion}
        onConfirmar={confirmar}
        onCancelar={() => setConfirmacion(null)}
      />
    </div>
  )
}
