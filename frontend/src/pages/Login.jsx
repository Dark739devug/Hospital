import { useState } from 'react'
import { Button } from 'reactstrap'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import { normalizarError } from '../utils/errores.js'

export default function Login() {
  const { usuario, cargando, login, sesionExpirada } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [verPassword, setVerPassword] = useState(false)
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (!cargando && usuario) return <Navigate to={location.state?.desde ?? '/'} replace />

  const enviar = async (e) => {
    e.preventDefault()
    const nuevos = {}
    if (!username.trim()) nuevos.username = 'El usuario es obligatorio'
    if (!password) nuevos.password = 'La contraseña es obligatoria'
    setErrores(nuevos)
    setErrorGeneral('')
    if (Object.keys(nuevos).length) return

    setEnviando(true)
    try {
      await login(username.trim(), password)
      navigate(location.state?.desde ?? '/', { replace: true })
    } catch (error) {
      const { status, message, detalles } = normalizarError(error)
      setErrores(detalles)
      // 401 = credenciales incorrectas o usuario inactivo.
      setErrorGeneral(status === 401 ? 'Usuario o contraseña incorrectos.' : message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="login-form" onSubmit={enviar} noValidate>
      <h1 className="login-titulo">Iniciar sesión</h1>
      <p className="login-subtitulo">Ingrese sus credenciales para continuar.</p>

      {sesionExpirada && !errorGeneral && (
        <ErrorAlert tipo="aviso" mensaje="Su sesión expiró. Inicie sesión nuevamente." />
      )}
      <ErrorAlert mensaje={errorGeneral} />

      <FormField
        className="col-12 px-0"
        nombre="username"
        etiqueta="Usuario"
        valor={username}
        onChange={(e) => setUsername(e.target.value)}
        error={errores.username}
        autoComplete="username"
        autoFocus
      />
      <FormField
        className="col-12 px-0 login-campo-clave"
        nombre="password"
        etiqueta="Contraseña"
        tipo={verPassword ? 'text' : 'password'}
        valor={password}
        onChange={(e) => setPassword(e.target.value)}
        error={errores.password}
        autoComplete="current-password"
        adorno={
          <button
            type="button"
            className="login-ver-clave"
            onClick={() => setVerPassword((v) => !v)}
            aria-label={verPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={verPassword}
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z" />
              <circle cx="12" cy="12" r="3" />
              {verPassword && <path d="M4 4l16 16" />}
            </svg>
          </button>
        }
      />

      <Button type="submit" color="primary" className="login-boton" block disabled={enviando}>
        {enviando ? 'Ingresando...' : 'Ingresar'}
      </Button>
    </form>
  )
}
