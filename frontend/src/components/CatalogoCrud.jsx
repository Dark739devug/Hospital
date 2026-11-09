import { useMemo, useState } from 'react'
import { Alert, Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap'
import { crud } from '../api/administracion.js'
import { useAuth } from '../auth/useAuth.js'
import { BadgeActivo } from './Badge.jsx'
import ConfirmDialog from './ConfirmDialog.jsx'
import CredencialesGeneradas from './CredencialesGeneradas.jsx'
import EmptyState from './EmptyState.jsx'
import ErrorAlert from './ErrorAlert.jsx'
import FormField from './FormField.jsx'
import Pagination from './Pagination.jsx'
import Table from './Table.jsx'
import { useApi } from '../hooks/useApi.js'
import { useOpciones } from '../hooks/useOpciones.js'
import { errorMonto } from '../utils/dinero.js'
import { normalizarError } from '../utils/errores.js'
import { acortar, formatearHora, formatearMoneda } from '../utils/formato.js'

const TAMANO_PAGINA = 10

const esActivo = (config, fila) => fila[config.activoCampo] !== false

/** Pide las opciones de los campos `select` que vienen de otro endpoint: { nombreCampo: [{ valor, etiqueta }] }. */

const valorInicial = (campo, fila) => {
  const v = fila?.[campo.nombre]
  if (v === null || v === undefined) return ''
  // LocalTime 'HH:mm:ss' -> valor de <input type="time">
  if (campo.tipo === 'hora') return String(v).slice(0, 5)
  return String(v)
}

function aPayload(campos, form) {
  const datos = {}
  for (const c of campos) {
    const v = (form[c.nombre] ?? '').toString()
    if (c.tipo === 'entero') datos[c.nombre] = v === '' ? null : Number(v)
    else if (c.tipo === 'decimal') datos[c.nombre] = v === '' ? null : Number(v.replace(',', '.'))
    else if (c.tipo === 'hora') datos[c.nombre] = v === '' ? null : `${v}:00`
    else if (c.tipo === 'select' && !c.valorTexto) datos[c.nombre] = v === '' ? null : Number(v)
    else datos[c.nombre] = v.trim() === '' ? null : v.trim()
  }
  return datos
}

function validar(campos, form) {
  const errores = {}
  for (const c of campos) {
    const v = (form[c.nombre] ?? '').toString()
    if (c.requerido && v.trim() === '') {
      errores[c.nombre] = `${c.etiqueta} es obligatorio.`
      continue
    }
    if (v === '') continue
    if (c.max && v.length > c.max) errores[c.nombre] = `${c.etiqueta} no puede superar los ${c.max} caracteres.`
    else if (c.tipo === 'entero' && (!/^\d+$/.test(v) || (c.min !== undefined && Number(v) < c.min))) errores[c.nombre] = `${c.etiqueta} debe ser un entero${c.min !== undefined ? ` mayor o igual a ${c.min}` : ''}.`
    else if (c.tipo === 'decimal') {
      const m = errorMonto(v, { etiqueta: c.etiqueta })
      if (m) errores[c.nombre] = m
    }
  }
  return errores
}

/** Diálogo de alta y edición de un registro de catálogo (también lo usa la vista de clínicas). */
export function DialogoCatalogo({ config, fila, opciones, protegida, onCerrar, onListo }) {
  const api = crud(config.ruta)
  const esEdicion = Boolean(fila)
  const [form, setForm] = useState(() => Object.fromEntries(config.campos.map((c) => [c.nombre, valorInicial(c, fila)])))
  const [activo, setActivo] = useState(fila ? esActivo(config, fila) : true)
  const [errores, setErrores] = useState({})
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const cambiar = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const guardar = async (ev) => {
    ev.preventDefault()
    const locales = validar(config.campos, form)
    setErrores(locales)
    if (Object.keys(locales).length) return
    setGuardando(true)
    setError('')
    try {
      const payload = aPayload(config.campos, form)
      if (esEdicion) await api.actualizar(fila[config.idCampo], { ...fila, ...payload, [config.activoCampo]: activo })
      // El backend crea el registro activo por defecto
      const creado = esEdicion ? null : await api.crear(payload)
      // Médicos y enfermeros: el alta devuelve las credenciales generadas (solo esta vez)
      onListo(esEdicion ? 'Cambios guardados.' : `Se creó el registro de ${config.singular}.`, creado?.credenciales ?? null)
    } catch (e) {
      // 400: `detalles` por campo (mismos nombres que el DTO); 409: duplicado o conflicto; 422: reglas de negocio (estados base, formato)
      const { message, detalles } = normalizarError(e)
      setErrores(detalles ?? {})
      setError(Object.keys(detalles ?? {}).length ? 'Revise los campos marcados.' : message)
      setGuardando(false)
    }
  }

  return (
    <Modal isOpen toggle={guardando ? undefined : onCerrar} centered fade={false}>
      <form onSubmit={guardar} noValidate>
        <ModalHeader closeAriaLabel="Cerrar" toggle={guardando ? undefined : onCerrar}>
          {esEdicion ? `Editar ${config.singular}` : `${config.femenino ? 'Nueva' : 'Nuevo'} ${config.singular}`}
        </ModalHeader>
        <ModalBody>
          <div className="row">
            {config.campos.map((c) => {
              const lista = (opciones[c.nombre] ?? []).filter((o) => o.activo !== false || String(o.valor) === form[c.nombre])
              // En un select de texto cuyo valor actual no está en la lista (dato antiguo) se conserva como opción
              const conActual = form[c.nombre] && !lista.some((o) => String(o.valor) === form[c.nombre]) ? [...lista, { valor: form[c.nombre], etiqueta: form[c.nombre] }] : lista
              const bloqueado = esEdicion && protegida && c.nombre === (config.campoClave ?? 'descripcion')
              return (
                <FormField
                  key={c.nombre}
                  className="col-12"
                  nombre={c.nombre}
                  etiqueta={c.etiqueta}
                  tipo={c.tipo === 'hora' ? 'time' : c.tipo === 'entero' ? 'number' : c.tipo === 'select' ? 'select' : c.tipo === 'textarea' ? 'textarea' : 'text'}
                  opciones={c.tipo === 'select' ? conActual.map((o) => ({ valor: String(o.valor), etiqueta: o.etiqueta })) : undefined}
                  placeholder={c.tipo === 'select' ? (c.requerido ? 'Seleccione...' : 'Sin definir') : undefined}
                  inputMode={c.tipo === 'decimal' ? 'decimal' : undefined}
                  min={c.tipo === 'entero' ? (c.min ?? 0) : undefined}
                  valor={form[c.nombre]}
                  onChange={cambiar}
                  error={errores[c.nombre]}
                  requerido={c.requerido}
                  disabled={bloqueado}
                  ayuda={bloqueado ? 'Estado base del sistema: no se puede renombrar.' : c.ayuda}
                  maxLength={c.tipo === 'texto' || !c.tipo || c.tipo === 'textarea' ? c.max : undefined}
                />
              )
            })}
            {esEdicion && (
              <div className="col-12">
                <label className="d-flex align-items-center small mb-0">
                  <input type="checkbox" className="mr-2" checked={activo} onChange={(e) => setActivo(e.target.checked)} disabled={protegida && config.protegidos} />
                  Activo
                </label>
              </div>
            )}
          </div>
          {error && (
            <Alert color="danger" fade={false} role="alert" className="mt-3 mb-0">
              {error}
            </Alert>
          )}
        </ModalBody>
        <ModalFooter>
          <Button type="button" color="secondary" outline onClick={onCerrar} disabled={guardando}>
            Cancelar
          </Button>
          <Button type="submit" color="primary" disabled={guardando}>
            {guardando ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Crear'}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  )
}

/**
 * CRUD genérico de un catálogo (lista con búsqueda, activos/inactivos y paginación; crear y editar en un diálogo; desactivar
 * con confirmación). Se configura con un objeto de `config/catalogos.js`. Quien no puede escribir ve solo la lista.
 */
export default function CatalogoCrud({ config, extra }) {
  const { rol } = useAuth()
  const api = useMemo(() => crud(config.ruta), [config.ruta])
  const puedeEscribir = config.escribir.includes(rol)
  const puedeDesactivar = (config.desactivar ?? config.escribir).includes(rol)

  const [texto, setTexto] = useState('')
  const [incluirInactivos, setIncluirInactivos] = useState(false)
  const [pagina, setPagina] = useState(1)
  const [exito, setExito] = useState('')
  const [credenciales, setCredenciales] = useState(null)
  const [dialogo, setDialogo] = useState(null) // { fila }
  const [aDesactivar, setADesactivar] = useState(null)
  const [procesando, setProcesando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  const { datos, error, cargando, recargar } = useApi(`catalogo|${config.clave}`, () => api.listar())
  const opciones = useOpciones(config)

  const esProtegida = (fila) => (config.protegidos ?? []).some((p) => p.toLowerCase() === String(fila[config.campoClave ?? 'descripcion'] ?? '').toLowerCase())
  const etiquetaOpcion = (campo, v) => opciones[campo]?.find((o) => String(o.valor) === String(v))?.etiqueta ?? (v ? `#${v}` : '—')
  const nombreCompleto = (f) => [f.nombre, f.apellidoPaterno, f.apellidoMaterno].filter(Boolean).join(' ')

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    return (datos ?? [])
      .filter((f) => incluirInactivos || esActivo(config, f))
      .filter((f) => !busqueda || JSON.stringify(Object.values(f)).toLowerCase().includes(busqueda))
      .sort((a, b) => a[config.idCampo] - b[config.idCampo])
  }, [datos, texto, incluirInactivos, config])

  const campoPorNombre = Object.fromEntries(config.campos.map((c) => [c.nombre, c]))
  const nombresColumnas = config.columnas ?? config.campos.map((c) => c.nombre)
  const columnas = [
    ...nombresColumnas.map((n) => {
      if (n === 'nombreCompleto') return { clave: n, titulo: 'Nombre', render: (f) => <strong>{nombreCompleto(f) || '—'}</strong> }
      if (n === 'horario') return { clave: n, titulo: 'Horario', render: (f) => `${formatearHora(f.horaInicio)} – ${formatearHora(f.horaFin)}` }
      const c = campoPorNombre[n]
      return {
        clave: n,
        titulo: c?.etiqueta ?? n,
        render: (f) => {
          const v = f[n]
          if (c?.tipo === 'select' && !c.valorTexto) return etiquetaOpcion(n, v)
          if (n === 'color') return v ? <span><span className="muestra-color" style={{ backgroundColor: colorCss(v) }} aria-hidden="true" /> {v}</span> : '—'
          if (c?.tipo === 'decimal') return formatearMoneda(v)
          if (c?.tipo === 'hora') return formatearHora(v)
          return acortar(v === null || v === undefined || v === '' ? '' : String(v), 70)
        },
      }
    }),
    { clave: 'estado', titulo: 'Estado', render: (f) => <BadgeActivo activo={esActivo(config, f)} /> },
  ]
  if (puedeEscribir || puedeDesactivar) {
    columnas.push({
      clave: 'acciones',
      titulo: 'Acciones',
      className: 'text-right',
      render: (f) => (
        <div className="tabla-acciones d-flex flex-wrap justify-content-end">
          {puedeEscribir && (
            <button type="button" className="enlace-boton text-primary" onClick={() => setDialogo({ fila: f })}>
              Editar
            </button>
          )}
          {puedeDesactivar && esActivo(config, f) && !esProtegida(f) && (
            <button
              type="button"
              className="enlace-boton text-danger"
              onClick={() => {
                setErrorDesactivar('')
                setADesactivar(f)
              }}
            >
              Desactivar
            </button>
          )}
        </div>
      ),
    })
  }

  const desactivar = async () => {
    setProcesando(true)
    setErrorDesactivar('')
    try {
      await api.eliminar(aDesactivar[config.idCampo])
      setADesactivar(null)
      setExito('Registro desactivado.')
      recargar()
    } catch (e) {
      // 404 no existe; 400/409/422: estado base, tiene dependencias (pacientes, habitaciones, citas, asignaciones...)
      setErrorDesactivar(normalizarError(e).message)
    } finally {
      setProcesando(false)
    }
  }

  return (
    <div>
      <div className="d-flex flex-column flex-md-row align-items-md-center justify-content-md-between mb-3">
        <div>
          <h2 className="h4 mb-1">{config.titulo}</h2>
          {config.nota && <p className="text-muted small mb-0">{config.nota}</p>}
          {!puedeEscribir && <p className="text-muted small mb-0">Solo lectura: su perfil no puede modificar este catálogo.</p>}
        </div>
        {puedeEscribir && (
          <Button color="primary" className="btn-round mt-2 mt-md-0" onClick={() => setDialogo({ fila: null })}>
            {config.femenino ? 'Nueva' : 'Nuevo'} {config.singular}
          </Button>
        )}
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <CredencialesGeneradas credenciales={credenciales} onCerrar={() => setCredenciales(null)} />
      <ErrorAlert mensaje={error?.message} />

      <Table
        columnas={columnas}
        filas={filas.slice((pagina - 1) * TAMANO_PAGINA, pagina * TAMANO_PAGINA)}
        claveFila={(f) => f[config.idCampo]}
        cargando={cargando}
        busqueda={texto}
        onBuscar={(v) => {
          setTexto(v)
          setPagina(1)
        }}
        placeholderBusqueda="Buscar"
        acciones={
          <label className="d-flex align-items-center small mb-0">
            <input
              type="checkbox"
              checked={incluirInactivos}
              onChange={(e) => {
                setIncluirInactivos(e.target.checked)
                setPagina(1)
              }}
              className="mr-2"
            />
            Mostrar inactivos
          </label>
        }
        vacio={<EmptyState titulo="Sin registros" descripcion="Ajuste la búsqueda o cree un registro." />}
        pie={<Pagination pagina={pagina} totalElementos={filas.length} tamano={TAMANO_PAGINA} onCambiarPagina={setPagina} />}
      />

      {extra?.(datos ?? [])}

      {dialogo && (
        <DialogoCatalogo
          config={config}
          fila={dialogo.fila}
          opciones={opciones}
          protegida={dialogo.fila ? esProtegida(dialogo.fila) : false}
          onCerrar={() => setDialogo(null)}
          onListo={(mensaje, nuevas) => {
            setDialogo(null)
            setExito(mensaje)
            if (nuevas) setCredenciales(nuevas)
            recargar()
          }}
        />
      )}

      <ConfirmDialog
        abierto={Boolean(aDesactivar)}
        titulo={`Desactivar ${config.singular}`}
        mensaje="El registro se desactiva (borrado lógico) y deja de ofrecerse en los formularios. El backend lo impide si tiene dependencias."
        textoConfirmar="Desactivar"
        peligro
        cargando={procesando}
        error={errorDesactivar}
        onConfirmar={desactivar}
        onCancelar={() => setADesactivar(null)}
      />
    </div>
  )
}

const COLORES_CSS = { rojo: '#d9534f', naranja: '#f96332', amarillo: '#ffc107', verde: '#18ce0f', azul: '#2CA8FF' }
function colorCss(nombre) {
  return COLORES_CSS[String(nombre).toLowerCase()] ?? '#9a9a9a'
}
