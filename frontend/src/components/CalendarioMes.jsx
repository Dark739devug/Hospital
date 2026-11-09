// Calendario mensual que marca los días con citas. Trabaja con fechas 'YYYY-MM-DD' en hora local
// (sin objetos Date en UTC) para no desfasar el día por la zona horaria.
const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const pad = (n) => String(n).padStart(2, '0')
const aISO = (anio, mes, dia) => `${anio}-${pad(mes)}-${pad(dia)}`

/** Primer día del mes ('YYYY-MM') desplazado `delta` meses. */
function moverMes(mes, delta) {
  const [a, m] = mes.split('-').map(Number)
  const d = new Date(a, m - 1 + delta, 1)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

/**
 * `mes`: 'YYYY-MM'. `conteos`: { 'YYYY-MM-DD': número de citas }. `seleccionado`: día elegido o ''.
 * `onSeleccionar(fecha)`: clic en un día (vuelve a llamar con el mismo día para deseleccionar).
 */
export default function CalendarioMes({ mes, onCambiarMes, conteos, hoy, seleccionado, onSeleccionar }) {
  const [anio, numMes] = mes.split('-').map(Number)
  const diasEnMes = new Date(anio, numMes, 0).getDate()
  // getDay(): 0 = domingo; la semana empieza en lunes
  const desfase = (new Date(anio, numMes - 1, 1).getDay() + 6) % 7
  const celdas = [...Array(desfase).fill(null), ...Array.from({ length: diasEnMes }, (_, i) => aISO(anio, numMes, i + 1))]
  while (celdas.length % 7) celdas.push(null)

  const nombreMes = new Date(anio, numMes - 1, 1).toLocaleDateString('es-GT', { month: 'long', year: 'numeric' })
  const titulo = nombreMes.charAt(0).toUpperCase() + nombreMes.slice(1)
  const totalMes = Object.entries(conteos).reduce((s, [f, n]) => (f.startsWith(mes) ? s + n : s), 0)

  return (
    <div className="calendario">
      <div className="calendario-encabezado">
        <button type="button" className="btn btn-sm btn-outline-primary btn-round mb-0" onClick={() => onCambiarMes(moverMes(mes, -1))} aria-label="Mes anterior">
          &lsaquo;
        </button>
        <div className="text-center">
          <div className="calendario-titulo">{titulo}</div>
          <div className="small text-muted">
            {totalMes} {totalMes === 1 ? 'cita pendiente' : 'citas pendientes'}
          </div>
        </div>
        <button type="button" className="btn btn-sm btn-outline-primary btn-round mb-0" onClick={() => onCambiarMes(moverMes(mes, 1))} aria-label="Mes siguiente">
          &rsaquo;
        </button>
      </div>

      <div className="calendario-grilla" role="grid" aria-label={`Citas pendientes de ${titulo}`}>
        {DIAS_SEMANA.map((d) => (
          <div key={d} className="calendario-dia-semana" role="columnheader">
            {d}
          </div>
        ))}
        {celdas.map((fecha, i) => {
          if (!fecha) return <div key={`vacio-${i}`} className="calendario-celda calendario-celda-vacia" aria-hidden="true" />
          const n = conteos[fecha] ?? 0
          const clases = [
            'calendario-celda',
            n ? 'calendario-con-citas' : '',
            n && fecha < hoy ? 'calendario-vencida' : '',
            fecha === hoy ? 'calendario-hoy' : '',
            fecha === seleccionado ? 'calendario-seleccionada' : '',
          ]
            .filter(Boolean)
            .join(' ')
          return (
            <button
              key={fecha}
              type="button"
              role="gridcell"
              className={clases}
              disabled={!n}
              aria-pressed={fecha === seleccionado}
              aria-label={`${Number(fecha.slice(8))}: ${n ? `${n} ${n === 1 ? 'cita' : 'citas'}` : 'sin citas'}`}
              onClick={() => onSeleccionar(fecha === seleccionado ? '' : fecha)}
            >
              <span className="calendario-numero">{Number(fecha.slice(8))}</span>
              {n > 0 && <span className="calendario-conteo">{n}</span>}
            </button>
          )
        })}
      </div>

      <div className="calendario-leyenda small text-muted">
        <span><i className="calendario-punto calendario-punto-citas" /> Con citas</span>
        <span><i className="calendario-punto calendario-punto-vencida" /> Vencidas</span>
        <span><i className="calendario-punto calendario-punto-hoy" /> Hoy</span>
      </div>
    </div>
  )
}
