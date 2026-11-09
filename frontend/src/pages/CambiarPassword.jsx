import { useState } from 'react'
import { Button } from 'reactstrap'
import { Link, useNavigate } from 'react-router-dom'
import { cambiarPassword } from '../api/auth.js'
import { useAuth } from '../auth/useAuth.js'
import ErrorAlert from '../components/ErrorAlert.jsx'
import FormField from '../components/FormField.jsx'
import { normalizarError } from '../utils/errores.js'
import { formatearFechaHora } from '../utils/formato.js'

const LARGO_MINIMO = 8

/**
 * Cambio de la contraseña propia. Si la contraseña es temporal (personal recién registrado o acceso restablecido por un
 * administrador), ProtectedRoute trae aquí al usuario y no lo deja usar el sistema hasta cambiarla.
 */
export default function CambiarPassword() {
  const { usuario, logout, passwordCambiada } = useAuth()
  const navigate = useNavigate()
  const obligatorio = Boolean(usuario?.debeCambiarPassword)
  const [form, setForm] = useState({ actual: '', nueva: '', confirmar: '' })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [enviando, setEnviando] = useState(false)

  const cambiar = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    const nuevos = {}
    if (!form.actual) nuevos.actual = 'Ingrese su contraseña actual'
    if (form.nueva.length < LARGO_MINIMO) nuevos.nueva = `Debe tener al menos ${LARGO_MINIMO} caracteres`
    else if (form.nueva === form.actual) nuevos.nueva = 'Debe ser diferente a la contraseña actual'
    if (form.confirmar !== form.nueva) nuevos.confirmar = 'Las contraseñas no coinciden'
    setErrores(nuevos)
    setErrorGeneral('')
    if (Object.keys(nuevos).length) return

    setEnviando(true)
    try {
      await cambiarPassword(form.actual, form.nueva)
      passwordCambiada()
      navigate('/', { replace: true })
    } catch (error) {
      const { message, detalles } = normalizarError(error)
      // El backend nombra los campos passwordActual / nuevaPassword
      setErrores({ actual: detalles.passwordActual, nueva: detalles.nuevaPassword })
      setErrorGeneral(message)
      setEnviando(false)
    }
  }

  return (
    <form className="login-form" onSubmit={enviar} noValidate>
      <h1 className="login-titulo">Cambiar contraseña</h1>
      <p className="login-subtitulo">
        {obligatorio
          ? `Su contraseña es temporal${usuario.fechaExpiracionPassword ? ` y vence el ${formatearFechaHora(usuario.fechaExpiracionPassword)}` : ''}. Cree una nueva para continuar.`
          : 'Ingrese su contraseña actual y la nueva.'}
      </p>

      <ErrorAlert mensaje={errorGeneral} />

      <FormField
        className="col-12 px-0"
        nombre="actual"
        etiqueta={obligatorio ? 'Contraseña temporal' : 'Contraseña actual'}
        tipo="password"
        valor={form.actual}
        onChange={cambiar}
        error={errores.actual}
        autoComplete="current-password"
        autoFocus
      />
      <FormField
        className="col-12 px-0"
        nombre="nueva"
        etiqueta="Nueva contraseña"
        tipo="password"
        valor={form.nueva}
        onChange={cambiar}
        error={errores.nueva}
        autoComplete="new-password"
        ayuda={`Mínimo ${LARGO_MINIMO} caracteres.`}
      />
      <FormField
        className="col-12 px-0"
        nombre="confirmar"
        etiqueta="Confirmar nueva contraseña"
        tipo="password"
        valor={form.confirmar}
        onChange={cambiar}
        error={errores.confirmar}
        autoComplete="new-password"
      />

      <Button type="submit" color="primary" className="login-boton" block disabled={enviando}>
        {enviando ? 'Guardando...' : 'Cambiar contraseña'}
      </Button>

      <p className="text-center mt-3 mb-0 small">
        {obligatorio ? (
          <button type="button" className="btn btn-link p-0" onClick={logout}>
            Cerrar sesión
          </button>
        ) : (
          <Link to="/">Volver al inicio</Link>
        )}
      </p>
    </form>
  )
}
