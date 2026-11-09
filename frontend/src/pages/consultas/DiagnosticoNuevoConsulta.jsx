import { useState } from 'react'
import { asociarDiagnostico } from '../../api/consultas.js'
import { crearDiagnostico } from '../../api/diagnosticos.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import { normalizarError } from '../../utils/errores.js'
import { hoyISO } from '../../utils/formato.js'
import { FORM_DIAGNOSTICO_VACIO, formularioADiagnostico, validarDiagnostico } from '../diagnosticos/utilsDiagnostico.js'

/**
 * Registra un diagnóstico y lo asocia a la consulta en un solo paso.
 * Son dos peticiones (crear y asociar): si la asociación falla, se recuerda el diagnóstico ya creado
 * para que al reintentar solo se asocie y no quede duplicado.
 */
export default function DiagnosticoNuevoConsulta({ idConsulta, tipos, onRegistrado }) {
  const [form, setForm] = useState(FORM_DIAGNOSTICO_VACIO)
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [idCreadoSinAsociar, setIdCreadoSinAsociar] = useState(null)

  const cambiar = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }))
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorGeneral('')
    let idDiagnostico = idCreadoSinAsociar
    if (!idDiagnostico) {
      const locales = validarDiagnostico(form)
      setErrores(locales)
      if (Object.keys(locales).length) return
    }

    setGuardando(true)
    try {
      if (!idDiagnostico) {
        idDiagnostico = (await crearDiagnostico(formularioADiagnostico(form))).idDiagnostico
        setIdCreadoSinAsociar(idDiagnostico)
      }
      await asociarDiagnostico(idDiagnostico, idConsulta)
      onRegistrado('Diagnóstico registrado en la consulta.')
    } catch (err) {
      const { message, detalles } = normalizarError(err)
      if (idDiagnostico) {
        setErrorGeneral(`El diagnóstico se guardó, pero no se pudo asociar a la consulta: ${message} Pulse "Reintentar asociación".`)
      } else {
        setErrores(detalles)
        setErrorGeneral(Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
      }
      setGuardando(false)
    }
  }

  const opcionesTipo = tipos
    .filter((t) => t.activo !== false)
    .map((t) => ({ valor: String(t.idTipoDiagnostico), etiqueta: t.descripcion }))
  const bloqueado = guardando || Boolean(idCreadoSinAsociar)

  return (
    <form onSubmit={enviar} noValidate>
      <ErrorAlert mensaje={errorGeneral} />
      <fieldset disabled={bloqueado} className="row">
        <FormField
          className="col-12"
          nombre="descripcion"
          etiqueta="Descripción"
          valor={form.descripcion}
          onChange={cambiar}
          error={errores.descripcion}
          maxLength={255}
          requerido
        />
        <FormField
          nombre="idTipoDiagnostico"
          etiqueta="Tipo de diagnóstico"
          tipo="select"
          opciones={opcionesTipo}
          valor={form.idTipoDiagnostico}
          onChange={cambiar}
          error={errores.idTipoDiagnostico}
          requerido
        />
        <FormField
          nombre="fecha"
          etiqueta="Fecha"
          tipo="date"
          valor={form.fecha}
          onChange={cambiar}
          error={errores.fecha}
          max={hoyISO()}
          ayuda="Si se deja vacía se usa la fecha de hoy."
        />
        <FormField
          className="col-12"
          nombre="observaciones"
          etiqueta="Observaciones"
          tipo="textarea"
          valor={form.observaciones}
          onChange={cambiar}
          error={errores.observaciones}
        />
      </fieldset>
      <div className="d-flex justify-content-end">
        <button type="submit" disabled={guardando} className="btn btn-primary btn-round">
          {guardando ? 'Guardando...' : idCreadoSinAsociar ? 'Reintentar asociación' : 'Registrar diagnóstico'}
        </button>
      </div>
    </form>
  )
}
