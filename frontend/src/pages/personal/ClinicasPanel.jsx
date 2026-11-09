import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Col, Row } from 'reactstrap'
import client from '../../api/client.js'
import { crud } from '../../api/administracion.js'
import { listarHospitalizacionesActivas } from '../../api/hospitalizaciones.js'
import { RUTAS } from '../../api/rutas.js'
import { useAuth } from '../../auth/useAuth.js'
import { BadgeActivo } from '../../components/Badge.jsx'
import { DialogoCatalogo } from '../../components/CatalogoCrud.jsx'
import ConfirmDialog from '../../components/ConfirmDialog.jsx'
import EmptyState from '../../components/EmptyState.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Table from '../../components/Table.jsx'
import TarjetaKpi from '../../components/TarjetaKpi.jsx'
import { mapaPor, useApi } from '../../hooks/useApi.js'
import { useOpciones } from '../../hooks/useOpciones.js'
import { normalizarError } from '../../utils/errores.js'

const claseSelect = 'form-control'
const activa = (c) => c.estadoClinica !== false

/**
 * Clínicas: indicadores, tarjetas por clínica (departamento, especialidad, ubicación y pacientes hospitalizados en ella)
 * y resumen por departamento. Alta, edición y desactivación con el mismo diálogo del catálogo (`config` de catalogos.js).
 */
