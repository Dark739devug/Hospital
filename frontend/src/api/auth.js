import client from './client.js'
import { RUTAS } from './rutas.js'

/** POST /api/auth/login -> { token, type, debeCambiarPassword, fechaExpiracionPassword } */
export const iniciarSesion = (username, password) =>
  client.post(RUTAS.auth.login, { username, password }).then((r) => r.data)

/** GET /api/auth/me -> { username, roles: ['ROLE_ADMIN'], debeCambiarPassword, fechaExpiracionPassword } */
export const obtenerUsuarioActual = () => client.get(RUTAS.auth.me).then((r) => r.data)

/** POST /api/auth/cambiar-password. 422 si la actual es incorrecta o la nueva es igual. */
export const cambiarPassword = (passwordActual, nuevaPassword) =>
  client.post(RUTAS.auth.cambiarPassword, { passwordActual, nuevaPassword }).then((r) => r.data)
