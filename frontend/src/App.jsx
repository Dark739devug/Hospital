import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import RoleRoute from './auth/RoleRoute.jsx'
import { obtenerModulo } from './config/menu.js'
import AuthLayout from './layouts/AuthLayout.jsx'
import MainLayout from './layouts/MainLayout.jsx'
import Loading from './components/Loading.jsx'
const Inicio = lazy(() => import('./pages/Inicio.jsx'))
const Login = lazy(() => import('./pages/Login.jsx'))
const CambiarPassword = lazy(() => import('./pages/CambiarPassword.jsx'))
const NoAutorizado = lazy(() => import('./pages/NoAutorizado.jsx'))
const NoEncontrado = lazy(() => import('./pages/NoEncontrado.jsx'))
const AgendaCitas = lazy(() => import('./pages/citas/AgendaCitas.jsx'))
const CitaDetalle = lazy(() => import('./pages/citas/CitaDetalle.jsx'))
const CitaForm = lazy(() => import('./pages/citas/CitaForm.jsx'))
const CitasLista = lazy(() => import('./pages/citas/CitasLista.jsx'))
const ConsultaDetalle = lazy(() => import('./pages/consultas/ConsultaDetalle.jsx'))
const ConsultaForm = lazy(() => import('./pages/consultas/ConsultaForm.jsx'))
const ConsultasLista = lazy(() => import('./pages/consultas/ConsultasLista.jsx'))
const DiagnosticoDetalle = lazy(() => import('./pages/diagnosticos/DiagnosticoDetalle.jsx'))
const DiagnosticoForm = lazy(() => import('./pages/diagnosticos/DiagnosticoForm.jsx'))
const DiagnosticosLista = lazy(() => import('./pages/diagnosticos/DiagnosticosLista.jsx'))
const TratamientoDetalle = lazy(() => import('./pages/tratamientos/TratamientoDetalle.jsx'))
const TratamientoForm = lazy(() => import('./pages/tratamientos/TratamientoForm.jsx'))
const TratamientosLista = lazy(() => import('./pages/tratamientos/TratamientosLista.jsx'))
const HabitacionDetalle = lazy(() => import('./pages/habitaciones/HabitacionDetalle.jsx'))
const HabitacionForm = lazy(() => import('./pages/habitaciones/HabitacionForm.jsx'))
const HabitacionesLista = lazy(() => import('./pages/habitaciones/HabitacionesLista.jsx'))
const HospitalizacionDetalle = lazy(() => import('./pages/hospitalizaciones/HospitalizacionDetalle.jsx'))
const HospitalizacionForm = lazy(() => import('./pages/hospitalizaciones/HospitalizacionForm.jsx'))
const HospitalizacionesLista = lazy(() => import('./pages/hospitalizaciones/HospitalizacionesLista.jsx'))
const MedicamentoDetalle = lazy(() => import('./pages/medicamentos/MedicamentoDetalle.jsx'))
const MedicamentoForm = lazy(() => import('./pages/medicamentos/MedicamentoForm.jsx'))
const MedicamentosLista = lazy(() => import('./pages/medicamentos/MedicamentosLista.jsx'))
const RecetasLista = lazy(() => import('./pages/recetas/RecetasLista.jsx'))
const CuentaDetalle = lazy(() => import('./pages/cuentas/CuentaDetalle.jsx'))
const CuentaForm = lazy(() => import('./pages/cuentas/CuentaForm.jsx'))
const CuentasLista = lazy(() => import('./pages/cuentas/CuentasLista.jsx'))
const FacturaDetalle = lazy(() => import('./pages/facturas/FacturaDetalle.jsx'))
const FacturaForm = lazy(() => import('./pages/facturas/FacturaForm.jsx'))
const FacturasLista = lazy(() => import('./pages/facturas/FacturasLista.jsx'))
const PagosLista = lazy(() => import('./pages/pagos/PagosLista.jsx'))
const CatalogosPagina = lazy(() => import('./pages/catalogos/CatalogosPagina.jsx'))
const MedicoDetalle = lazy(() => import('./pages/personal/MedicoDetalle.jsx'))
const MedicoForm = lazy(() => import('./pages/personal/MedicoForm.jsx'))
const PersonalPagina = lazy(() => import('./pages/personal/PersonalPagina.jsx'))
const ClinicasPagina = lazy(() => import('./pages/personal/ClinicasPagina.jsx'))
const UsuariosPagina = lazy(() => import('./pages/usuarios/UsuariosPagina.jsx'))
const EmergenciaDetalle = lazy(() => import('./pages/emergencias/EmergenciaDetalle.jsx'))
const EmergenciaForm = lazy(() => import('./pages/emergencias/EmergenciaForm.jsx'))
const EmergenciasLista = lazy(() => import('./pages/emergencias/EmergenciasLista.jsx'))
const ProcedimientoForm = lazy(() => import('./pages/procedimientos/ProcedimientoForm.jsx'))
const ProcedimientosLista = lazy(() => import('./pages/procedimientos/ProcedimientosLista.jsx'))
const PacienteDetalle = lazy(() => import('./pages/pacientes/PacienteDetalle.jsx'))
const PacienteForm = lazy(() => import('./pages/pacientes/PacienteForm.jsx'))
const PacientesLista = lazy(() => import('./pages/pacientes/PacientesLista.jsx'))

