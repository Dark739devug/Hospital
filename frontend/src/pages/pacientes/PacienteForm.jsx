import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { listarAlergias, listarTiposSangre } from '../../api/catalogos.js'
import { actualizarPaciente, crearPaciente, obtenerPaciente } from '../../api/pacientes.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import Loading from '../../components/Loading.jsx'
import { normalizarError } from '../../utils/errores.js'
import { ayerISO } from '../../utils/formato.js'
import {
  FORM_VACIO,
  SEXOS,
  formularioAPeticion,
  nombreCompleto,
  pacienteAFormulario,
  validarPaciente,
} from './utilsPaciente.js'

export default function PacienteForm() {
  const { id } = useParams()
  const esEdicion = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(FORM_VACIO)
  const [alergias, setAlergias] = useState([])
  const [tiposSangre, setTiposSangre] = useState([])
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [errorCarga, setErrorCarga] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    let cancelado = false
    // El estado se reinicia dentro de la promesa y no de forma síncrona en el efecto (igual que useApi).
    Promise.resolve()
      .then(() => {
        if (cancelado) return null
        setCargando(true)
        setErrorCarga(null)
        const catalogos = Promise.allSettled([listarAlergias(), listarTiposSangre()])
        const paciente = esEdicion ? obtenerPaciente(id) : Promise.resolve(null)
        return Promise.all([paciente, catalogos])
      })
      .then((resultado) => {
        if (cancelado || !resultado) return
        const [p, [a, t]] = resultado
        if (p) setForm(pacienteAFormulario(p))
        if (a.status === 'fulfilled') setAlergias(a.value)
        if (t.status === 'fulfilled') setTiposSangre(t.value)
      })
      .catch((e) => {
        if (!cancelado) setErrorCarga(normalizarError(e))
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [id, esEdicion])

  const cambiar = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorGeneral('')
    const locales = validarPaciente(form)
    setErrores(locales)
    if (Object.keys(locales).length) return

    setGuardando(true)
    try {
      const datos = formularioAPeticion(form)
      const guardado = esEdicion ? await actualizarPaciente(id, datos) : await crearPaciente(datos)
      navigate(`/pacientes/${guardado.idPaciente}`, {
        replace: true,
        state: { mensaje: esEdicion ? 'Paciente actualizado.' : 'Paciente registrado.' },
      })
    } catch (err) {
      const { status, message, detalles } = normalizarError(err)
      setErrores(detalles) // 400: detalles campo -> mensaje
      // 400 muestra los errores bajo cada campo; 404, 409, 422, 403 y otros muestran `message`.
      setErrorGeneral(status === 400 && Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <Loading texto="Cargando..." />

  if (errorCarga) {
    return (
      <div>
        <ErrorAlert
          titulo={errorCarga.status === 404 ? 'Paciente no encontrado' : 'No se pudo cargar el formulario'}
          mensaje={errorCarga.message}
        />
        <Link to="/pacientes" className="volver-enlace">
          &larr; Volver a pacientes
        </Link>
      </div>
    )
  }

  const opcionesCatalogo = (lista, clave) =>
    lista.filter((x) => x.activo !== false).map((x) => ({ valor: String(x[clave]), etiqueta: x.nombre }))

  return (
    <div>
      <Link to={esEdicion ? `/pacientes/${id}` : '/pacientes'} className="volver-enlace">
        &larr; {esEdicion ? 'Volver al detalle' : 'Volver a pacientes'}
      </Link>
      <h1 className="h3 mb-0">
        {esEdicion ? `Editar paciente: ${nombreCompleto(form)}` : 'Nuevo paciente'}
      </h1>

      <form onSubmit={enviar} noValidate className="card card-body">
        <ErrorAlert mensaje={errorGeneral} />

        <div className="row">
          <FormField
            nombre="dpiPaciente"
            etiqueta="DPI"
            valor={form.dpiPaciente}
            onChange={cambiar}
            error={errores.dpiPaciente}
            requerido
            inputMode="numeric"
            maxLength={13}
            placeholder="13 dígitos"
          />
          <FormField
            nombre="sexo"
            etiqueta="Sexo"
            tipo="select"
            opciones={SEXOS}
            valor={form.sexo}
            onChange={cambiar}
            error={errores.sexo}
            requerido
          />
          <FormField
            nombre="nombre"
            etiqueta="Nombre"
            valor={form.nombre}
            onChange={cambiar}
            error={errores.nombre}
            requerido
            maxLength={100}
          />
          <FormField
            nombre="fechaNacimiento"
            etiqueta="Fecha de nacimiento"
            tipo="date"
            valor={form.fechaNacimiento}
            onChange={cambiar}
            error={errores.fechaNacimiento}
            max={ayerISO()}
          />
          <FormField
            nombre="apellidoPaterno"
            etiqueta="Apellido paterno"
            valor={form.apellidoPaterno}
            onChange={cambiar}
            error={errores.apellidoPaterno}
            requerido
            maxLength={100}
          />
          <FormField
            nombre="apellidoMaterno"
            etiqueta="Apellido materno"
            valor={form.apellidoMaterno}
            onChange={cambiar}
            error={errores.apellidoMaterno}
            requerido
            maxLength={100}
          />
          <FormField
            nombre="telefono"
            etiqueta="Teléfono"
            tipo="tel"
            valor={form.telefono}
            onChange={cambiar}
            error={errores.telefono}
            requerido
            maxLength={20}
          />
          <FormField
            nombre="correoElectronico"
            etiqueta="Correo electrónico"
            tipo="email"
            valor={form.correoElectronico}
            onChange={cambiar}
            error={errores.correoElectronico}
            maxLength={150}
          />
          <FormField
            nombre="idTipoDeSangre"
            etiqueta="Tipo de sangre"
            tipo="select"
            placeholder="Sin especificar"
            opciones={opcionesCatalogo(tiposSangre, 'idTipoDeSangre')}
            valor={form.idTipoDeSangre}
            onChange={cambiar}
            error={errores.idTipoDeSangre}
          />
          <FormField
            nombre="idAlergias"
            etiqueta="Alergia"
            tipo="select"
            placeholder="Ninguna"
            opciones={opcionesCatalogo(alergias, 'idAlergias')}
            valor={form.idAlergias}
            onChange={cambiar}
            error={errores.idAlergias}
          />
          <FormField
            className="col-12"
            nombre="direccion"
            etiqueta="Dirección"
            tipo="textarea"
            valor={form.direccion}
            onChange={cambiar}
            error={errores.direccion}
            maxLength={255}
          />
        </div>

        <div className="acciones-grupo d-flex justify-content-end border-top pt-3">
          <Link
            to={esEdicion ? `/pacientes/${id}` : '/pacientes'}
            className="btn btn-secondary btn-round"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={guardando}
            className="btn btn-primary btn-round"
          >
            {guardando ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Registrar paciente'}
          </button>
        </div>
      </form>
    </div>
  )
}
