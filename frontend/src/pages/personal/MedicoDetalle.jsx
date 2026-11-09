import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  asignarEspecialidad,
  cambiarEstadoMedico,
  listarEspecialidadesDeMedico,
  obtenerMedico,
  quitarEspecialidad,
} from '../../api/administracion.js'
import client from '../../api/client.js'
import { RUTAS } from '../../api/rutas.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import Dato from '../../components/Dato.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import CredencialesGeneradas from '../../components/CredencialesGeneradas.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { normalizarError } from '../../utils/errores.js'
import { nombreMedico } from '../citas/utilsCita.js'

export default function MedicoDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { rol } = useAuth()
  const puedeEditar = puede(rol, 'personal', 'editar')
  const puedeEstado = puede(rol, 'personal', 'desactivar')

  const [exito, setExito] = useState(location.state?.mensaje ?? '')
  // Solo llegan justo después de registrar al médico; se guardan antes de limpiar location.state.
  const [credenciales, setCredenciales] = useState(location.state?.credenciales ?? null)
  const [errorAccion, setErrorAccion] = useState('')
  const [nueva, setNueva] = useState('')
  const [asignando, setAsignando] = useState(false)
  const [confirmacion, setConfirmacion] = useState(null) // { tipo: 'estado' | 'quitar', dato }
  const [procesando, setProcesando] = useState(false)
  const [errorConfirmacion, setErrorConfirmacion] = useState('')

  const { datos, error, cargando, recargar } = useApi(`medico|${id}`, async () => {
    const [medico, asignadas, especialidades] = await Promise.all([
      obtenerMedico(id),
      listarEspecialidadesDeMedico(id).catch(() => []),
      client.get(RUTAS.especialidadesMedicas).then((r) => r.data).catch(() => []),
    ])
    return { medico, asignadas, especialidades, porId: mapaPor(especialidades, 'idEspecialidad') }
  })

  useEffect(() => {
    if (location.state?.mensaje) navigate(location.pathname, { replace: true, state: null })
  }, [location.state, location.pathname, navigate])

  if (cargando) return <Loading texto="Cargando médico..." />
  if (error) {
    return (
      <div>
        <ErrorAlert titulo={error.status === 404 ? 'Médico no encontrado' : 'No se pudo cargar el médico'} mensaje={error.message} />
        <Link to="/personal" className="volver-enlace">
          &larr; Volver a personal
        </Link>
      </div>
    )
  }

  const { medico: m, asignadas, especialidades, porId } = datos
  const activo = m.estado !== false
  const disponibles = especialidades.filter((e) => e.activo !== false && !asignadas.some((a) => a.idEspecialidad === e.idEspecialidad))

  const asignar = async () => {
    setAsignando(true)
    setErrorAccion('')
    try {
      await asignarEspecialidad(Number(nueva), Number(id))
      setNueva('')
      setExito('Especialidad asignada.')
      recargar()
    } catch (e) {
      // 400: médico o especialidad inexistente/inactiva, o ya asignada
      setErrorAccion(normalizarError(e).message)
    } finally {
      setAsignando(false)
    }
  }

  const confirmar = async () => {
    setProcesando(true)
    setErrorConfirmacion('')
    try {
      if (confirmacion.tipo === 'estado') {
        await cambiarEstadoMedico(id, !activo)
        setExito(activo ? 'Médico desactivado.' : 'Médico activado.')
      } else {
        await quitarEspecialidad(confirmacion.dato.idEspecialidad, Number(id))
        setExito('Especialidad quitada.')
      }
      setConfirmacion(null)
      recargar()
    } catch (e) {
      // 422: no se desactiva con citas, hospitalizaciones o emergencias activas; 404: asignación inexistente
      setErrorConfirmacion(normalizarError(e).message)
    } finally {
      setProcesando(false)
    }
  }

  return (
    <div>
      <Link to="/personal" className="volver-enlace">
        &larr; Volver a personal
      </Link>
      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <CredencialesGeneradas credenciales={credenciales} onCerrar={() => setCredenciales(null)} />

      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-1">{nombreMedico(m)}</h1>
          <BadgeActivo activo={m.estado} />
        </div>
        <div className="acciones-grupo d-flex flex-wrap mt-3 mt-md-0">
          {puedeEditar && (
            <Link to={`/personal/medicos/${m.idMedico}/editar`} className="btn btn-secondary btn-round">
              Editar
            </Link>
          )}
          {puedeEstado && (
            <button
              type="button"
              className={`btn btn-round ${activo ? 'btn-danger' : 'btn-success'}`}
              onClick={() => {
                setErrorConfirmacion('')
                setConfirmacion({ tipo: 'estado' })
              }}
            >
              {activo ? 'Desactivar' : 'Activar'}
            </button>
          )}
        </div>
      </div>

      <div className="card card-body">
        <div className="row">
          <Dato etiqueta="N.º de colegiado">{m.noColegiado}</Dato>
          <Dato etiqueta="Teléfono">{m.telefono}</Dato>
          <Dato etiqueta="Correo electrónico">{m.email}</Dato>
        </div>
      </div>

      <div className="card card-body">
        <h2 className="h5 mb-3">Especialidades</h2>
        <ErrorAlert mensaje={errorAccion} onCerrar={() => setErrorAccion('')} />
        {asignadas.length === 0 ? (
          <p className="text-muted">Este médico no tiene especialidades asignadas.</p>
        ) : (
          <ul className="lista-divisoria mb-3">
            {asignadas.map((a) => (
              <li key={a.idEspecialidad} className="d-flex justify-content-between align-items-center">
                <span>
                  <strong>{porId[a.idEspecialidad]?.nombre ?? `Especialidad #${a.idEspecialidad}`}</strong>
                  {porId[a.idEspecialidad]?.descripcion && <span className="text-muted small d-block">{porId[a.idEspecialidad].descripcion}</span>}
                </span>
                {puedeEditar && (
                  <button
                    type="button"
                    className="enlace-boton text-danger"
                    onClick={() => {
                      setErrorConfirmacion('')
                      setConfirmacion({ tipo: 'quitar', dato: a })
                    }}
                  >
                    Quitar
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
        {puedeEditar && (
          <div className="border-top pt-3 d-flex flex-wrap align-items-end acciones-grupo">
            <div className="flex-grow-1">
              <label htmlFor="nueva-especialidad" className="d-block font-weight-bold small mb-1">
                Asignar una especialidad
              </label>
              <select id="nueva-especialidad" className="form-control" value={nueva} onChange={(e) => setNueva(e.target.value)}>
                <option value="">Seleccione...</option>
                {disponibles.map((e) => (
                  <option key={e.idEspecialidad} value={e.idEspecialidad}>
                    {e.nombre}
                  </option>
                ))}
              </select>
            </div>
            <button type="button" className="btn btn-primary btn-round" disabled={!nueva || asignando} onClick={asignar}>
              {asignando ? 'Asignando...' : 'Asignar'}
            </button>
          </div>
        )}
      </div>

      <ConfirmDialog
        abierto={Boolean(confirmacion)}
        titulo={confirmacion?.tipo === 'quitar' ? 'Quitar especialidad' : activo ? 'Desactivar médico' : 'Activar médico'}
        mensaje={
          confirmacion?.tipo === 'quitar'
            ? '¿Quitar esta especialidad al médico?'
            : activo
              ? 'El médico deja de ofrecerse en citas, hospitalizaciones y emergencias. No se permite si tiene citas, hospitalizaciones o emergencias activas.'
              : '¿Activar nuevamente a este médico?'
        }
        textoConfirmar={confirmacion?.tipo === 'quitar' ? 'Quitar' : activo ? 'Desactivar' : 'Activar'}
        peligro={confirmacion?.tipo === 'quitar' || activo}
        cargando={procesando}
        error={errorConfirmacion}
        onConfirmar={confirmar}
        onCancelar={() => setConfirmacion(null)}
      />
    </div>
  )
}
