import CatalogoCrud from '../../components/CatalogoCrud.jsx'
import Pestanas from '../../components/Pestanas.jsx'
import { CATALOGOS_PERSONAL } from '../../config/catalogos.js'
import MedicosLista from './MedicosLista.jsx'

/**
 * Personal médico y estructura: médicos (con su propio detalle y formulario), enfermeros, turnos, departamentos, clínicas y
 * especialidades. Todos los roles clínicos pueden consultar; solo ADMIN crea, edita y desactiva.
 */
const MODULO_CLINICAS = ['clinicas', 'departamentos', 'especialidades-clinica']

export default function PersonalPagina() {
  return (
    <div>
      <div className="card card-body">
        <h1 className="h3 mb-0">Personal médico</h1>
        <p className="text-muted mb-0">Médicos, enfermeros, turnos y especialidades médicas.</p>
      </div>
      <div className="card card-body pestanas-contenedor">
        <Pestanas
          etiqueta="Personal"
          pestanas={[
            { clave: 'medicos', titulo: 'Médicos', contenido: <MedicosLista /> },
            // Clínicas, departamentos y especialidades de clínica están en el módulo Clínicas (/clinicas)
            ...CATALOGOS_PERSONAL.filter((c) => !MODULO_CLINICAS.includes(c.clave)).map((c) => ({ clave: c.clave, titulo: c.titulo, contenido: <CatalogoCrud config={c} /> })),
          ]}
        />
      </div>
    </div>
  )
}
