import { useState } from 'react'
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap'
import { ajustarStock } from '../api/medicamentos.js'
import { normalizarError } from '../utils/errores.js'

/** Diálogo de ajuste de stock: entrada (+) o salida (-); el stock resultante no puede ser negativo. */
export default function AjusteStockDialog({ medicamento, onCerrar, onListo }) {
  const [cambio, setCambio] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const valor = Number(cambio)
  const valido = cambio !== '' && Number.isInteger(valor) && valor !== 0
  const resultado = valido ? medicamento.stock + valor : null

  const confirmar = async () => {
    setGuardando(true)
    setError('')
    try {
      await ajustarStock(medicamento.idMedicamento, valor)
      onListo(valor)
    } catch (e) {
      // 400: formato inválido; 404: no existe; 422: stock insuficiente / medicamento inactivo / cambio 0
      setError(normalizarError(e).message)
      setGuardando(false)
    }
  }

  return (
    <Modal isOpen toggle={guardando ? undefined : onCerrar} centered fade={false}>
      <ModalHeader closeAriaLabel="Cerrar" toggle={guardando ? undefined : onCerrar}>Ajustar stock</ModalHeader>
      <ModalBody>
        <p>
          {medicamento.nombreComercial}: stock actual <strong>{medicamento.stock}</strong>
          {medicamento.stockMinimo != null && <span className="text-muted"> (mínimo {medicamento.stockMinimo})</span>}
        </p>
        <label htmlFor="cambio-stock" className="d-block font-weight-bold small mb-1">
          Cambio de stock
        </label>
        <input
          id="cambio-stock"
          type="number"
          step="1"
          value={cambio}
          onChange={(e) => setCambio(e.target.value)}
          className={`form-control${cambio !== '' && !valido ? ' is-invalid' : ''}`}
          placeholder="Ej. 50 (entrada) o -10 (salida)"
        />
        <small className="form-text text-muted">Use un valor positivo para entradas y negativo para salidas. No puede ser cero.</small>
        {cambio !== '' && !valido && <div className="invalid-feedback d-block">Ingrese un número entero distinto de cero.</div>}
        {resultado !== null && (
          <p className={`mt-3 mb-0${resultado < 0 ? ' text-danger' : ''}`}>
            Stock resultante: <strong>{resultado}</strong>
            {resultado < 0 && ' (insuficiente)'}
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
