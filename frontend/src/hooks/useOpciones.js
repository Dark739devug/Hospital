import client from '../api/client.js'
import { useApi } from './useApi.js'

/** Opciones de los campos `select` de un catálogo (config/catalogos.js); las remotas se piden al backend. */
export function useOpciones(config) {
  const remotos = config.campos.filter((c) => c.opciones && !Array.isArray(c.opciones))
  const clave = `opciones|${config.clave}`
  const { datos } = useApi(clave, async () => {
    const pares = await Promise.all(
      remotos.map(async (c) => {
        const { ruta, valor, etiqueta, activo } = c.opciones
        const filas = await client.get(ruta).then((r) => r.data).catch(() => [])
        return [c.nombre, filas.map((f) => ({ valor: String(f[valor]), etiqueta: f[etiqueta], activo: f[activo] !== false }))]
      }),
    )
    return Object.fromEntries(pares)
  })
  return Object.fromEntries(
    config.campos.filter((c) => c.opciones).map((c) => [c.nombre, Array.isArray(c.opciones) ? c.opciones : (datos?.[c.nombre] ?? [])]),
  )
}
