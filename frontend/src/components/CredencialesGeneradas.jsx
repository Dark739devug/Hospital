import { useState } from 'react'
import { Alert } from 'reactstrap'
import { formatearFechaHora } from '../utils/formato.js'

/** Usuario y contraseña temporal del personal recién registrado. Solo llegan en la respuesta de creación. */
export default function CredencialesGeneradas({ credenciales, onCerrar }) {
  const [copiado, setCopiado] = useState(false)
  if (!credenciales) return null
  const { username, passwordTemporal, fechaExpiracion } = credenciales

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(`Usuario: ${username}\nContraseña: ${passwordTemporal}`)
      setCopiado(true)
    } catch {
      setCopiado(false)
    }
  }

  return (
    <Alert color="info" fade={false} toggle={onCerrar} closeAriaLabel="Cerrar" className="mb-3">
      <strong className="d-block">Acceso al sistema</strong>
      Usuario: <strong>{username}</strong> · Contraseña temporal: <strong>{passwordTemporal}</strong>
      <br />
      Vence el {formatearFechaHora(fechaExpiracion)}. Anótela ahora, no se vuelve a mostrar.{' '}
      <button type="button" className="btn btn-link text-white p-0 font-weight-bold" onClick={copiar}>
        {copiado ? 'Copiado' : 'Copiar'}
      </button>
    </Alert>
  )
}
