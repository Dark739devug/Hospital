import { Link } from 'react-router-dom'
import { Card, CardBody } from 'reactstrap'

/** Indicador numérico con ícono (Inicio, Farmacia). `kpi` = { icono, alerta }; con `ruta` toda la tarjeta es un enlace. */
export default function TarjetaKpi({ kpi, valor, ruta, titulo }) {
  const cuerpo = (
    <Card className="tarjeta-kpi">
      <CardBody>
        <div className="d-flex align-items-center">
          <div className={`kpi-icono mr-3${kpi.alerta && Number(valor) > 0 ? ' kpi-alerta' : ''}`} aria-hidden="true">
            <i className={`now-ui-icons ${kpi.icono}`} />
          </div>
          <div className="tarjeta-modulo-texto">
            <div className="kpi-valor">{valor === null ? '—' : valor}</div>
            <div className="text-muted small">{titulo}</div>
          </div>
        </div>
      </CardBody>
    </Card>
  )
  return ruta ? (
    <Link to={ruta} className="tarjeta-modulo" aria-label={`${titulo}: ${valor ?? 'sin dato'}`}>
      {cuerpo}
    </Link>
  ) : (
    cuerpo
  )
}
