import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Button } from 'reactstrap'
import { desactivarMedicamento, obtenerMedicamento } from '../../api/medicamentos.js'
import { listarRecetas } from '../../api/recetas.js'
import { listarTratamientos } from '../../api/tratamientos.js'
import { useAuth } from '../../auth/useAuth.js'
import AjusteStockDialog from '../../components/AjusteStockDialog.jsx'
import Badge, { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Loading from '../../components/Loading.jsx'
import StockBadge from '../../components/StockBadge.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { normalizarError } from '../../utils/errores.js'
import { acortar, formatearMoneda } from '../../utils/formato.js'

export default function MedicamentoDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'medicamentos', 'editar')
  const puedeDesactivar = puede(rol, 'medicamentos', 'desactivar')
  const puedeAjustar = puede(rol, 'medicamentos', 'ajustarStock')
  const puedeVerRecetas = puede(rol, 'recetas', 'ver')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  const [ajustando, setAjustando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [desactivando, setDesactivando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  const { datos: medicamento, error, cargando, recargar } = useApi(`medicamento|${id}`, () => obtenerMedicamento(id))
  // Tratamientos que usan el medicamento (la lectura de recetas está limitada a ADMIN, MEDICO y FARMACEUTICO)
  const { datos: usos } = useApi(`medicamento-usos|${id}|${puedeVerRecetas}`, async () => {
    if (!puedeVerRecetas) return null
    const [recetas, tratamientos] = await Promise.all([listarRecetas({ idMedicamento: id }), listarTratamientos().catch(() => [])])
    const porId = mapaPor(tratamientos, 'idTratamiento')
    return recetas.map((r) => ({ ...r, tratamiento: porId[r.idTratamiento] }))
  })

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  const desactivar = async () => {
    setDesactivando(true)
    setErrorDesactivar('')
    try {
      await desactivarMedicamento(id)
      navigate('/medicamentos', { replace: true, state: { mensaje: 'El medicamento fue desactivado.' } })
    } catch (e) {
      // 409: está asociado a uno o más tratamientos; 404: ya no existe; 422: ya inactivo
      setErrorDesactivar(normalizarError(e).message)
      setDesactivando(false)
    }
  }

  if (cargando) return <Loading texto="Cargando medicamento..." />

  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Medicamento no encontrado' : 'No se pudo cargar el medicamento'} mensaje={error.message} />
        <Link to="/medicamentos" className="volver-enlace">
          &larr; Volver a medicamentos
        </Link>
      </div>
    )
  }

  const m = medicamento
  const activo = m.activo !== false

  return (
    <div>
      <Link to="/medicamentos" className="volver-enlace">
        &larr; Volver a medicamentos
      </Link>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-1">{m.nombreComercial}</h1>
          <div className="d-flex align-items-center acciones-grupo">
            <BadgeActivo activo={m.activo} />
            <StockBadge medicamento={m} />
            {m.requiereReceta && <Badge tono="azul">Requiere receta</Badge>}
          </div>
        </div>
        <div className="acciones-grupo d-flex flex-wrap mt-3 mt-md-0">
          {activo && puedeAjustar && (
            <Button color="primary" className="btn-round" onClick={() => setAjustando(true)}>
              Ajustar stock
            </Button>
          )}
          {activo && puedeEditar && (
            <Link to={`/medicamentos/${m.idMedicamento}/editar`} className="btn btn-secondary btn-round">
              Editar
            </Link>
          )}
          {activo && puedeDesactivar && (
            <button
              type="button"
              onClick={() => {
                setErrorDesactivar('')
                setConfirmando(true)
              }}
              className="btn btn-danger btn-round"
            >
              Desactivar
            </button>
          )}
        </div>
      </div>

      <div className="card card-body">
        <div className="row">
          <Dato etiqueta="Nombre genérico">{m.nombreGenerico}</Dato>
          <Dato etiqueta="Presentación">{m.presentacion}</Dato>
          <Dato etiqueta="Laboratorio">{m.laboratorio}</Dato>
          <Dato etiqueta="Stock actual">{String(m.stock)}</Dato>
          <Dato etiqueta="Stock mínimo">{String(m.stockMinimo)}</Dato>
          <Dato etiqueta="Precio">{formatearMoneda(m.precio)}</Dato>
        </div>
      </div>

      {puedeVerRecetas && (
        <div className="card card-body">
          <h2 className="h5 mb-3">Tratamientos que lo incluyen</h2>
          {usos === null ? (
            <p className="text-muted small mb-0">Cargando...</p>
          ) : usos.length === 0 ? (
            <p className="text-muted mb-0">Este medicamento no está asignado a ningún tratamiento.</p>
          ) : (
            <ul className="lista-divisoria">
              {usos.map((u) => (
                <li key={u.idTratamiento}>
                  <Link to={`/tratamientos/${u.idTratamiento}`} className="font-weight-bold">
                    {u.tratamiento ? acortar(u.tratamiento.descripcion, 80) : `Tratamiento #${u.idTratamiento}`}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {ajustando && (
        <AjusteStockDialog
          medicamento={m}
          onCerrar={() => setAjustando(false)}
          onListo={(cambio) => {
            setAjustando(false)
            setExito(`Stock actualizado (${cambio > 0 ? '+' : ''}${cambio}).`)
            recargar()
          }}
        />
      )}

      <ConfirmDialog
        abierto={confirmando}
        titulo="Desactivar medicamento"
        mensaje="¿Desea desactivar este medicamento? No se permite si está asignado a algún tratamiento."
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
