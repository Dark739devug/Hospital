import { Navigate, Outlet, useLocation } from 'react-router-dom'
import Loading from '../components/Loading.jsx'
import { useAuth } from './useAuth.js'

export const RUTA_CAMBIAR_PASSWORD = '/cambiar-password'

/** Exige sesión iniciada; si no, redirige a /login. Con contraseña temporal solo deja entrar a cambiarla. */
export default function ProtectedRoute() {
  const { usuario, cargando, cerroSesion } = useAuth()
  const location = useLocation()

  if (cargando) return <Loading pantallaCompleta texto="Verificando sesión..." />
  if (!usuario) {
    return <Navigate to="/login" replace state={cerroSesion ? null : { desde: location.pathname }} />
  }
  if (usuario.debeCambiarPassword && location.pathname !== RUTA_CAMBIAR_PASSWORD) {
    return <Navigate to={RUTA_CAMBIAR_PASSWORD} replace />
  }
  return <Outlet />
}
