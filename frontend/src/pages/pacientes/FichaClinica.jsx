import { Link } from 'react-router-dom'
import Badge from '../../components/Badge.jsx'
import Dato from '../../components/Dato.jsx'
import { formatearFecha, formatearFechaHora } from '../../utils/formato.js'
import { tratamientoVigente } from './utilsPaciente.js'

// Secciones de la ficha del paciente. Los datos salen del historial (GET /api/reportes/paciente/{id}/historial).

const ordenarDesc = (lista, campo) => [...(lista ?? [])].sort((a, b) => String(b[campo] ?? '').localeCompare(String(a[campo] ?? '')))

// Rangos de referencia de adultos
function estadoPresion(texto) {
  const [s, d] = String(texto).split('/').map(Number)
  if (!s || !d) return null
  if (s >= 140 || d >= 90) return { texto: 'Alta', tono: 'rojo' }
  if (s >= 130 || d >= 80) return { texto: 'Elevada', tono: 'ambar' }
  if (s < 90 || d < 60) return { texto: 'Baja', tono: 'azul' }
  return { texto: 'Normal', tono: 'verde' }
}
const estadoFrecuencia = (fc) => (fc < 60 ? { texto: 'Baja', tono: 'azul' } : fc > 100 ? { texto: 'Alta', tono: 'ambar' } : { texto: 'Normal', tono: 'verde' })
function estadoTemperatura(t) {
  if (t < 36) return { texto: 'Baja', tono: 'azul' }
  if (t <= 37.5) return { texto: 'Normal', tono: 'verde' }
  if (t <= 38) return { texto: 'Febrícula', tono: 'ambar' }
  return { texto: 'Fiebre', tono: 'rojo' }
}
function estadoImc(imc) {
  if (imc < 18.5) return { texto: 'Bajo peso', tono: 'ambar' }
  if (imc < 25) return { texto: 'Normal', tono: 'verde' }
  if (imc < 30) return { texto: 'Sobrepeso', tono: 'ambar' }
  return { texto: 'Obesidad', tono: 'rojo' }
}

function Valor({ valor, unidad, estado }) {
  if (valor == null || valor === '') return null
  return (
    <span className="d-inline-flex align-items-center acciones-grupo">
      <span>
        {valor} {unidad}
      </span>
      {estado && <Badge tono={estado.tono}>{estado.texto}</Badge>}
    </span>
  )
}

/** Signos vitales de la última consulta que los registró. */
export function SignosVitales({ consultas }) {
  const lista = ordenarDesc((consultas ?? []).filter((c) => c.activo !== false), 'fechaHora')
  const ultimo = (campo) => lista.find((c) => c[campo] != null && c[campo] !== '')?.[campo] ?? null
  const ultima = lista[0]
  const pa = ultimo('presionArterial')
  const fc = ultimo('frecuenciaCardiaca')
  const temp = ultimo('temperatura')
  const peso = ultimo('peso')
  const talla = ultimo('talla')
  const imc = peso && talla ? Number(peso) / (Number(talla) / 100) ** 2 : null

  return (
    <div className="card card-body">
      <div className="d-flex flex-wrap align-items-baseline justify-content-between mb-3">
        <h2 className="h5 mb-0">Signos vitales</h2>
        {ultima && (
          <span className="text-muted small">
            Última consulta: <Link to={`/consultas/${ultima.idConsulta}`}>{formatearFechaHora(ultima.fechaHora)}</Link>
          </span>
        )}
      </div>
      {!ultima ? (
        <p className="text-muted mb-0">No hay consultas registradas.</p>
      ) : (
        <div className="row">
          <Dato etiqueta="Presión arterial"><Valor valor={pa} unidad="mmHg" estado={pa ? estadoPresion(pa) : null} /></Dato>
          <Dato etiqueta="Frecuencia cardíaca"><Valor valor={fc} unidad="lpm" estado={fc != null ? estadoFrecuencia(Number(fc)) : null} /></Dato>
          <Dato etiqueta="Temperatura"><Valor valor={temp} unidad="°C" estado={temp != null ? estadoTemperatura(Number(temp)) : null} /></Dato>
          <Dato etiqueta="Peso"><Valor valor={peso} unidad="kg" /></Dato>
          <Dato etiqueta="Talla"><Valor valor={talla} unidad="cm" /></Dato>
          <Dato etiqueta="IMC"><Valor valor={imc != null ? imc.toFixed(1) : null} estado={imc != null ? estadoImc(imc) : null} /></Dato>
        </div>
      )}
    </div>
  )
}