export default function ClinicasPanel({ config, version = 0 }) {
  const { rol } = useAuth()
  const api = useMemo(() => crud(config.ruta), [config.ruta])
  const puedeEscribir = config.escribir.includes(rol)
  const puedeDesactivar = (config.desactivar ?? config.escribir).includes(rol)
  const opciones = useOpciones(config)

  const [texto, setTexto] = useState('')
  const [idDepartamento, setIdDepartamento] = useState('')
  const [estado, setEstado] = useState('activas')
  const [exito, setExito] = useState('')
  const [dialogo, setDialogo] = useState(null) // { fila } para editar (el alta está en el encabezado de la página)
  const [aDesactivar, setADesactivar] = useState(null)
  const [procesando, setProcesando] = useState(false)
  const [errorDesactivar, setErrorDesactivar] = useState('')

  const { datos, error, cargando, recargar } = useApi(`clinicas-panel|${version}`, async () => {
    const [clinicas, departamentos, especialidades, hospitalizaciones] = await Promise.all([
      api.listar(),
      client.get(RUTAS.departamentos).then((r) => r.data).catch(() => []),
      client.get(RUTAS.especialidadesClinica).then((r) => r.data).catch(() => []),
      listarHospitalizacionesActivas().catch(() => []),
    ])
    const internados = {}
    for (const h of hospitalizaciones) if (h.idClinica) internados[h.idClinica] = (internados[h.idClinica] ?? 0) + 1
    return {
      clinicas,
      departamentos,
      especialidades,
      internados,
      totalInternados: hospitalizaciones.filter((h) => h.idClinica).length,
      departamentosPorId: mapaPor(departamentos, 'idDepartamento'),
      especialidadesPorId: mapaPor(especialidades, 'idEspecialidad'),
    }
  })

  const clinicas = useMemo(() => datos?.clinicas ?? [], [datos])

  const filas = useMemo(() => {
    const busqueda = texto.trim().toLowerCase()
    return clinicas
      .filter((c) => estado === 'todas' || (estado === 'activas' ? activa(c) : !activa(c)))
      .filter((c) => !idDepartamento || String(c.idDepartamento) === idDepartamento)
      .filter((c) => {
        if (!busqueda) return true
        const depto = datos.departamentosPorId[c.idDepartamento]?.nombre ?? ''
        const esp = datos.especialidadesPorId[c.idEspecialidad]?.nombre ?? ''
        return `${c.nombre} ${c.ubicacion ?? ''} ${c.descripcion ?? ''} ${depto} ${esp}`.toLowerCase().includes(busqueda)
      })
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'))
  }, [clinicas, datos, texto, idDepartamento, estado])

  const desactivar = async () => {
    setProcesando(true)
    setErrorDesactivar('')
    try {
      await api.eliminar(aDesactivar.idClinica)
      setExito(`La clínica ${aDesactivar.nombre} fue desactivada.`)
      setADesactivar(null)
      recargar()
    } catch (e) {
      // 409/422: tiene registros activos asociados (p. ej. hospitalizaciones)
      setErrorDesactivar(normalizarError(e).message)
    } finally {
      setProcesando(false)
    }
  }

  const departamentos = datos?.departamentos ?? []
  const columnasDepartamentos = [
    { clave: 'nombre', titulo: 'Departamento', render: (d) => (
      <>
        <strong>{d.nombre}</strong>
        {d.descripcion && <div className="text-muted small">{d.descripcion}</div>}
      </>
    ) },
    { clave: 'telefono', titulo: 'Teléfono', render: (d) => d.telefono || '—' },
    { clave: 'clinicas', titulo: 'Clínicas activas', render: (d) => clinicas.filter((c) => c.idDepartamento === d.idDepartamento && activa(c)).length },
    { clave: 'estado', titulo: 'Estado', render: (d) => <BadgeActivo activo={d.estado} /> },
  ]

  return (
    <div>
      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />
      <ErrorAlert mensaje={error?.message} />

      <Row>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'business_bank' }} titulo={`Clínicas activas${datos ? ` de ${clinicas.length}` : ''}`} valor={datos ? clinicas.filter(activa).length : null} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'design_app' }} titulo="Departamentos activos" valor={datos ? departamentos.filter((d) => d.estado !== false).length : null} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'health_ambulance' }} titulo="Especialidades de clínica" valor={datos ? datos.especialidades.filter((e) => e.activo !== false).length : null} />
        </Col>
        <Col sm="6" xl="3">
          <TarjetaKpi kpi={{ icono: 'users_single-02' }} titulo="Pacientes hospitalizados" valor={datos ? datos.totalInternados : null} ruta="/hospitalizaciones" />
        </Col>
      </Row>

      <div className="d-flex flex-wrap align-items-center acciones-filtros mb-3">
        <input
          type="search"
          aria-label="Buscar clínica"
          className="form-control flex-grow-1 mr-2"
          placeholder="Buscar por nombre, ubicación, departamento o especialidad"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
        />
        <select aria-label="Filtrar por departamento" className={claseSelect} value={idDepartamento} onChange={(e) => setIdDepartamento(e.target.value)}>
          <option value="">Todos los departamentos</option>
          {departamentos.map((d) => (
            <option key={d.idDepartamento} value={d.idDepartamento}>
              {d.nombre}
            </option>
          ))}
        </select>
        <select aria-label="Filtrar por estado" className={claseSelect} value={estado} onChange={(e) => setEstado(e.target.value)}>
          <option value="activas">Activas</option>
          <option value="inactivas">Inactivas</option>
          <option value="todas">Todas</option>
        </select>
      </div>

      {cargando ? (
        <p className="text-muted">Cargando clínicas...</p>
      ) : filas.length === 0 ? (
        <EmptyState titulo="No hay clínicas" descripcion="Ajuste los filtros o registre una clínica." />
      ) : (
        <Row>
          {filas.map((c) => {
            const internados = datos.internados[c.idClinica] ?? 0
            return (
              <Col key={c.idClinica} md="6" xl="4" className="d-flex">
                <div className="card card-body w-100">
                  <div className="d-flex justify-content-between align-items-start">
                    <h3 className="h6 font-weight-bold mb-1">{c.nombre}</h3>
                    <BadgeActivo activo={activa(c)} />
                  </div>
                  {c.descripcion && <p className="text-muted small mb-2">{c.descripcion}</p>}
                  <ul className="lista-divisoria small mb-2 flex-grow-1">
                    <li className="d-flex justify-content-between">
                      <span className="text-muted">Departamento</span>
                      <span>{datos.departamentosPorId[c.idDepartamento]?.nombre ?? `#${c.idDepartamento}`}</span>
                    </li>
                    <li className="d-flex justify-content-between">
                      <span className="text-muted">Especialidad</span>
                      <span>{c.idEspecialidad ? (datos.especialidadesPorId[c.idEspecialidad]?.nombre ?? `#${c.idEspecialidad}`) : '—'}</span>
                    </li>
                    <li className="d-flex justify-content-between">
                      <span className="text-muted">Ubicación</span>
                      <span className="text-right">{c.ubicacion || '—'}</span>
                    </li>
                    <li className="d-flex justify-content-between">
                      <span className="text-muted">Teléfono</span>
                      <span>{c.telefono || '—'}</span>
                    </li>
                    <li className="d-flex justify-content-between">
                      <span className="text-muted">Hospitalizados</span>
                      <span className={internados ? 'font-weight-bold' : ''}>{internados}</span>
                    </li>
                  </ul>
                  {puedeEscribir && (
                    <div className="tabla-acciones d-flex justify-content-end">
                      <button type="button" className="btn btn-link text-primary p-0" onClick={() => setDialogo({ fila: c })}>
                        Editar
                      </button>
                      {puedeDesactivar && activa(c) && (
                        <button
                          type="button"
                          className="btn btn-link text-danger p-0"
                          onClick={() => {
                            setErrorDesactivar('')
                            setADesactivar(c)
                          }}
                        >
                          Desactivar
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </Col>
            )
          })}
        </Row>
      )}

      <h2 className="h5 mt-2 mb-3">Departamentos</h2>
      <Table
        columnas={columnasDepartamentos}
        filas={departamentos}
        claveFila={(d) => d.idDepartamento}
        cargando={cargando}
        vacio={<EmptyState titulo="No hay departamentos" descripcion="Regístrelos con «Nuevo departamento»." />}
      />
      <p className="small text-muted">
        Los departamentos se editan y desactivan en la pestaña <Link to="/clinicas?tab=departamentos">Departamentos</Link>.
      </p>

      {dialogo && (
        <DialogoCatalogo
          config={config}
          fila={dialogo.fila}
          opciones={opciones}
          protegida={false}
          onCerrar={() => setDialogo(null)}
          onListo={(mensaje) => {
            setDialogo(null)
            setExito(mensaje)
            recargar()
          }}
        />
      )}

      <ConfirmDialog
        abierto={!!aDesactivar}
        titulo="Desactivar clínica"
        mensaje={`¿Desea desactivar ${aDesactivar?.nombre ?? 'la clínica'}? No se puede si tiene registros activos asociados.`}
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
