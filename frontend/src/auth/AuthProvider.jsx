import { useCallback, useEffect, useMemo, useState } from 'react'
import { iniciarSesion, obtenerUsuarioActual } from '../api/auth.js'
import {
  borrarToken,
  guardarToken,
  obtenerToken,
  registrarManejadorNoAutorizado,
} from '../api/sesion.js'
import { AuthContext } from './AuthContext.js'

const quitarPrefijo = (rol) => String(rol).replace(/^ROLE_/, '')

function construirUsuario(datos) {
  const roles = (datos.roles ?? []).map(quitarPrefijo)
  return {
    username: datos.username,
    roles,
    rol: roles[0] ?? null,
    // Contraseña temporal: ProtectedRoute obliga a cambiarla antes de usar el sistema.
    debeCambiarPassword: Boolean(datos.debeCambiarPassword),
    fechaExpiracionPassword: datos.fechaExpiracionPassword ?? null,
  }
}

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null)
  // Si hay token guardado, hay que consultar /me antes de decidir si hay sesión.
  const [cargando, setCargando] = useState(() => Boolean(obtenerToken()))
  const [sesionExpirada, setSesionExpirada] = useState(false)
  // true cuando el usuario cerró sesión con el botón: ProtectedRoute no recuerda la página
  // actual, así el siguiente usuario entra a Inicio y no a la última pantalla del anterior.
  const [cerroSesion, setCerroSesion] = useState(false)

  // Ante cualquier 401 (fuera del login) el cliente HTTP avisa aquí.
  useEffect(() => {
    registrarManejadorNoAutorizado(() => {
      setUsuario((actual) => {
        if (actual) setSesionExpirada(true)
        return null
      })
      setCargando(false)
    })
    return () => registrarManejadorNoAutorizado(null)
  }, [])

  // Recuperar la sesión al recargar la página.
  useEffect(() => {
    if (!obtenerToken()) return
    let cancelado = false
    obtenerUsuarioActual()
      .then((datos) => {
        if (!cancelado) setUsuario(construirUsuario(datos))
      })
      .catch(() => {
        borrarToken()
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [])

  const login = useCallback(async (username, password) => {
    const { token } = await iniciarSesion(username, password)
    guardarToken(token)
    try {
      const datos = await obtenerUsuarioActual()
      setSesionExpirada(false)
      setCerroSesion(false)
      setUsuario(construirUsuario(datos))
    } catch (error) {
      borrarToken()
      throw error
    }
  }, [])

  const logout = useCallback(() => {
    borrarToken()
    setSesionExpirada(false)
    setCerroSesion(true)
    setUsuario(null)
  }, [])

  const passwordCambiada = useCallback(() => {
    setUsuario((actual) => (actual ? { ...actual, debeCambiarPassword: false, fechaExpiracionPassword: null } : actual))
  }, [])

  const tieneRol = useCallback((roles) => !!usuario && roles.includes(usuario.rol), [usuario])

  const valor = useMemo(
    () => ({
      usuario,
      rol: usuario?.rol ?? null,
      cargando,
      sesionExpirada,
      cerroSesion,
      login,
      logout,
      passwordCambiada,
      tieneRol,
    }),
    [usuario, cargando, sesionExpirada, cerroSesion, login, logout, passwordCambiada, tieneRol],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