export function TratamientosVigentes({ tratamientos, medicamentosTratamiento, medicamentosPorId }) {
  const vigentes = ordenarDesc((tratamientos ?? []).filter(tratamientoVigente), 'fechaInicio')
  return (
    <div className="card card-body">
      <h2 className="h5 mb-3">Tratamientos vigentes</h2>
      {vigentes.length === 0 ? (
        <p className="text-muted mb-0">No tiene tratamientos vigentes.</p>
      ) : (
        <ul className="lista-divisoria">
          {vigentes.map((t) => {
            const meds = (medicamentosTratamiento ?? [])
              .filter((m) => m.idTratamiento === t.idTratamiento)
              .map((m) => medicamentosPorId[m.idMedicamento]?.nombreComercial ?? `Medicamento #${m.idMedicamento}`)
            return (
              <li key={t.idTratamiento}>
                <Link to={`/tratamientos/${t.idTratamiento}`} className="font-weight-bold">
                  {t.descripcion}
                </Link>
                <div className="text-muted small">
                  {[t.fechaInicio && `Desde ${formatearFecha(t.fechaInicio)}`, t.fechaFin && `hasta ${formatearFecha(t.fechaFin)}`].filter(Boolean).join(' ')}
                </div>
                {meds.length > 0 && <div className="small">{meds.join(', ')}</div>}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

export function DiagnosticosRecientes({ diagnosticos }) {
  const lista = ordenarDesc((diagnosticos ?? []).filter((d) => d.activo !== false), 'fecha').slice(0, 5)
  return (
    <div className="card card-body">
      <h2 className="h5 mb-3">Diagnósticos recientes</h2>
      {lista.length === 0 ? (
        <p className="text-muted mb-0">No tiene diagnósticos registrados.</p>
      ) : (
        <ul className="lista-divisoria">
          {lista.map((d) => (
            <li key={d.idDiagnostico} className="d-flex justify-content-between align-items-start">
              <Link to={`/diagnosticos/${d.idDiagnostico}`} className="font-weight-bold">
                {d.descripcion}
              </Link>
              <span className="text-muted small text-nowrap ml-2">{formatearFecha(d.fecha)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** Médicos de las citas del paciente, del más reciente al más antiguo. */
export function MedicosTratantes({ citas, medicosPorId }) {
  const ultimaCita = new Map()
  for (const c of ordenarDesc((citas ?? []).filter((c) => c.activo !== false), 'fecha')) {
    if (!ultimaCita.has(c.idMedico)) ultimaCita.set(c.idMedico, c.fecha)
  }
  const lista = [...ultimaCita.entries()].slice(0, 5)
  return (
    <div className="card card-body">
      <h2 className="h5 mb-3">Médicos</h2>
      {lista.length === 0 ? (
        <p className="text-muted mb-0">No tiene citas registradas.</p>
      ) : (
        <ul className="lista-divisoria">
          {lista.map(([idMedico, fecha]) => {
            const m = medicosPorId[idMedico]
            return (
              <li key={idMedico} className="d-flex justify-content-between align-items-start">
                <span className="font-weight-bold">{m ? [m.nombre, m.apellidoPaterno].filter(Boolean).join(' ') : `Médico #${idMedico}`}</span>
                <span className="text-muted small text-nowrap ml-2">{formatearFecha(fecha)}</span>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
