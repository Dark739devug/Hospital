import { useState } from 'react'
import { agregarMedicamentoConsulta, quitarMedicamentoConsulta } from '../../api/consultas.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearMoneda } from '../../utils/formato.js'

/**
 * Medicamentos de un tratamiento dentro de la consulta. Cada uno se cobra en la factura de la consulta
 * (cantidad x precio del medicamento); al quitarlo se quita también su línea de la factura.
 */
export default function MedicamentosTratamiento({ idConsulta, idTratamiento, items, medicamentos, editable, onCambio, onError }) {
  const [idMedicamento, setIdMedicamento] = useState('')
  const [cantidad, setCantidad] = useState('1')
  const [guardando, setGuardando] = useState(false)

  const asignados = new Set(items.map((i) => i.idMedicamento))
  const disponibles = medicamentos.filter((m) => !asignados.has(m.idMedicamento))
  const n = Number(cantidad)
  const cantidadValida = Number.isInteger(n) && n >= 1 && n <= 1000

  const agregar = async () => {
    setGuardando(true)
    try {
      await agregarMedicamentoConsulta(idConsulta, { idTratamiento, idMedicamento: Number(idMedicamento), cantidad: n })
      const nombre = medicamentos.find((m) => String(m.idMedicamento) === idMedicamento)?.nombreComercial ?? 'Medicamento'
      setIdMedicamento('')
      setCantidad('1')
      onCambio(`${nombre} agregado al tratamiento y a la factura de la consulta.`)
    } catch (e) {
      onError(normalizarError(e).message)
    } finally {
      setGuardando(false)
    }
  }

  const quitar = async (item) => {
    setGuardando(true)
    try {
      await quitarMedicamentoConsulta(idConsulta, idTratamiento, item.idMedicamento)
      onCambio(`${item.concepto} quitado del tratamiento y de la factura.`)
    } catch (e) {
      onError(normalizarError(e).message)
    } finally {
      setGuardando(false)
    }
  }

  if (!editable && items.length === 0) return null

  return (
    <div className="ml-3 mt-1">
      {items.map((i) => (
        <div key={i.idDetalle} className="d-flex justify-content-between align-items-center small">
          <span>
            {i.concepto} × {i.cantidad}
          </span>
          <span className="d-flex align-items-center acciones-grupo">
            <span className="text-muted">{formatearMoneda(i.importe)}</span>
            {editable && (
              <button type="button" className="btn btn-link text-danger p-0" disabled={guardando} onClick={() => quitar(i)}>
                Quitar
              </button>
            )}
          </span>
        </div>
      ))}

      {editable && (
        <div className="d-flex flex-wrap align-items-center acciones-grupo mt-1">
          <select
            aria-label="Medicamento"
            className="form-control form-control-sm w-auto"
            value={idMedicamento}
            onChange={(e) => setIdMedicamento(e.target.value)}
            disabled={guardando}
          >
            <option value="">{disponibles.length ? 'Agregar medicamento...' : 'No hay medicamentos disponibles'}</option>
            {disponibles.map((m) => (
              <option key={m.idMedicamento} value={m.idMedicamento} disabled={m.stock <= 0}>
                {m.nombreComercial}
                {m.presentacion ? ` (${m.presentacion})` : ''} · {formatearMoneda(m.precio)}
                {m.stock <= 0 ? ' · sin stock' : ''}
              </option>
            ))}
          </select>
          <input
            aria-label="Cantidad"
            type="number"
            min="1"
            max="1000"
            step="1"
            className={`form-control form-control-sm cantidad-medicamento${cantidad !== '' && !cantidadValida ? ' is-invalid' : ''}`}
            value={cantidad}
            onChange={(e) => setCantidad(e.target.value)}
            disabled={guardando}
          />
          <button
            type="button"
            className="btn btn-outline-secondary btn-round btn-sm mb-0"
            disabled={!idMedicamento || !cantidadValida || guardando}
            onClick={agregar}
          >
            {guardando ? 'Guardando...' : 'Agregar'}
          </button>
        </div>
      )}
    </div>
  )
}
