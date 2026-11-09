import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  actualizarHospitalizacion,
  listarHospitalizacionesActivas,
  obtenerHospitalizacion,
  registrarHospitalizacion,
} from '../../api/hospitalizaciones.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import Loading from '../../components/Loading.jsx'
import { useApi } from '../../hooks/useApi.js'
import { useDatosHospitalizacion } from '../../hooks/useDatosHospitalizacion.js'
import { normalizarError } from '../../utils/errores.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import {
  FORM_HOSP_VACIO,
  formularioAHospitalizacion,
  hospitalizacionAFormulario,
  nombreHabitacion,
  validarHospitalizacion,
} from './utilsHospitalizacion.js'

export default function HospitalizacionForm() {
  const { id } = useParams()
  const esEdicion = Boolean(id)
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const datos = useDatosHospitalizacion()
  const { datos: carga, error: errorCarga, cargando } = useApi(`hospitalizacion-form|${id ?? 'nueva'}`, async () => {
    const [original, activas] = await Promise.all([esEdicion ? obtenerHospitalizacion(id) : null, listarHospitalizacionesActivas().catch(() => [])])
    return { original, activas }
  })

  const [cambios, setCambios] = useState({})
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)

  if (cargando || datos.cargando) return <Loading texto="Cargando..." />

  if (errorCarga) {
    return (
      <div>
        <ErrorAlert
          titulo={errorCarga.status === 404 ? 'Hospitalización no encontrada' : 'No se pudo cargar el formulario'}
          mensaje={errorCarga.message}
        />
        <Link to="/hospitalizaciones" className="volver-enlace">
          &larr; Volver a hospitalizaciones
        </Link>
      </div>
    )
  }

  const { original, activas } = carga
  const iniciales = original ? hospitalizacionAFormulario(original) : { ...FORM_HOSP_VACIO, idPaciente: params.get('idPaciente') ?? '', idHabitacion: params.get('idHabitacion') ?? '' }
  const form = { ...iniciales, ...cambios }

  const cambiar = (e) => {
    const { name, value } = e.target
    setCambios((c) => ({ ...c, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorGeneral('')
    const locales = validarHospitalizacion(form)
    setErrores(locales)
    if (Object.keys(locales).length) return

    setGuardando(true)
    try {
      const payload = formularioAHospitalizacion(form)
      const guardada = esEdicion ? await actualizarHospitalizacion(id, payload) : await registrarHospitalizacion(payload)
      navigate(`/hospitalizaciones/${guardada.idHospitalizacion}`, {
        replace: true,
        state: { mensaje: esEdicion ? 'Hospitalización actualizada.' : 'Paciente ingresado.' },
      })
    } catch (err) {
      // 400: errores por campo en `detalles`; 409: paciente ya hospitalizado; 422: habitación llena o en mantenimiento, referencias inexistentes
      const { message, detalles } = normalizarError(err)
      setErrores(detalles)
      setErrorGeneral(Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
    } finally {
      setGuardando(false)
    }
  }

  // Ocupación actual por habitación (hospitalizaciones activas) para ofrecer solo camas libres
  const ocupacion = {}
  for (const a of activas) ocupacion[a.idHabitacion] = (ocupacion[a.idHabitacion] ?? 0) + 1
  const mantenimiento = datos.estadosHab.find((s) => s.nombre === 'Mantenimiento')?.idEstadoHabitacion
  const opcionesHabitacion = datos.habitaciones
    .filter((h) => h.activo !== false || String(h.idHabitacion) === form.idHabitacion)
    .filter((h) => h.idEstadoHabitacion !== mantenimiento || String(h.idHabitacion) === form.idHabitacion)
    .filter((h) => (ocupacion[h.idHabitacion] ?? 0) < h.capacidad || String(h.idHabitacion) === form.idHabitacion)
    .map((h) => ({
      valor: String(h.idHabitacion),
      etiqueta: `${nombreHabitacion(h)} · ${datos.tiposHabPorId[h.idTipoHabitacion]?.descripcion ?? 'Sin tipo'} · ${ocupacion[h.idHabitacion] ?? 0}/${h.capacidad}`,
    }))
  const opcionesPaciente = datos.pacientes
    .filter((p) => p.activo !== false || String(p.idPaciente) === form.idPaciente)
    .map((p) => ({ valor: String(p.idPaciente), etiqueta: `${nombrePaciente(p)} (DPI ${p.dpiPaciente})` }))
  const opcionesMedico = datos.medicos
    .filter((m) => m.estado !== false || String(m.idMedico) === form.idMedicoResponsable)
    .map((m) => ({ valor: String(m.idMedico), etiqueta: nombreMedico(m) }))
  const opcionesClinica = datos.clinicas.map((c) => ({ valor: String(c.idClinica), etiqueta: c.nombre }))

  return (
    <div>
      <Link
        to={esEdicion ? `/hospitalizaciones/${id}` : '/hospitalizaciones'}
        className="volver-enlace"
      >
        &larr; {esEdicion ? 'Volver al detalle' : 'Volver a hospitalizaciones'}
      </Link>
      <h1 className="h3 mb-0">{esEdicion ? 'Editar hospitalización' : 'Ingresar paciente'}</h1>

      <form onSubmit={enviar} noValidate className="card card-body">
        <ErrorAlert mensaje={errorGeneral} />

        <div className="row">
          <FormField
            nombre="idPaciente"
            etiqueta="Paciente"
            tipo="select"
            opciones={opcionesPaciente}
            valor={form.idPaciente}
            onChange={cambiar}
            error={errores.idPaciente}
            disabled={esEdicion}
            requerido
            ayuda={esEdicion ? 'El paciente no se puede cambiar en una hospitalización registrada.' : undefined}
          />
          <FormField
            nombre="idMedicoResponsable"
            etiqueta="Médico responsable"
            tipo="select"
            opciones={opcionesMedico}
            valor={form.idMedicoResponsable}
            onChange={cambiar}
            error={errores.idMedicoResponsable}
            requerido
          />
          <FormField
            nombre="idHabitacion"
            etiqueta="Habitación"
            tipo="select"
            opciones={opcionesHabitacion}
            valor={form.idHabitacion}
            onChange={cambiar}
            error={errores.idHabitacion}
            requerido
            ayuda={esEdicion ? 'Cambiarla traslada al paciente y actualiza el estado de ambas habitaciones.' : 'Solo se ofrecen habitaciones con cama libre y fuera de mantenimiento.'}
          />
          <FormField
            nombre="idClinica"
            etiqueta="Clínica"
            tipo="select"
            placeholder="Sin clínica"
            opciones={opcionesClinica}
            valor={form.idClinica}
            onChange={cambiar}
            error={errores.idClinica}
          />
          {!esEdicion && (
            <FormField
              nombre="fechaIngreso"
              etiqueta="Fecha y hora de ingreso"
              tipo="datetime-local"
              valor={form.fechaIngreso}
              onChange={cambiar}
              error={errores.fechaIngreso}
              ayuda="Si se deja vacía se usa la fecha y hora actuales. No puede ser futura."
            />
          )}
          <FormField
            className="col-12"
            nombre="motivoIngreso"
            etiqueta="Motivo de ingreso"
            tipo="textarea"
            valor={form.motivoIngreso}
            onChange={cambiar}
            error={errores.motivoIngreso}
            requerido
          />
          <FormField
            className="col-12"
            nombre="diagnosticoIngreso"
            etiqueta="Diagnóstico de ingreso"
            tipo="textarea"
            valor={form.diagnosticoIngreso}
            onChange={cambiar}
            error={errores.diagnosticoIngreso}
          />
        </div>

        <div className="acciones-grupo d-flex justify-content-end border-top pt-3">
          <Link
            to={esEdicion ? `/hospitalizaciones/${id}` : '/hospitalizaciones'}
            className="btn btn-secondary btn-round"
          >
            Cancelar
          </Link>
          <button
            type="submit"
            disabled={guardando}
            className="btn btn-primary btn-round"
          >
            {guardando ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Ingresar paciente'}
          </button>
        </div>
      </form>
    </div>
  )
}
