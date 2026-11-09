import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { actualizarMedico, crearMedico, obtenerMedico } from '../../api/administracion.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import Loading from '../../components/Loading.jsx'
import { useApi } from '../../hooks/useApi.js'
import { normalizarError } from '../../utils/errores.js'

const CAMPOS = ['noColegiado', 'nombre', 'apellidoPaterno', 'apellidoMaterno', 'telefono', 'email']
const EMAIL = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

/** Replica las reglas del backend (MedicoServiceImpl) y los tamaños de las columnas de `medico`. */
function validar(f) {
  const e = {}
  const largo = (campo, etiqueta, max) => {
    if (f[campo].trim().length > max) e[campo] = `${etiqueta}: máximo ${max} caracteres`
  }
  if (!f.noColegiado.trim()) e.noColegiado = 'El número de colegiado es obligatorio'
  largo('noColegiado', 'El número de colegiado', 30)
  if (!f.nombre.trim()) e.nombre = 'El nombre es obligatorio'
  largo('nombre', 'El nombre', 100)
  if (!f.apellidoPaterno.trim()) e.apellidoPaterno = 'El apellido paterno es obligatorio'
  largo('apellidoPaterno', 'El apellido paterno', 100)
  largo('apellidoMaterno', 'El apellido materno', 100)
  if (f.telefono.trim() && (f.telefono.trim().length < 7 || f.telefono.trim().length > 20)) e.telefono = 'El teléfono debe contener entre 7 y 20 caracteres'
  if (f.email.trim() && !EMAIL.test(f.email.trim())) e.email = 'El formato del correo electrónico no es válido'
  largo('email', 'El correo', 150)
  return e
}

export default function MedicoForm() {
  const { id } = useParams()
  const esEdicion = Boolean(id)
  const navigate = useNavigate()
  const { datos: original, error: errorCarga, cargando } = useApi(`medico-form|${id ?? 'nuevo'}`, () => (esEdicion ? obtenerMedico(id) : null))
  const [cambios, setCambios] = useState({})
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)

  if (cargando) return <Loading texto="Cargando..." />
  if (errorCarga) {
    return (
      <div>
        <ErrorAlert titulo={errorCarga.status === 404 ? 'Médico no encontrado' : 'No se pudo cargar el médico'} mensaje={errorCarga.message} />
        <Link to="/personal" className="volver-enlace">
          &larr; Volver a personal
        </Link>
      </div>
    )
  }

  const form = { ...Object.fromEntries(CAMPOS.map((c) => [c, original?.[c] ?? ''])), ...cambios }
  const volver = esEdicion ? `/personal/medicos/${id}` : '/personal'

  const cambiar = (e) => {
    const { name, value } = e.target
    setCambios((c) => ({ ...c, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (ev) => {
    ev.preventDefault()
    setErrorGeneral('')
    const locales = validar(form)
    setErrores(locales)
    if (Object.keys(locales).length) return
    setGuardando(true)
    try {
      const vacioANulo = (v) => (v.trim() === '' ? null : v.trim())
      const payload = {
        noColegiado: form.noColegiado.trim(),
        nombre: form.nombre.trim(),
        apellidoPaterno: form.apellidoPaterno.trim(),
        apellidoMaterno: vacioANulo(form.apellidoMaterno),
        telefono: vacioANulo(form.telefono),
        email: vacioANulo(form.email),
      }
      const guardado = esEdicion ? await actualizarMedico(id, { ...original, ...payload }) : await crearMedico(payload)
      navigate(`/personal/medicos/${guardado.idMedico}`, { replace: true, state: { mensaje: esEdicion ? 'Médico actualizado.' : 'Médico registrado. Ahora puede asignarle especialidades.', credenciales: esEdicion ? null : guardado.credenciales } })
    } catch (err) {
      // 400: `detalles` por campo (noColegiado, nombre, apellidoPaterno, telefono, email); 409: colegiado o correo repetidos; 404: no existe
      const { message, detalles } = normalizarError(err)
      setErrores(detalles ?? {})
      setErrorGeneral(Object.keys(detalles ?? {}).length ? 'Revise los campos marcados.' : message)
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div>
      <Link to={volver} className="volver-enlace">
        &larr; {esEdicion ? 'Volver al médico' : 'Volver a personal'}
      </Link>
      <h1 className="h3 mb-0">{esEdicion ? 'Editar médico' : 'Nuevo médico'}</h1>
      <form onSubmit={enviar} noValidate className="card card-body">
        <ErrorAlert mensaje={errorGeneral} />
        <div className="row">
          <FormField nombre="noColegiado" etiqueta="Número de colegiado" valor={form.noColegiado} onChange={cambiar} error={errores.noColegiado} requerido maxLength={30} />
          <FormField nombre="nombre" etiqueta="Nombre" valor={form.nombre} onChange={cambiar} error={errores.nombre} requerido maxLength={100} />
          <FormField nombre="apellidoPaterno" etiqueta="Apellido paterno" valor={form.apellidoPaterno} onChange={cambiar} error={errores.apellidoPaterno} requerido maxLength={100} />
          <FormField nombre="apellidoMaterno" etiqueta="Apellido materno" valor={form.apellidoMaterno} onChange={cambiar} error={errores.apellidoMaterno} maxLength={100} />
          <FormField nombre="telefono" etiqueta="Teléfono" valor={form.telefono} onChange={cambiar} error={errores.telefono} maxLength={20} ayuda="Entre 7 y 20 caracteres." />
          <FormField nombre="email" etiqueta="Correo electrónico" tipo="email" valor={form.email} onChange={cambiar} error={errores.email} maxLength={150} />
        </div>
        <div className="acciones-grupo d-flex justify-content-end border-top pt-3">
          <Link to={volver} className="btn btn-secondary btn-round">
            Cancelar
          </Link>
          <button type="submit" disabled={guardando} className="btn btn-primary btn-round">
            {guardando ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Registrar médico'}
          </button>
        </div>
      </form>
    </div>
  )
}
