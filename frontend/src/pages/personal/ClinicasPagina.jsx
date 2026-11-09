import { useState } from 'react'
import { useAuth } from '../../auth/useAuth.js'
import CatalogoCrud, { DialogoCatalogo } from '../../components/CatalogoCrud.jsx'
import ErrorAlert from '../../components/ErrorAlert.jsx'
import Pestanas from '../../components/Pestanas.jsx'
import { CATALOGOS_PERSONAL } from '../../config/catalogos.js'
import { useOpciones } from '../../hooks/useOpciones.js'
import ClinicasPanel from './ClinicasPanel.jsx'

const config = Object.fromEntries(CATALOGOS_PERSONAL.map((c) => [c.clave, c]))
const CLINICAS = config.clinicas
const DEPARTAMENTOS = config.departamentos
const ESPECIALIDADES = config['especialidades-clinica']

/**
 * Módulo Clínicas: clínicas y consultorios, departamentos y especialidades de clínica.
 * Las altas están arriba, en el encabezado; edición y desactivación, en cada pestaña.
 */
export default function ClinicasPagina() {
  const { rol } = useAuth()
  const [alta, setAlta] = useState(null) // config del catálogo que se está creando
  const [exito, setExito] = useState('')
  // Al crear algo desde el encabezado se vuelven a cargar las pestañas
  const [version, setVersion] = useState(0)
  const opciones = useOpciones(alta ?? CLINICAS)

  const boton = (c, texto, clase) =>
    c.escribir.includes(rol) && (
      <button type="button" className={`btn ${clase} btn-round`} onClick={() => setAlta(c)}>
        {texto}
      </button>
    )

  return (
    <div>
      <div className="card card-body d-flex flex-column flex-md-row align-items-md-center justify-content-md-between">
        <div>
          <h1 className="h3 mb-0">Clínicas</h1>
          <p className="text-muted mb-0">Clínicas y consultorios, departamentos y especialidades de clínica.</p>
        </div>
        <div className="acciones-grupo d-flex flex-wrap">
          {boton(DEPARTAMENTOS, 'Nuevo departamento', 'btn-outline-secondary')}
          {boton(ESPECIALIDADES, 'Nueva especialidad clínica', 'btn-outline-secondary')}
          {boton(CLINICAS, 'Nueva clínica', 'btn-primary')}
        </div>
      </div>

      <ErrorAlert tipo="exito" mensaje={exito} onCerrar={() => setExito('')} />

      <div className="card card-body pestanas-contenedor">
        <Pestanas
          etiqueta="Clínicas"
          pestanas={[
            { clave: 'clinicas', titulo: 'Clínicas', contenido: <ClinicasPanel key={version} config={CLINICAS} version={version} /> },
            { clave: DEPARTAMENTOS.clave, titulo: DEPARTAMENTOS.titulo, contenido: <CatalogoCrud key={version} config={DEPARTAMENTOS} /> },
            { clave: ESPECIALIDADES.clave, titulo: ESPECIALIDADES.titulo, contenido: <CatalogoCrud key={version} config={ESPECIALIDADES} /> },
          ]}
        />
      </div>

      {alta && (
        <DialogoCatalogo
          key={alta.clave}
          config={alta}
          fila={null}
          opciones={opciones}
          protegida={false}
          onCerrar={() => setAlta(null)}
          onListo={(mensaje) => {
            setAlta(null)
            setExito(mensaje)
            setVersion((v) => v + 1)
          }}
        />
      )}
    </div>
  )
}
