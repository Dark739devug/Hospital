import { useState } from 'react'
import { crearTratamiento } from '../../api/tratamientos.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import { normalizarError } from '../../utils/errores.js'
import { FORM_TRATAMIENTO_VACIO, formularioATratamiento, validarTratamiento } from '../tratamientos/utilsTratamiento.js'

/** Registra un tratamiento para un diagnóstico de la consulta, sin salir del detalle. */
export default function TratamientoNuevoConsulta({ idDiagnostico, onRegistrado, onCancelar }) {
  const [form, setForm] = useState({ ...FORM_TRATAMIENTO_VACIO, idDiagnostico: String(idDiagnostico) })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)

  const cambiar = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorGeneral('')
    const locales = validarTratamiento(form)
    setErrores(locales)
    if (Object.keys(locales).length) return

    setGuardando(true)
    try {
      await crearTratamiento(formularioATratamiento(form))
      onRegistrado('Tratamiento registrado.')
    } catch (err) {
      const { message, detalles } = normalizarError(err)
      setErrores(detalles)
      setErrorGeneral(Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
      setGuardando(false)
    }
  }

  return (
    <form onSubmit={enviar} noValidate className="border-top pt-3 mt-2">
      <ErrorAlert mensaje={errorGeneral} />
      <fieldset disabled={guardando} className="row">
        <FormField
          className="col-12"
          nombre="descripcion"
          etiqueta="Tratamiento"
          valor={form.descripcion}
          onChange={cambiar}
          error={errores.descripcion}
          maxLength={255}
          requerido
          autoFocus
        />
        <FormField
          className="col-md-4"
          nombre="duracionDias"
          etiqueta="Duración (días)"
          tipo="number"
          step="1"
          min="1"
          valor={form.duracionDias}
          onChange={cambiar}
          error={errores.duracionDias}
        />
        <FormField
          className="col-md-4"
          nombre="fechaInicio"
          etiqueta="Fecha de inicio"
          tipo="date"
          valor={form.fechaInicio}
          onChange={cambiar}
          error={errores.fechaInicio}
          ayuda="Si se deja vacía se usa la fecha de hoy."
        />
        <FormField
          className="col-md-4"
          nombre="fechaFin"
          etiqueta="Fecha de fin"
          tipo="date"
          valor={form.fechaFin}
          onChange={cambiar}
          error={errores.fechaFin}
        />
        <FormField
          className="col-12"
          nombre="indicaciones"
          etiqueta="Indicaciones"
          tipo="textarea"
          valor={form.indicaciones}
          onChange={cambiar}
          error={errores.indicaciones}
        />
      </fieldset>
      <div className="acciones-grupo d-flex justify-content-end">
        <button type="button" onClick={onCancelar} disabled={guardando} className="btn btn-secondary btn-round">
          Cancelar
        </button>
        <button type="submit" disabled={guardando} className="btn btn-primary btn-round">
          {guardando ? 'Guardando...' : 'Registrar tratamiento'}
        </button>
      </div>
    </form>
  )
}
