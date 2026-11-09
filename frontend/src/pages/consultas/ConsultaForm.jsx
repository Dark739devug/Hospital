import { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { listarTiposDiagnostico } from '../../api/catalogos.js'
import { listarCitas } from '../../api/citas.js'
import { actualizarConsulta, agregarMedicamentoConsulta, asociarDiagnostico, crearConsulta, listarConsultas, obtenerConsulta } from '../../api/consultas.js'
import { listarMedicamentos } from '../../api/medicamentos.js'
import { crearDiagnostico } from '../../api/diagnosticos.js'
import { crearTratamiento } from '../../api/tratamientos.js'
import { useAuth } from '../../auth/useAuth.js'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import FormField from '../../components/FormField.jsx'
import Loading from '../../components/Loading.jsx'
import { puede } from '../../config/menu.js'
import { useApi } from '../../hooks/useApi.js'
import { useDatosCita } from '../../hooks/useDatosCita.js'
import { normalizarError } from '../../utils/errores.js'
import { formatearFecha, formatearHora, formatearMoneda, hoyISO } from '../../utils/formato.js'
import { nombreMedico, nombrePaciente } from '../citas/utilsCita.js'
import { FORM_DIAGNOSTICO_VACIO, formularioADiagnostico, validarDiagnostico } from '../diagnosticos/utilsDiagnostico.js'
import { FORM_TRATAMIENTO_VACIO, formularioATratamiento, validarTratamiento } from '../tratamientos/utilsTratamiento.js'
import {
  FORM_CONSULTA_VACIO,
  consultaAFormulario,
  consultaEditable,
  formularioAConsulta,
  textoFinEdicion,
  validarConsulta,
} from './utilsConsulta.js'

/** Antepone `prefijo.` a las claves de un objeto de errores. */
const conPrefijo = (prefijo, errores) =>
  Object.fromEntries(Object.entries(errores).map(([campo, mensaje]) => [`${prefijo}.${campo}`, mensaje]))


export default function ConsultaForm() {
  const { id } = useParams()
  const esEdicion = Boolean(id)
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { rol } = useAuth()
  const datosCita = useDatosCita()
  // Diagnóstico y tratamiento en el mismo registro: requieren poder crear ambos (ADMIN y MEDICO)
  const puedeRegistrarClinico = puede(rol, 'diagnosticos', 'crear') && puede(rol, 'tratamientos', 'crear')
  const puedeAgregarMedicamento = puede(rol, 'consultas', 'agregarMedicamento')

  const { datos, error: errorCarga, cargando } = useApi(`consulta-form|${id ?? 'nueva'}`, async () => {
    const [consulta, citas, consultas, tipos, medicamentos] = await Promise.all([
      esEdicion ? obtenerConsulta(id) : null,
      listarCitas(),
      listarConsultas(),
      esEdicion ? [] : listarTiposDiagnostico().catch(() => []),
      !esEdicion && puedeAgregarMedicamento ? listarMedicamentos({ soloActivos: true }).catch(() => []) : [],
    ])
    return { consulta, citas, consultas, tipos, medicamentos }
  })

  const [cambios, setCambios] = useState({})
  const [diagnostico, setDiagnostico] = useState(FORM_DIAGNOSTICO_VACIO)
  const [tratamiento, setTratamiento] = useState(FORM_TRATAMIENTO_VACIO)
  const [conDiagnostico, setConDiagnostico] = useState(false)
  const [conTratamiento, setConTratamiento] = useState(false)
  // Medicamentos del tratamiento: [{ idMedicamento, cantidad }]; se cobran en la factura de la consulta
  const [recetados, setRecetados] = useState([])
  const [nuevoMedicamento, setNuevoMedicamento] = useState({ idMedicamento: '', cantidad: '1' })
  const [errores, setErrores] = useState({})
  const [errorGeneral, setErrorGeneral] = useState('')
  const [guardando, setGuardando] = useState(false)

  if (cargando || datosCita.cargando) return <Loading texto="Cargando..." />

  const volver = esEdicion ? `/consultas/${id}` : '/consultas'

  if (errorCarga) {
    return (
      <div>
        <ErrorAlert
          titulo={errorCarga.status === 404 ? 'Consulta no encontrada' : 'No se pudo cargar el formulario'}
          mensaje={errorCarga.message}
        />
        <Link to="/consultas" className="volver-enlace">
          &larr; Volver a consultas
        </Link>
      </div>
    )
  }

  const { consulta, citas, consultas, tipos, medicamentos } = datos
  const medicamentoPorId = (idMed) => medicamentos.find((m) => m.idMedicamento === idMed)

  // Consultas pasadas: solo lectura (24 horas desde la atención). El backend también lo rechaza.
  if (esEdicion && !consultaEditable(consulta)) {
    return (
      <div>
        <Link to={volver} className="volver-enlace">
          &larr; Volver al detalle
        </Link>
        <ErrorAlert
          tipo="aviso"
          titulo="Esta consulta ya no se puede editar"
          mensaje={`Las consultas solo se modifican durante las 24 horas siguientes a la atención (hasta el ${textoFinEdicion(consulta)}).`}
        />
      </div>
    )
  }

  const iniciales = consulta
    ? consultaAFormulario(consulta)
    : { ...FORM_CONSULTA_VACIO, idCita: params.get('idCita') ?? '' }
  const form = { ...iniciales, ...cambios }

  // Una cita solo admite una consulta, y solo si está activa, Confirmada o En curso y su fecha no es
  // futura. Al editar se mantiene siempre la cita actual.
  const citasConConsulta = new Set(consultas.map((c) => c.idCita))
  const atendible = (c) =>
    c.activo !== false &&
    !citasConConsulta.has(c.idCita) &&
    c.fecha <= hoyISO() &&
    ['confirmada', 'en curso'].includes((datosCita.estadosPorId[c.idEstado]?.descripcion ?? '').toLowerCase())
  const citasDisponibles = citas.filter((c) => atendible(c) || String(c.idCita) === form.idCita)
  const opcionesCita = citasDisponibles.map((c) => ({
    valor: String(c.idCita),
    etiqueta: `#${c.idCita} · ${formatearFecha(c.fecha)} ${formatearHora(c.hora)} · ${nombrePaciente(datosCita.pacientesPorId[c.idPaciente], c.idPaciente)} · ${nombreMedico(datosCita.medicosPorId[c.idMedico], c.idMedico)}`,
  }))
  const opcionesTipo = tipos
    .filter((t) => t.activo !== false)
    .map((t) => ({ valor: String(t.idTipoDiagnostico), etiqueta: t.descripcion }))

  const limpiarError = (nombre) => {
    if (errores[nombre]) setErrores((er) => ({ ...er, [nombre]: undefined }))
  }
  const cambiar = (e) => {
    const { name, value } = e.target
    setCambios((c) => ({ ...c, [name]: value }))
    limpiarError(name)
  }
  // Los campos de diagnóstico y tratamiento se llaman `diagnostico.x` / `tratamiento.x` para no chocar con los de la consulta
  const cambiarSeccion = (setter) => (e) => {
    const { name, value } = e.target
    setter((f) => ({ ...f, [name.split('.')[1]]: value }))
    limpiarError(name)
  }
  const cambiarDiagnostico = cambiarSeccion(setDiagnostico)
  const cambiarTratamiento = cambiarSeccion(setTratamiento)

  const validar = () => {
    let e = validarConsulta(form)
    if (!esEdicion && conDiagnostico) e = { ...e, ...conPrefijo('diagnostico', validarDiagnostico(diagnostico)) }
    if (!esEdicion && conDiagnostico && conTratamiento) {
      // El diagnóstico del tratamiento es el que se crea en este mismo registro
      const { idDiagnostico: _ignorado, ...erroresTratamiento } = validarTratamiento({ ...tratamiento, idDiagnostico: 'nuevo' })
      e = { ...e, ...conPrefijo('tratamiento', erroresTratamiento) }
    }
    return e
  }

  const enviar = async (e) => {
    e.preventDefault()
    setErrorGeneral('')
    const locales = validar()
    setErrores(locales)
    if (Object.keys(locales).length) {
      setErrorGeneral('Revise los campos marcados.')
      return
    }

    setGuardando(true)
    try {
      const datosConsulta = formularioAConsulta(form)
      let idConsulta
      let mensaje
      if (esEdicion) {
        idConsulta = (await actualizarConsulta(id, datosConsulta)).idConsulta
        mensaje = 'Consulta actualizada.'
      } else {
        // Cada parte va a su endpoint: consulta, diagnóstico, asociación y tratamiento
        idConsulta = (await crearConsulta(datosConsulta)).idConsulta
        mensaje = 'Consulta registrada. La cita pasó automáticamente al estado «Atendida».'
        if (conDiagnostico) {
          // La consulta ya quedó guardada: si falla un paso de aquí en adelante se avisa en el detalle,
          // donde se puede completar el diagnóstico o el tratamiento.
          let paso = 'registrar el diagnóstico'
          try {
            const { idDiagnostico } = await crearDiagnostico(formularioADiagnostico(diagnostico))
            paso = 'asociar el diagnóstico a la consulta'
            await asociarDiagnostico(idDiagnostico, idConsulta)
            if (conTratamiento) {
              paso = 'registrar el tratamiento'
              const { idTratamiento } = await crearTratamiento(formularioATratamiento({ ...tratamiento, idDiagnostico: String(idDiagnostico) }))
              // Cada medicamento se asigna al tratamiento y se suma a la factura de la consulta
              for (const r of recetados) {
                paso = `agregar ${medicamentoPorId(r.idMedicamento)?.nombreComercial ?? 'el medicamento'}`
                await agregarMedicamentoConsulta(idConsulta, { idTratamiento, idMedicamento: r.idMedicamento, cantidad: r.cantidad })
              }
            }
            mensaje = !conTratamiento
              ? 'Consulta registrada con su diagnóstico.'
              : recetados.length
                ? 'Consulta registrada con su diagnóstico, tratamiento y medicamentos (sumados a la factura de la consulta).'
                : 'Consulta registrada con su diagnóstico y tratamiento.'
          } catch (err) {
            navigate(`/consultas/${idConsulta}`, {
              replace: true,
              state: { mensaje, error: `No se pudo ${paso}: ${normalizarError(err).message}` },
            })
            return
          }
        }
      }
      navigate(`/consultas/${idConsulta}`, { replace: true, state: { mensaje } })
    } catch (err) {
      const { message, detalles } = normalizarError(err)
      setErrores(detalles)
      setErrorGeneral(Object.keys(detalles).length ? 'Revise los campos marcados.' : message)
      setGuardando(false)
    }
  }

  return (
    <div>
      <Link to={volver} className="volver-enlace">
        &larr; {esEdicion ? 'Volver al detalle' : 'Volver a consultas'}
      </Link>
      <h1 className="h3 mb-0">{esEdicion ? `Editar consulta #${id}` : 'Nueva consulta'}</h1>

      <form onSubmit={enviar} noValidate className="card card-body">
        <ErrorAlert mensaje={errorGeneral} />
        {esEdicion && (
          <ErrorAlert
            tipo="aviso"
            mensaje={`Se puede modificar hasta el ${textoFinEdicion(consulta)}.`}
          />
        )}

        <h2 className="h5 mb-3">Datos clínicos</h2>
        <div className="row">
          <FormField
            className="col-12"
            nombre="idCita"
            etiqueta="Cita"
            tipo="select"
            placeholder="Seleccione la cita atendida"
            opciones={opcionesCita}
            valor={form.idCita}
            onChange={cambiar}
            error={errores.idCita}
            disabled={esEdicion}
            requerido
            ayuda={!esEdicion ? 'Solo aparecen citas Confirmadas o En curso, con fecha de hoy o anterior, que aún no tienen consulta.' : undefined}
          />
          <FormField
            nombre="fechaHora"
            etiqueta="Fecha y hora de atención"
            tipo="datetime-local"
            valor={form.fechaHora}
            onChange={cambiar}
            error={errores.fechaHora}
            disabled={esEdicion}
            ayuda={esEdicion ? undefined : 'Si se deja vacía se usa la fecha y hora actuales.'}
          />
          <FormField
            nombre="presionArterial"
            etiqueta="Presión arterial"
            valor={form.presionArterial}
            onChange={cambiar}
            error={errores.presionArterial}
            placeholder="120/80"
            maxLength={10}
          />
          <FormField nombre="peso" etiqueta="Peso (kg)" tipo="number" step="0.01" min="0" valor={form.peso} onChange={cambiar} error={errores.peso} />
          <FormField nombre="talla" etiqueta="Talla (cm)" tipo="number" step="0.01" min="0" valor={form.talla} onChange={cambiar} error={errores.talla} />
          <FormField
            nombre="temperatura"
            etiqueta="Temperatura (°C)"
            tipo="number"
            step="0.1"
            min="30"
            max="45"
            valor={form.temperatura}
            onChange={cambiar}
            error={errores.temperatura}
          />
          <FormField
            nombre="frecuenciaCardiaca"
            etiqueta="Frecuencia cardíaca (lpm)"
            tipo="number"
            step="1"
            min="20"
            max="250"
            valor={form.frecuenciaCardiaca}
            onChange={cambiar}
            error={errores.frecuenciaCardiaca}
          />
          <FormField className="col-12" nombre="sintomas" etiqueta="Síntomas" tipo="textarea" valor={form.sintomas} onChange={cambiar} error={errores.sintomas} />
        </div>

        {!esEdicion && puedeRegistrarClinico && (
          <div className="border-top pt-3 mb-3">
            <div className="d-flex align-items-center justify-content-between">
              <h2 className="h5 mb-0">Diagnóstico</h2>
              <button
                type="button"
                className={`btn btn-link p-0 font-weight-bold${conDiagnostico ? ' text-danger' : ''}`}
                onClick={() => {
                  setConDiagnostico((v) => !v)
                  setConTratamiento(false)
                }}
              >
                {conDiagnostico ? 'Quitar diagnóstico' : '+ Agregar diagnóstico'}
              </button>
            </div>
            {!conDiagnostico ? (
              <p className="text-muted small mb-0">Opcional.</p>
            ) : (
              <div className="row mt-3">
                <FormField
                  className="col-12"
                  nombre="diagnostico.descripcion"
                  etiqueta="Diagnóstico"
                  valor={diagnostico.descripcion}
                  onChange={cambiarDiagnostico}
                  error={errores['diagnostico.descripcion']}
                  maxLength={255}
                  requerido
                />
                <FormField
                  nombre="diagnostico.idTipoDiagnostico"
                  etiqueta="Tipo de diagnóstico"
                  tipo="select"
                  opciones={opcionesTipo}
                  valor={diagnostico.idTipoDiagnostico}
                  onChange={cambiarDiagnostico}
                  error={errores['diagnostico.idTipoDiagnostico']}
                  requerido
                />
                <FormField
                  nombre="diagnostico.fecha"
                  etiqueta="Fecha del diagnóstico"
                  tipo="date"
                  valor={diagnostico.fecha}
                  onChange={cambiarDiagnostico}
                  error={errores['diagnostico.fecha']}
                  max={hoyISO()}
                  ayuda="Si se deja vacía se usa la fecha de hoy."
                />
                <FormField
                  className="col-12"
                  nombre="diagnostico.observaciones"
                  etiqueta="Observaciones"
                  tipo="textarea"
                  valor={diagnostico.observaciones}
                  onChange={cambiarDiagnostico}
                  error={errores['diagnostico.observaciones']}
                />
              </div>
            )}
          </div>
        )}

        {!esEdicion && puedeRegistrarClinico && conDiagnostico && (
          <div className="border-top pt-3 mb-3">
            <div className="d-flex align-items-center justify-content-between">
              <h2 className="h5 mb-0">Tratamiento</h2>
              <button
                type="button"
                className={`btn btn-link p-0 font-weight-bold${conTratamiento ? ' text-danger' : ''}`}
                onClick={() => setConTratamiento((v) => !v)}
              >
                {conTratamiento ? 'Quitar tratamiento' : '+ Agregar tratamiento'}
              </button>
            </div>
            {!conTratamiento ? (
              <p className="text-muted small mb-0">Opcional.</p>
            ) : (
              <div className="row mt-3">
                <FormField
                  className="col-12"
                  nombre="tratamiento.descripcion"
                  etiqueta="Tratamiento"
                  valor={tratamiento.descripcion}
                  onChange={cambiarTratamiento}
                  error={errores['tratamiento.descripcion']}
                  maxLength={255}
                  requerido
                />
                <FormField
                  className="col-md-4"
                  nombre="tratamiento.duracionDias"
                  etiqueta="Duración (días)"
                  tipo="number"
                  step="1"
                  min="1"
                  valor={tratamiento.duracionDias}
                  onChange={cambiarTratamiento}
                  error={errores['tratamiento.duracionDias']}
                />
                <FormField
                  className="col-md-4"
                  nombre="tratamiento.fechaInicio"
                  etiqueta="Fecha de inicio"
                  tipo="date"
                  valor={tratamiento.fechaInicio}
                  onChange={cambiarTratamiento}
                  error={errores['tratamiento.fechaInicio']}
                  ayuda="Si se deja vacía se usa la fecha de hoy."
                />
                <FormField
                  className="col-md-4"
                  nombre="tratamiento.fechaFin"
                  etiqueta="Fecha de fin"
                  tipo="date"
                  valor={tratamiento.fechaFin}
                  onChange={cambiarTratamiento}
                  error={errores['tratamiento.fechaFin']}
                />
                <FormField
                  className="col-12"
                  nombre="tratamiento.indicaciones"
                  etiqueta="Indicaciones"
                  tipo="textarea"
                  valor={tratamiento.indicaciones}
                  onChange={cambiarTratamiento}
                  error={errores['tratamiento.indicaciones']}
                />
                {puedeAgregarMedicamento && (
                  <div className="col-12">
                    <label className="d-block font-weight-bold small mb-1">Medicamentos</label>
                    {recetados.length === 0 ? (
                      <p className="text-muted small mb-2">Opcional. Se cobran en la factura de la consulta.</p>
                    ) : (
                      <ul className="lista-divisoria mb-2">
                        {recetados.map((r) => {
                          const m = medicamentoPorId(r.idMedicamento)
                          return (
                            <li key={r.idMedicamento} className="d-flex justify-content-between align-items-center small">
                              <span>
                                {m?.nombreComercial}
                                {m?.presentacion ? ` (${m.presentacion})` : ''} × {r.cantidad}
                              </span>
                              <span className="d-flex align-items-center acciones-grupo">
                                <span className="text-muted">{formatearMoneda(Number(m?.precio ?? 0) * r.cantidad)}</span>
                                <button
                                  type="button"
                                  className="btn btn-link text-danger p-0"
                                  onClick={() => setRecetados((lista) => lista.filter((x) => x.idMedicamento !== r.idMedicamento))}
                                >
                                  Quitar
                                </button>
                              </span>
                            </li>
                          )
                        })}
                      </ul>
                    )}
                    <div className="d-flex flex-wrap align-items-center acciones-grupo mb-3">
                      <select
                        aria-label="Medicamento"
                        className="form-control form-control-sm w-auto"
                        value={nuevoMedicamento.idMedicamento}
                        onChange={(e) => setNuevoMedicamento((n) => ({ ...n, idMedicamento: e.target.value }))}
                      >
                        <option value="">Agregar medicamento...</option>
                        {medicamentos
                          .filter((m) => !recetados.some((r) => r.idMedicamento === m.idMedicamento))
                          .map((m) => (
                            <option key={m.idMedicamento} value={m.idMedicamento} disabled={m.stock <= 0}>
                              {m.nombreComercial}
                              {m.presentacion ? ` (${m.presentacion})` : ''} · {formatearMoneda(m.precio)}
                              {m.stock <= 0 ? ' · sin stock' : ''}
                            </option>
                          ))}
                      </select>
                      <input
                        aria-label="Cantidad"
                        type="number"
                        min="1"
                        max="1000"
                        step="1"
                        className="form-control form-control-sm cantidad-medicamento"
                        value={nuevoMedicamento.cantidad}
                        onChange={(e) => setNuevoMedicamento((n) => ({ ...n, cantidad: e.target.value }))}
                      />
                      <button
                        type="button"
                        className="btn btn-outline-secondary btn-round btn-sm mb-0"
                        disabled={
                          !nuevoMedicamento.idMedicamento ||
                          !Number.isInteger(Number(nuevoMedicamento.cantidad)) ||
                          Number(nuevoMedicamento.cantidad) < 1 ||
                          Number(nuevoMedicamento.cantidad) > 1000
                        }
                        onClick={() => {
                          setRecetados((lista) => [...lista, { idMedicamento: Number(nuevoMedicamento.idMedicamento), cantidad: Number(nuevoMedicamento.cantidad) }])
                          setNuevoMedicamento({ idMedicamento: '', cantidad: '1' })
                        }}
                      >
                        Agregar
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        <div className="acciones-grupo d-flex justify-content-end border-top pt-3">
          <Link to={volver} className="btn btn-secondary btn-round">
            Cancelar
          </Link>
          <button type="submit" disabled={guardando} className="btn btn-primary btn-round">
            {guardando ? 'Guardando...' : esEdicion ? 'Guardar cambios' : 'Registrar consulta'}
          </button>
        </div>
      </form>
    </div>
  )
}
