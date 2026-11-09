/**
 * Convierte cualquier error de axios en { status, message, detalles }.
 * - detalles: mapa campo -> mensaje (solo en 400 de validación).
 * - El backend responde { timestamp, status, error, message, path, detalles }.
 *   El login fallido también usa este formato (401 con `message`).
 */
export function normalizarError(error) {
  if (!error?.response) {
    return {
      status: 0,
      message: 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.',
      detalles: {},
    }
  }
  const { status, data } = error.response
  const cuerpo = data && typeof data === 'object' ? data : {}
  let message = cuerpo.message
  if (!message) {
    if (status === 401) message = 'Su sesión no es válida o ha expirado.'
    else if (status === 403) message = 'No tiene permisos para realizar esta acción.'
    else if (status === 404) message = 'El recurso solicitado no existe.'
    else if (status >= 500) message = 'Ocurrió un error interno en el servidor.'
    else message = 'No se pudo completar la solicitud.'
  }
  return { status, message, detalles: cuerpo.detalles ?? {} }
}
