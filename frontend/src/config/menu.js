// Permisos y menú por rol, derivados de los @PreAuthorize de cada controller del backend
// (hospital/src/main/java/com/example/hospital/<paquete>/*Controller.java).
//
// IMPORTANTE: muchos endpoints GET no tienen @PreAuthorize, así que los puede consultar
// cualquier usuario autenticado (constante TODOS). La escritura suele ser más restrictiva.
// Cada módulo declara por operación: ver (listar/consultar), crear, editar, desactivar.
//
// `implementado: false` => el módulo existe en el backend pero su pantalla llega en una
// fase futura; no aparece en el menú todavía.

export const ADMIN = 'ADMIN'
export const MEDICO = 'MEDICO'
export const ENFERMERO = 'ENFERMERO'
export const RECEPCIONISTA = 'RECEPCIONISTA'
export const FARMACEUTICO = 'FARMACEUTICO'
export const CAJERO = 'CAJERO'

export const TODOS = [ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, FARMACEUTICO, CAJERO]
const CLINICOS = [ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA]
const SOLO_ADMIN = [ADMIN]

export const MODULOS = [
  {
    clave: 'pacientes',
    icono: 'users_single-02',
    titulo: 'Pacientes',
    ruta: '/pacientes',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: CLINICOS,
      crear: [ADMIN, RECEPCIONISTA],
      editar: [ADMIN, RECEPCIONISTA],
      desactivar: [ADMIN],
      // GET /api/reportes/paciente/{id}/historial
      historial: [ADMIN, MEDICO, ENFERMERO],
    },
  },
  {
    clave: 'citas',
    icono: 'ui-1_calendar-60',
    titulo: 'Citas',
    ruta: '/citas',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: TODOS,
      crear: [ADMIN, RECEPCIONISTA],
      editar: [ADMIN, RECEPCIONISTA],
      desactivar: SOLO_ADMIN,
      cambiarEstado: [ADMIN, RECEPCIONISTA, MEDICO],
    },
  },
  {
    clave: 'agenda',
    icono: 'ui-2_time-alarm',
    titulo: 'Agenda',
    ruta: '/agenda',
    grupo: 'Atención',
    implementado: true,
    // GET /api/citas/pendientes está abierto a cualquier usuario autenticado, pero la agenda necesita los nombres de
    // pacientes y médicos (que solo leen los roles clínicos), así que se ofrece a ellos.
    // Confirmar una cita usa PATCH /api/citas/{id}/estado/{id}: ADMIN, RECEPCIONISTA, MEDICO.
    permisos: { ver: CLINICOS, confirmar: [ADMIN, RECEPCIONISTA, MEDICO] },
  },
  {
    clave: 'consultas',
    icono: 'files_paper',
    titulo: 'Consultas',
    ruta: '/consultas',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: TODOS,
      crear: [ADMIN, MEDICO],
      editar: [ADMIN, MEDICO],
      desactivar: SOLO_ADMIN,
      // /api/diagnostico-consulta: asociar y quitar diagnósticos de una consulta
      asociarDiagnostico: [ADMIN, MEDICO],
    },
  },
  {
    clave: 'diagnosticos',
    icono: 'education_glasses',
    titulo: 'Diagnósticos',
    ruta: '/diagnosticos',
    grupo: 'Atención',
    implementado: true,
    permisos: { ver: TODOS, crear: [ADMIN, MEDICO], editar: [ADMIN, MEDICO], desactivar: SOLO_ADMIN },
  },
  {
    clave: 'tratamientos',
    icono: 'objects_support-17',
    titulo: 'Tratamientos',
    ruta: '/tratamientos',
    grupo: 'Atención',
    implementado: true,
    permisos: { ver: TODOS, crear: [ADMIN, MEDICO], editar: [ADMIN, MEDICO], desactivar: SOLO_ADMIN },
  },
  {
    clave: 'emergencia',
    icono: 'health_ambulance',
    titulo: 'Emergencia',
    ruta: '/emergencia',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: TODOS,
      crear: [ADMIN, MEDICO, ENFERMERO],
      editar: [ADMIN, MEDICO, ENFERMERO],
      desactivar: SOLO_ADMIN,
    },
  },
  {
    clave: 'triaje',
    icono: 'ui-1_bell-53',
    titulo: 'Niveles de triaje',
    ruta: '/triaje',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: CLINICOS,
      crear: [ADMIN, MEDICO, ENFERMERO],
      editar: [ADMIN, MEDICO, ENFERMERO],
      desactivar: SOLO_ADMIN,
    },
  },
  {
    clave: 'procedimientos',
    icono: 'design_scissors',
    titulo: 'Procedimientos de emergencia',
    ruta: '/procedimientos',
    grupo: 'Atención',
    implementado: true,
    // /api/procedimientos-emergencia: lectura ADMIN/MEDICO/ENFERMERO; escritura ADMIN/MEDICO; desactivar ADMIN
    permisos: {
      ver: [ADMIN, MEDICO, ENFERMERO],
      crear: [ADMIN, MEDICO],
      editar: [ADMIN, MEDICO],
      desactivar: SOLO_ADMIN,
    },
  },
  {
    clave: 'hospitalizaciones',
    icono: 'business_bank',
    titulo: 'Hospitalizaciones',
    ruta: '/hospitalizaciones',
    grupo: 'Atención',
    implementado: true,
    permisos: {
      ver: CLINICOS,
      crear: [ADMIN, MEDICO, RECEPCIONISTA],
      editar: [ADMIN, MEDICO],
      desactivar: SOLO_ADMIN,
      // POST /api/hospitalizaciones/{id}/alta
      darAlta: [ADMIN, MEDICO],
      // /api/asignacion-enfermeros: lectura abierta, escritura solo ADMIN
      asignarEnfermeros: SOLO_ADMIN,
    },
  },
  {
    clave: 'habitaciones',
    icono: 'shopping_shop',
    titulo: 'Habitaciones',
    ruta: '/habitaciones',
    grupo: 'Atención',
    implementado: true,
    // PATCH /estado/{id} también es solo ADMIN
    permisos: { ver: CLINICOS, crear: SOLO_ADMIN, editar: SOLO_ADMIN, desactivar: SOLO_ADMIN, cambiarEstado: SOLO_ADMIN },
  },
  {
    clave: 'medicamentos',
    icono: 'shopping_box',
    titulo: 'Medicamentos',
    ruta: '/medicamentos',
    grupo: 'Farmacia',
    implementado: true,
    permisos: {
      ver: TODOS,
      crear: [ADMIN, FARMACEUTICO, MEDICO],
      editar: [ADMIN, FARMACEUTICO],
      desactivar: SOLO_ADMIN,
      // PATCH /api/medicamentos/{id}/stock
      ajustarStock: [ADMIN, FARMACEUTICO],
    },
  },
  {
    clave: 'recetas',
    icono: 'files_single-copy-04',
    titulo: 'Medicamentos recetados',
    ruta: '/recetas',
    grupo: 'Farmacia',
    implementado: true,
    // /api/tratamiento-medicamento: lectura ADMIN/MEDICO/FARMACEUTICO; asignar (crear) y quitar solo ADMIN/MEDICO.
    permisos: {
      ver: [ADMIN, MEDICO, FARMACEUTICO],
      crear: [ADMIN, MEDICO],
      editar: [],
      desactivar: [ADMIN, MEDICO],
    },
  },
  {
    clave: 'facturas',
    icono: 'files_single-copy-04',
    titulo: 'Facturas',
    ruta: '/facturas',
    grupo: 'Facturación',
    implementado: true,
    // /api/facturas: GET abierto a cualquier usuario autenticado, pero el saldo (GET /{id}/saldo) y los pagos solo los leen
    // ADMIN/CAJERO(/RECEPCIONISTA); por eso la pantalla se limita a los roles de caja y recepción.
    // POST: ADMIN/CAJERO/RECEPCIONISTA; PUT y PATCH estado: ADMIN/CAJERO; DELETE: ADMIN.
    // /api/detalle-factura: POST ADMIN/CAJERO/RECEPCIONISTA; PUT ADMIN/CAJERO; DELETE ADMIN.
    permisos: {
      ver: [ADMIN, CAJERO, RECEPCIONISTA],
      crear: [ADMIN, CAJERO, RECEPCIONISTA],
      editar: [ADMIN, CAJERO],
      desactivar: SOLO_ADMIN,
      cambiarEstado: [ADMIN, CAJERO],
      agregarLinea: [ADMIN, CAJERO, RECEPCIONISTA],
      editarLinea: [ADMIN, CAJERO],
      quitarLinea: SOLO_ADMIN,
    },
  },
  {
    clave: 'cuentas',
    icono: 'business_money-coins',
    titulo: 'Cuentas',
    ruta: '/cuentas',
    grupo: 'Facturación',
    implementado: true,
    // /api/cuentas: GET abierto; POST, PUT y PATCH ajustar-saldo: ADMIN/CAJERO; DELETE: ADMIN
    permisos: {
      ver: [ADMIN, CAJERO, RECEPCIONISTA],
      crear: [ADMIN, CAJERO],
      editar: [ADMIN, CAJERO],
      desactivar: SOLO_ADMIN,
      ajustarSaldo: [ADMIN, CAJERO],
    },
  },
  {
    clave: 'pagos',
    icono: 'shopping_credit-card',
    titulo: 'Pagos',
    ruta: '/pagos',
    grupo: 'Facturación',
    implementado: true,
    // /api/pagos: GET y POST ADMIN/CAJERO; PUT (corregir) y DELETE (anular) solo ADMIN
    permisos: { ver: [ADMIN, CAJERO], crear: [ADMIN, CAJERO], editar: SOLO_ADMIN, desactivar: SOLO_ADMIN },
  },
  {
    clave: 'personal',
    icono: 'users_circle-08',
    titulo: 'Personal médico',
    ruta: '/personal',
    grupo: 'Administración',
    implementado: true,
    // /api/medico y /api/turnos: lectura CLINICOS; enfermeros, departamentos, clínicas y especialidades: lectura abierta.
    // Escritura (crear, editar, desactivar, asignar especialidades): solo ADMIN.
    permisos: { ver: CLINICOS, crear: SOLO_ADMIN, editar: SOLO_ADMIN, desactivar: SOLO_ADMIN },
  },
  {
    clave: 'catalogos',
    icono: 'design_bullet-list-67',
    titulo: 'Catálogos',
    ruta: '/catalogos',
    grupo: 'Administración',
    implementado: true,
    // Catálogos (tipos, estados, niveles de urgencia, métodos de pago): lectura casi abierta, escritura ADMIN.
    // Alergias: escritura ADMIN/MEDICO (desactivar solo ADMIN). La pantalla ofrece a cada rol solo los catálogos que puede
    // modificar (ver `escribir` en config/catalogos.js); por eso `ver` es ADMIN y MEDICO.
    permisos: { ver: [ADMIN, MEDICO], crear: SOLO_ADMIN, editar: SOLO_ADMIN, desactivar: SOLO_ADMIN },
  },
  {
    clave: 'usuarios',
    icono: 'objects_key-25',
    titulo: 'Usuarios y roles',
    ruta: '/usuarios',
    grupo: 'Administración',
    implementado: true,
    permisos: { ver: SOLO_ADMIN, crear: SOLO_ADMIN, editar: SOLO_ADMIN, desactivar: SOLO_ADMIN },
  },
]

export const obtenerModulo = (clave) => MODULOS.find((m) => m.clave === clave)

/** El rol puede realizar la operación (ver/crear/editar/desactivar) en el módulo? */
export const puede = (rol, claveModulo, operacion = 'ver') =>
  !!rol && !!obtenerModulo(claveModulo)?.permisos[operacion]?.includes(rol)

/** Módulos implementados que el rol puede al menos listar, agrupados para el sidebar. */
export function menuParaRol(rol) {
  const visibles = MODULOS.filter((m) => m.implementado && m.permisos.ver.includes(rol))
  const grupos = []
  for (const modulo of visibles) {
    let grupo = grupos.find((g) => g.titulo === modulo.grupo)
    if (!grupo) {
      grupo = { titulo: modulo.grupo, items: [] }
      grupos.push(grupo)
    }
    grupo.items.push(modulo)
  }
  return grupos
}
