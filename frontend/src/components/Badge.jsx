// Etiquetas de color de Now UI Dashboard / Bootstrap 4 (clases badge-*).
const TONOS = {
  gris: 'badge-secondary',
  verde: 'badge-success',
  azul: 'badge-info',
  ambar: 'badge-warning',
  rojo: 'badge-danger',
  teal: 'badge-primary',
  naranja: 'badge-naranja',
  amarillo: 'badge-amarillo',
}

export default function Badge({ children, tono = 'gris' }) {
  return <span className={`badge ${TONOS[tono] ?? TONOS.gris}`}>{children}</span>
}

/** Badge Activo/Inactivo para el campo `activo`. */
export function BadgeActivo({ activo }) {
  return activo === false ? <Badge>Inactivo</Badge> : <Badge tono="verde">Activo</Badge>
}