const permisosPacientes = obtenerModulo('pacientes').permisos
const permisosEmergencia = obtenerModulo('emergencia').permisos
const permisosMedicamentos = obtenerModulo('medicamentos').permisos
const permisosRecetas = obtenerModulo('recetas').permisos
const permisosHospitalizaciones = obtenerModulo('hospitalizaciones').permisos
const permisosHabitaciones = obtenerModulo('habitaciones').permisos
const permisosProcedimientos = obtenerModulo('procedimientos').permisos
const permisosCitas = obtenerModulo('citas').permisos
const permisosAgenda = obtenerModulo('agenda').permisos
const permisosConsultas = obtenerModulo('consultas').permisos
const permisosDiagnosticos = obtenerModulo('diagnosticos').permisos
const permisosTratamientos = obtenerModulo('tratamientos').permisos
const permisosFacturas = obtenerModulo('facturas').permisos
const permisosCuentas = obtenerModulo('cuentas').permisos
const permisosPagos = obtenerModulo('pagos').permisos
const permisosPersonal = obtenerModulo('personal').permisos
const permisosClinicas = obtenerModulo('clinicas').permisos
const permisosCatalogos = obtenerModulo('catalogos').permisos
const permisosUsuarios = obtenerModulo('usuarios').permisos

export default function App() {
  return (
    <Suspense fallback={<Loading pantallaCompleta texto="Cargando..." />}>
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/cambiar-password" element={<CambiarPassword />} />
        </Route>

        <Route element={<MainLayout />}>
          <Route index element={<Inicio />} />

          <Route element={<RoleRoute roles={permisosPacientes.ver} />}>
            <Route path="/pacientes" element={<PacientesLista />} />
            <Route path="/pacientes/:id" element={<PacienteDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosPacientes.crear} />}>
            <Route path="/pacientes/nuevo" element={<PacienteForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosPacientes.editar} />}>
            <Route path="/pacientes/:id/editar" element={<PacienteForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosAgenda.ver} />}>
            <Route path="/agenda" element={<AgendaCitas />} />
          </Route>
          <Route element={<RoleRoute roles={permisosCitas.ver} />}>
            <Route path="/citas" element={<CitasLista />} />
            <Route path="/citas/:id" element={<CitaDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosCitas.crear} />}>
            <Route path="/citas/nueva" element={<CitaForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosCitas.editar} />}>
            <Route path="/citas/:id/editar" element={<CitaForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosConsultas.ver} />}>
            <Route path="/consultas" element={<ConsultasLista />} />
            <Route path="/consultas/:id" element={<ConsultaDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosConsultas.crear} />}>
            <Route path="/consultas/nueva" element={<ConsultaForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosConsultas.editar} />}>
            <Route path="/consultas/:id/editar" element={<ConsultaForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosDiagnosticos.ver} />}>
            <Route path="/diagnosticos" element={<DiagnosticosLista />} />
            <Route path="/diagnosticos/:id" element={<DiagnosticoDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosDiagnosticos.crear} />}>
            <Route path="/diagnosticos/nuevo" element={<DiagnosticoForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosDiagnosticos.editar} />}>
            <Route path="/diagnosticos/:id/editar" element={<DiagnosticoForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosTratamientos.ver} />}>
            <Route path="/tratamientos" element={<TratamientosLista />} />
            <Route path="/tratamientos/:id" element={<TratamientoDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosTratamientos.crear} />}>
            <Route path="/tratamientos/nuevo" element={<TratamientoForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosTratamientos.editar} />}>
            <Route path="/tratamientos/:id/editar" element={<TratamientoForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosEmergencia.ver} />}>
            <Route path="/emergencia" element={<EmergenciasLista />} />
            <Route path="/emergencia/:id" element={<EmergenciaDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosEmergencia.crear} />}>
            <Route path="/emergencia/nueva" element={<EmergenciaForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosEmergencia.editar} />}>
            <Route path="/emergencia/:id/editar" element={<EmergenciaForm />} />
          </Route>

          {/* Los niveles de triaje se gestionan en Catálogos; se conserva la ruta antigua */}
          <Route path="/triaje/*" element={<Navigate to="/catalogos?tab=niveles-triaje" replace />} />

          <Route element={<RoleRoute roles={permisosProcedimientos.ver} />}>
            <Route path="/procedimientos" element={<ProcedimientosLista />} />
          </Route>
          <Route element={<RoleRoute roles={permisosProcedimientos.crear} />}>
            <Route path="/procedimientos/nuevo" element={<ProcedimientoForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosProcedimientos.editar} />}>
            <Route path="/procedimientos/:id/editar" element={<ProcedimientoForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosHospitalizaciones.ver} />}>
            <Route path="/hospitalizaciones" element={<HospitalizacionesLista />} />
            <Route path="/hospitalizaciones/:id" element={<HospitalizacionDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosHospitalizaciones.crear} />}>
            <Route path="/hospitalizaciones/nueva" element={<HospitalizacionForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosHospitalizaciones.editar} />}>
            <Route path="/hospitalizaciones/:id/editar" element={<HospitalizacionForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosHabitaciones.ver} />}>
            <Route path="/habitaciones" element={<HabitacionesLista />} />
            <Route path="/habitaciones/:id" element={<HabitacionDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosHabitaciones.crear} />}>
            <Route path="/habitaciones/nueva" element={<HabitacionForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosHabitaciones.editar} />}>
            <Route path="/habitaciones/:id/editar" element={<HabitacionForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosMedicamentos.ver} />}>
            <Route path="/medicamentos" element={<MedicamentosLista />} />
            <Route path="/medicamentos/:id" element={<MedicamentoDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosMedicamentos.crear} />}>
            <Route path="/medicamentos/nuevo" element={<MedicamentoForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosMedicamentos.editar} />}>
            <Route path="/medicamentos/:id/editar" element={<MedicamentoForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosRecetas.ver} />}>
            <Route path="/recetas" element={<RecetasLista />} />
          </Route>

          <Route element={<RoleRoute roles={permisosFacturas.ver} />}>
            <Route path="/facturas" element={<FacturasLista />} />
            <Route path="/facturas/:id" element={<FacturaDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosFacturas.crear} />}>
            <Route path="/facturas/nueva" element={<FacturaForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosFacturas.editar} />}>
            <Route path="/facturas/:id/editar" element={<FacturaForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosCuentas.ver} />}>
            <Route path="/cuentas" element={<CuentasLista />} />
            <Route path="/cuentas/:id" element={<CuentaDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosCuentas.crear} />}>
            <Route path="/cuentas/nueva" element={<CuentaForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosCuentas.editar} />}>
            <Route path="/cuentas/:id/editar" element={<CuentaForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosPagos.ver} />}>
            <Route path="/pagos" element={<PagosLista />} />
          </Route>

          <Route element={<RoleRoute roles={permisosPersonal.ver} />}>
            <Route path="/personal" element={<PersonalPagina />} />
          </Route>
          <Route element={<RoleRoute roles={permisosClinicas.ver} />}>
            <Route path="/clinicas" element={<ClinicasPagina />} />
            <Route path="/personal/medicos/:id" element={<MedicoDetalle />} />
          </Route>
          <Route element={<RoleRoute roles={permisosPersonal.crear} />}>
            <Route path="/personal/medicos/nuevo" element={<MedicoForm />} />
          </Route>
          <Route element={<RoleRoute roles={permisosPersonal.editar} />}>
            <Route path="/personal/medicos/:id/editar" element={<MedicoForm />} />
          </Route>

          <Route element={<RoleRoute roles={permisosCatalogos.ver} />}>
            <Route path="/catalogos" element={<CatalogosPagina />} />
          </Route>

          <Route element={<RoleRoute roles={permisosUsuarios.ver} />}>
            <Route path="/usuarios" element={<UsuariosPagina />} />
          </Route>

          <Route path="/no-autorizado" element={<NoAutorizado />} />
          <Route path="/inicio" element={<Navigate to="/" replace />} />
          <Route path="*" element={<NoEncontrado />} />
        </Route>
      </Route>
    </Routes>
    </Suspense>
  )
}
