import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap'
import { ajustarSaldoCuenta, desactivarCuenta, obtenerCuenta } from '../../api/cuentas.js'
import { listarPagosDeCuenta } from '../../api/pagos.js'
import { useAuth } from '../../auth/useAuth.js'
import Badge, { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosFacturacion } from '../../hooks/useDatosFacturacion.js'
import { aCentavos, centavosATexto, deCentavos } from '../../utils/dinero.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFechaHora, formatearMoneda } from '../../utils/formato.js'
import { nombrePaciente } from '../citas/utilsCita.js'
import { tonoEstadoCuenta } from './utilsCuenta.js'

/**
 * Ajuste de saldo: positivo suma, negativo resta. El backend responde 422 si el monto es 0 o tiene más de 2 decimales, si el
 * saldo resultante es negativo, o si la cuenta está desactivada o Cerrada. Con saldo 0 una cuenta Abierta pasa a Liquidada y
 * una Liquidada vuelve a Abierta si el nuevo saldo es mayor que 0.
 */
function AjusteSaldoDialog({ cuenta, onCerrar, onListo }) {
  const [monto, setMonto] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const cambioCent = aCentavos(monto)
  const valido = cambioCent !== null && cambioCent !== 0
  const actualCent = aCentavos(cuenta.saldo) ?? 0
  const resultado = valido ? actualCent + cambioCent : null

  const confirmar = async () => {
    setGuardando(true)
    setError('')
    try {
      await ajustarSaldoCuenta(cuenta.idCuenta, deCentavos(cambioCent))
      onListo(`Saldo ajustado (${cambioCent > 0 ? '+' : ''}${centavosATexto(cambioCent)}).`)
    } catch (e) {
      // 400: monto ausente o con formato inválido; 404: no existe; 422: reglas anteriores
      setError(normalizarError(e).message)
      setGuardando(false)
    }
  }

  return (
    <Modal isOpen toggle={guardando ? undefined : onCerrar} centered fade={false}>
      <ModalHeader closeAriaLabel="Cerrar" toggle={guardando ? undefined : onCerrar}>
        Ajustar saldo
      </ModalHeader>
      <ModalBody>
        <p>
          Cuenta #{cuenta.idCuenta}: saldo actual <strong>{formatearMoneda(cuenta.saldo)}</strong>
        </p>
        <label htmlFor="ajuste-saldo" className="d-block font-weight-bold small mb-1">
          Monto del ajuste (Q)
        </label>
        <input
          id="ajuste-saldo"
          type="text"
          inputMode="decimal"
          value={monto}
          onChange={(e) => setMonto(e.target.value)}
          className={`form-control${monto !== '' && !valido ? ' is-invalid' : ''}`}
          placeholder="Ej. 500.00 (suma) o -120.50 (resta)"
        />
        <small className="form-text text-muted">Positivo suma al saldo y negativo resta. Hasta 2 decimales; no puede ser cero.</small>
        {monto !== '' && !valido && <div className="invalid-feedback d-block">Ingrese un monto distinto de cero con hasta 2 decimales.</div>}
        {resultado !== null && (
          <p className={`mt-3 mb-0${resultado < 0 ? ' text-danger' : ''}`}>
            Saldo resultante: <strong>{formatearMoneda(deCentavos(resultado))}</strong>
            {resultado < 0 && ' (no permitido)'}
            {resultado === 0 && cuenta.estado === 'Abierta' && ' (la cuenta pasará a Liquidada)'}
            {resultado > 0 && cuenta.estado === 'Liquidada' && ' (la cuenta volverá a Abierta)'}
          </p>
        )}
        {error && (
          <Alert color="danger" fade={false} role="alert" className="mt-3 mb-0">
            {error}
          </Alert>
        )}
      </ModalBody>
      <ModalFooter>
        <Button color="secondary" outline onClick={onCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button color="primary" onClick={confirmar} disabled={!valido || resultado < 0 || guardando}>
          {guardando ? 'Guardando...' : 'Aplicar ajuste'}
        </Button>
      </ModalFooter>
    </Modal>
  )
}

export default function CuentaDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'cuentas', 'editar')
  const puedeDesactivar = puede(rol, 'cuentas', 'desactivar')
  const puedeAjustar = puede(rol, 'cuentas', 'ajustarSaldo')
  const puedeVerPagos = puede(rol, 'pagos', 'ver')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [ajustando, setAjustando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  const { datos: carga, error, cargando, recargar } = useApi(`cuenta|${id}`, async () => {
    const cuenta = await obtenerCuenta(id)
    const pagos = puedeVerPagos ? await listarPagosDeCuenta(id) : null
    return { cuenta, pagos }
  })

  // Nombre del paciente con GET /api/paciente/{id}/resumen (permitido también a CAJERO)
  const datos = useDatosFacturacion(carga ? [carga.cuenta.idPaciente] : [])

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const desactivar = async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarCuenta(id)
      setConfirmando(false)
      setExito('La cuenta fue desactivada.')
      recargar()
    } catch (e) {
      // 404: no existe; 409: tiene pagos registrados; 422: ya desactivada
      setErrorDesactivar(normalizarError(e).message)
    } finally {
      setDesactivando(false)
    }
  }

  if (cargando || datos.cargando) return <Loading texto="Cargando cuenta..." />

  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Cuenta no encontrada' : 'No se pudo cargar la cuenta'} mensaje={error.message} />
        <Link to="/cuentas" className="volver-enlace">
          &larr; Volver a cuentas
        </Link>
      </div>
    )
  }

  const { cuenta: c, pagos } = carga
  const activa = c.activo !== false
  const pagosActivos = (pagos ?? []).filter((p) => p.activo !== false)

  return (
    <div>
      <Link to="/cuentas" className="volver-enlace">
        &larr; Volver a cuentas
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-1">Cuenta #{c.idCuenta}</h1>
          <div className="d-flex align-items-center acciones-grupo">
            <Badge tono={tonoEstadoCuenta(c.estado)}>{c.estado}</Badge>
            <BadgeActivo activo={c.activo} />
          </div>
        </div>
        <div className="acciones-grupo d-flex flex-wrap mt-3 mt-md-0">
          {activa && puedeAjustar && c.estado !== 'Cerrada' && (
            <Button color="primary" className="btn-round" onClick={() => setAjustando(true)}>
              Ajustar saldo
            </Button>
          )}
          {activa && puedeEditar && (
            <Link to={`/cuentas/${c.idCuenta}/editar`} className="btn btn-secondary btn-round">
              Editar
            </Link>
          )}
          {activa && puedeDesactivar && (
            <button
              type="button"
              className="btn btn-danger btn-round"
              onClick={() => {
                setErrorDesactivar('')
                setConfirmando(true)
              }}
            >
              Desactivar
            </button>
          )}
        </div>
      </div>

      <div className="card card-body">
        <div className="row">
          <Dato etiqueta="Paciente">
            {puede(rol, 'pacientes', 'ver') ? (
              <Link to={`/pacientes/${c.idPaciente}`}>{nombrePaciente(datos.pacientesPorId[c.idPaciente], c.idPaciente)}</Link>
            ) : (
              nombrePaciente(datos.pacientesPorId[c.idPaciente], c.idPaciente)
            )}
          </Dato>
          <Dato etiqueta="Hospitalización">
            {c.idHospitalizacion ? (
              puede(rol, 'hospitalizaciones', 'ver') ? (
                <Link to={`/hospitalizaciones/${c.idHospitalizacion}`}>#{c.idHospitalizacion}</Link>
              ) : (
                `#${c.idHospitalizacion}`
              )
            ) : (
              'Sin hospitalización'
            )}
          </Dato>
          <Dato etiqueta="Apertura">{formatearFechaHora(c.fechaApertura)}</Dato>
          <Dato etiqueta="Saldo">
            <strong>{formatearMoneda(c.saldo)}</strong>
          </Dato>
          <Dato etiqueta="Facturas">
            <Link to={`/facturas?idPaciente=${c.idPaciente}`}>Ver facturas del paciente</Link>
          </Dato>
        </div>
      </div>

      {puedeVerPagos && (
        <div className="card card-body">
          <h2 className="h5 mb-3">Pagos aplicados a esta cuenta</h2>
          {pagos.length === 0 ? (
            <EmptyState titulo="Sin pagos" descripcion="Ningún pago se ha registrado con esta cuenta." />
          ) : (
            <div className="table-responsive">
              <table className="table mb-0">
                <thead className="text-primary">
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Factura</th>
                    <th scope="col">Método</th>
                    <th scope="col">Referencia</th>
                    <th scope="col" className="text-right">
                      Monto
                    </th>
                    <th scope="col">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {pagos.map((p) => (
                    <tr key={p.idPago}>
                      <td>{formatearFechaHora(p.fechaPago)}</td>
                      <td>
                        <Link to={`/facturas/${p.idFactura}`}>#{p.idFactura}</Link>
                      </td>
                      <td>{datos.metodosPorId[p.idMetodoPago]?.descripcion ?? '—'}</td>
                      <td>{p.referencia || '—'}</td>
                      <td className="text-right">{formatearMoneda(p.monto)}</td>
                      <td>{p.activo === false ? <Badge tono="rojo">Anulado</Badge> : <Badge tono="verde">Aplicado</Badge>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {pagosActivos.length > 0 && (
            <p className="small text-muted mt-3 mb-0">
              Total aplicado: {formatearMoneda(deCentavos(pagosActivos.reduce((suma, p) => suma + (aCentavos(p.monto) ?? 0), 0)))}
            </p>
          )}
        </div>
      )}

      {ajustando && (
        <AjusteSaldoDialog
          cuenta={c}
          onCerrar={() => setAjustando(false)}
          onListo={(mensaje) => {
            setAjustando(false)
            setExito(mensaje)
            recargar()
          }}
        />
      )}

      <ConfirmDialog
        abierto={confirmando}
        titulo="Desactivar cuenta"
        mensaje="¿Desea desactivar esta cuenta? No se permite si tiene pagos registrados."
        textoConfirmar="Desactivar"
        peligro
        cargando={desactivando}
        error={errorDesactivar}
        onConfirmar={desactivar}
        onCancelar={() => setConfirmando(false)}
      />
    </div>
  )
}
