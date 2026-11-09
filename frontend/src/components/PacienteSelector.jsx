import { useEffect, useId, useState } from 'react'
import { buscarPacientesResumen, resumenPaciente } from '../api/pacientes.js'
import { useApi } from '../hooks/useApi.js'

const MINIMO = 2

/**
 * Selector de paciente con búsqueda (combobox accesible) contra GET /api/paciente/busqueda, que devuelve solo
 * id, nombre completo y DPI de pacientes activos (lo pueden usar ADMIN, CAJERO y RECEPCIONISTA).
 * - Espera ~300 ms tras escribir y pide el servidor con al menos 2 caracteres.
 * - Teclado: flechas para moverse, Enter para elegir, Escape para cerrar.
 * - `valor` es el idPaciente (texto) y `onChange(idPaciente, resumen)` recibe '' al quitar la selección.
 *   El nombre del paciente ya elegido (p. ej. al editar) se precarga con GET /api/paciente/{id}/resumen.
 * - `compacto` dibuja solo el control (sin etiqueta) para usarlo como filtro en las listas.
 */
export default function PacienteSelector({
  valor,
  onChange,
  nombre = 'idPaciente',
  etiqueta = 'Paciente',
  error,
  ayuda,
  requerido = false,
  disabled = false,
  compacto = false,
  placeholder = 'Buscar por nombre, apellido o DPI',
  className = '',
}) {
  const id = useId()
  const idLista = `${id}-lista`
  const [texto, setTexto] = useState('')
  const [consulta, setConsulta] = useState('')
  const [resultados, setResultados] = useState({ q: '', lista: [], fallo: false })
  const [escribiendo, setEscribiendo] = useState(false)
  const [activo, setActivo] = useState(-1)

  // Nombre del paciente seleccionado (con caché de resúmenes); si el rol no puede leerlo queda "Paciente #id"
  const { datos: seleccionado } = useApi(`paciente-seleccionado|${valor ?? ''}`, () => (valor ? resumenPaciente(valor) : null))

  // Espera de 300 ms antes de consultar
  useEffect(() => {
    const t = setTimeout(() => setConsulta(texto.trim()), 300)
    return () => clearTimeout(t)
  }, [texto])

  useEffect(() => {
    if (consulta.length < MINIMO) return undefined
    let cancelado = false
    buscarPacientesResumen(consulta)
      .then((lista) => {
        if (!cancelado) setResultados({ q: consulta, lista, fallo: false })
      })
      .catch(() => {
        if (!cancelado) setResultados({ q: consulta, lista: [], fallo: true })
      })
    return () => {
      cancelado = true
    }
  }, [consulta])

  const consultaValida = consulta.length >= MINIMO && texto.trim() === consulta
  const hayRespuesta = consultaValida && resultados.q === consulta
  const lista = hayRespuesta ? resultados.lista : []
  const abierto = escribiendo && texto.trim().length > 0

  const elegir = (r) => {
    setTexto('')
    setConsulta('')
    setEscribiendo(false)
    setActivo(-1)
    onChange(String(r.idPaciente), r)
  }

  const quitar = () => {
    setTexto('')
    setConsulta('')
    setEscribiendo(false)
    onChange('', null)
  }

  const alTeclear = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setEscribiendo(true)
      setActivo((i) => Math.min(i + 1, lista.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter') {
      if (abierto && lista[activo]) {
        e.preventDefault()
        elegir(lista[activo])
      } else if (abierto) {
        e.preventDefault()
      }
    } else if (e.key === 'Escape') {
      setEscribiendo(false)
      setActivo(-1)
    }
  }

  const nombreSeleccionado = valor ? (seleccionado?.nombreCompleto ?? `Paciente #${valor}`) : ''
  const mostrarSeleccion = Boolean(valor) && !escribiendo

  let mensaje = null
  if (abierto && lista.length === 0) {
    if (texto.trim().length < MINIMO) mensaje = `Escriba al menos ${MINIMO} caracteres`
    else if (!hayRespuesta) mensaje = 'Buscando...'
    else if (resultados.fallo) mensaje = 'No se pudo realizar la búsqueda'
    else mensaje = 'Sin resultados'
  }

  const control = (
    <div className="paciente-selector">
      <div className="input-group">
        <input
          id={id}
          name={nombre}
          type="text"
          role="combobox"
          aria-autocomplete="list"
          aria-expanded={abierto}
          aria-controls={idLista}
          aria-activedescendant={abierto && activo >= 0 ? `${id}-op-${activo}` : undefined}
          aria-invalid={error ? true : undefined}
          aria-label={compacto ? etiqueta : undefined}
          autoComplete="off"
          className={`form-control${error ? ' is-invalid' : ''}`}
          placeholder={compacto ? 'Todos los pacientes' : placeholder}
          disabled={disabled}
          value={mostrarSeleccion ? nombreSeleccionado : texto}
          onFocus={() => {
            if (!valor) setEscribiendo(true)
          }}
          onChange={(e) => {
            setTexto(e.target.value)
            setEscribiendo(true)
            setActivo(0)
          }}
          onBlur={() => setEscribiendo(false)}
          onKeyDown={alTeclear}
        />
        {valor && !disabled && (
          <div className="input-group-append">
            <button type="button" className="btn btn-outline-secondary btn-sm" onClick={quitar} aria-label="Quitar paciente seleccionado">
              &times;
            </button>
          </div>
        )}
      </div>
      {abierto && (
        <ul id={idLista} role="listbox" className="list-group paciente-selector-lista" aria-label="Resultados de pacientes">
          {mensaje && (
            <li className="list-group-item text-muted small" role="presentation">
              {mensaje}
            </li>
          )}
          {lista.map((r, i) => (
            <li
              key={r.idPaciente}
              id={`${id}-op-${i}`}
              role="option"
              aria-selected={i === activo}
              className={`list-group-item list-group-item-action${i === activo ? ' active' : ''}`}
              // mousedown (y no click) para elegir antes de que el campo pierda el foco
              onMouseDown={(e) => {
                e.preventDefault()
                elegir(r)
              }}
              onMouseEnter={() => setActivo(i)}
            >
              <strong>{r.nombreCompleto}</strong>
              <span className="d-block small">DPI {r.dpiPaciente}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )

  if (compacto) return <div className={`paciente-selector-compacto ${className}`.trim()}>{control}</div>

  return (
    <div className={`form-group ${/(^|\s)col-/.test(className) ? '' : 'col-md-6'} ${className}`.trim()}>
      <label htmlFor={id} className="font-weight-bold small mb-1">
        {etiqueta}
        {requerido && (
          <span className="text-danger ml-1" aria-hidden="true">
            *
          </span>
        )}
      </label>
      {control}
      {ayuda && !error && <small className="form-text text-muted">{ayuda}</small>}
      {error && <div className="invalid-feedback d-block">{error}</div>}
    </div>
  )
}
