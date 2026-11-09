# Documentación de la API REST

API del sistema de gestión hospitalaria (Spring Boot). Esta guía está pensada para quien vaya a consumir la API desde otro cliente (otro frontend, scripts, Postman, etc.). La referencia interactiva y siempre actualizada es **Swagger UI**; este documento resume lo mismo y se generó leyendo los controladores reales (`@RequestMapping`, `@GetMapping`/`@PostMapping`/..., `@PreAuthorize` y `@Operation`).

- **Total de endpoints documentados: 220** (98 GET, 42 POST, 35 PUT, 7 PATCH y 38 DELETE) repartidos en 40 controladores.
- Formato: JSON (`Content-Type: application/json`), codificación UTF-8.
- Dirección base en desarrollo: `http://localhost:8080`. En producción: `https://hospital-api-5jqk.onrender.com`.
- Rutas: todas cuelgan de `/api/<recurso>`.

Documentos relacionados: [README](../README.md), [arquitectura](arquitectura.md), [pruebas](pruebas.md).

## 1. Autenticación

Todos los endpoints requieren un token JWT, salvo `POST /api/auth/login` y la documentación Swagger. Sin token, o con un token inválido o vencido, la API responde **401**. Con un token válido pero sin el rol necesario responde **403**.

1. Iniciar sesión:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"<usuario>","password":"<contraseña>"}'
```

Respuesta `200`:

```json
{ "token": "<JWT>", "type": "Bearer" }
```

2. Enviar el token en cada petición protegida:

```bash
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer <JWT>"
```

Respuesta `200` de `/api/auth/me`: `{ "username": "...", "roles": ["ROLE_ADMIN"], "debeCambiarPassword": false, "fechaExpiracionPassword": null }`.

**Contraseña temporal.** Al registrar un médico o enfermero se le crea un usuario con contraseña temporal, que la respuesta de `POST /api/medico` y `POST /api/enfermeros` devuelve una sola vez en `credenciales` (`username`, `passwordTemporal`, `fechaExpiracion`). Vale 24 horas: el login y `/me` indican `debeCambiarPassword: true` y la fecha de vencimiento, y el usuario debe cambiarla con `POST /api/auth/cambiar-password`. Si vence, el login responde **422** y un administrador debe asignarle otra con `PUT /api/usuarios/{id}`; toda contraseña asignada por un administrador queda también como temporal por 24 horas.

Notas sobre el token:

- Algoritmo HS256, duración de **1 hora** por defecto (variable `JWT_EXPIRATION_MS`). No hay renovación: al vencer hay que iniciar sesión otra vez.
- Si un usuario se desactiva, su token deja de funcionar de inmediato (401), aunque no haya vencido.
- Las contraseñas se guardan con BCrypt. `POST /api/auth/register` (solo ADMIN) exige contraseña de 8 a 100 caracteres y el nombre del rol (`ADMIN`, `MEDICO`, `ENFERMERO`, `RECEPCIONISTA`, `FARMACEUTICO` o `CAJERO`).

## 2. Roles

| Rol | Uso típico |
|---|---|
| `ADMIN` | Administración total: usuarios, catálogos, personal y desactivación de registros. |
| `MEDICO` | Consultas, diagnósticos, tratamientos, emergencias, hospitalizaciones, recetas. |
| `ENFERMERO` | Emergencias, triaje y lectura clínica. |
| `RECEPCIONISTA` | Pacientes, citas, ingreso a emergencia, registro de hospitalizaciones y apoyo en caja. |
| `FARMACEUTICO` | Medicamentos y stock. |
| `CAJERO` | Facturas, detalle de factura, cuentas y pagos; consulta mínima de pacientes. |

La columna **Roles** de cada tabla sale del `@PreAuthorize` del método. "Autenticado" significa que no hay restricción de rol: sirve cualquier token válido (solo en las lecturas de catálogos y en `/api/auth/me`). La matriz completa por recurso está en [arquitectura.md](arquitectura.md#5-modelo-de-seguridad).

## 3. Formato de errores

Todos los errores tienen el mismo cuerpo JSON:

```json
{
  "timestamp": "2026-01-15T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "El diagnóstico con ID 5 se encuentra inactivo.",
  "path": "/api/tratamiento",
  "detalles": { "descripcion": "La descripción del tratamiento es obligatoria" }
}
```

`detalles` solo aparece en los errores de validación (400) y asocia cada campo con su mensaje. Los errores 401 y 403 generados por la capa de seguridad llevan los mismos campos, sin `detalles`.

| Código | Cuándo se usa |
|---|---|
| **200 / 201 / 204** | Éxito: lectura o actualización; creación; borrado lógico o desvinculación sin cuerpo. |
| **400** | Datos mal formados o que incumplen la validación: campos obligatorios, formatos, tamaños, rangos, JSON ilegible, parámetro con tipo incorrecto (por ejemplo un `id` que no es número). |
| **401** | Falta el token, es inválido, venció o el usuario está inactivo. |
| **403** | El rol del usuario no tiene permiso para esa operación. |
| **404** | El recurso indicado en la URL no existe. |
| **409** | Conflicto: duplicado (DPI, nombre de usuario, descripción de catálogo, par ya asignado) o la operación choca con el estado actual o con registros dependientes (por ejemplo desactivar algo que aún está en uso). También si la base de datos rechaza un duplicado que llegó al mismo tiempo que otro. |
| **422** | Se entendió la petición pero viola una regla de negocio: una referencia del cuerpo no existe o está inactiva, fechas incoherentes, stock insuficiente, transición de estado no permitida, etc. |
| **500** | Error inesperado del servidor (sin detalles internos en el mensaje). |

Convención: lo que se puede comprobar solo con el cuerpo (obligatorio, formato, tamaño) da 400; lo que depende del estado del sistema da 409 o 422; un id de la URL que no existe da 404, mientras que un id inexistente dentro del cuerpo da 422.

## 4. Convenciones generales

- **Borrado lógico:** los `DELETE` de entidades con historial no borran la fila, la dejan `activo = false` y responden `204`. Si el registro está en uso responden 409 o 422 con el motivo. Las asociaciones puras (por ejemplo médico-especialidad o diagnóstico-consulta) sí se eliminan físicamente.
- **Fechas:** `LocalDate` como `yyyy-MM-dd` (por ejemplo `2026-03-15`); `LocalDateTime` como `yyyy-MM-ddTHH:mm:ss`. No llevan zona horaria.
- **Montos:** números decimales con dos decimales, en quetzales (GTQ). El servidor calcula subtotal, IVA y total de las facturas a partir de las líneas de detalle (IVA configurable, 12 % por defecto); el cliente no los fija.
- **Listados:** no están paginados; devuelven arreglos JSON. Varios aceptan filtros opcionales por query string (se indican en la columna Descripción).
- **Identificadores compuestos:** las tablas de enlace usan dos ids en la ruta, por ejemplo `/api/diagnostico-consulta/{idDiagnostico}/{idConsulta}`.
- **CORS:** el navegador solo puede llamar desde los orígenes de la variable `CORS_ORIGINS` (por defecto `http://localhost:5173`).

## 5. Cómo leer las tablas de endpoints

- **Roles:** tomados del `@PreAuthorize`; "Autenticado" = cualquier usuario con token válido; "Público" = sin token.
- **Respuestas:** son los códigos declarados con `@ApiResponses` en cada método del controlador (el primero es el de éxito), más 401 en todo endpoint protegido y 403 donde hay restricción de rol. En Swagger UI cada código trae su descripción concreta (por ejemplo el motivo del 422) y el cuerpo de error `ErrorResponse` con ejemplo. Una escritura puede devolver 409 o 422 por reglas del módulo aunque el endpoint solo mencione el caso más habitual (ver [arquitectura.md](arquitectura.md#6-manejo-de-errores)).
- Los nombres entre comillas invertidas en la columna Descripción son parámetros de consulta (`?nombre=`) o el tipo del cuerpo (DTO) esperado. Sus campos, descripciones y ejemplos están en Swagger UI, sección **Schemas**; el ejemplo por defecto de cada cuerpo sirve para probar con **Try it out** (los ids de catálogos como `1` existen tras cargar `Hosp_postgres.sql`).

Operaciones con reglas de negocio destacadas:

| Operación | Regla principal |
|---|---|
| `PATCH /api/citas/{id}/estado/{nuevoIdEstado}` | Solo transiciones válidas entre estados de cita (Programada, Confirmada, Atendida, Cancelada...). |
| `POST /api/citas` | El médico no puede tener otra cita activa en la misma fecha y hora (409); la fecha no puede ser pasada. |
| `POST /api/hospitalizaciones` | El paciente no puede tener otra hospitalización activa (409); la habitación debe tener cupo y no estar en mantenimiento (422). |
| `POST /api/hospitalizaciones/{id}/alta` | Cierra la hospitalización y libera la habitación; solo si está Activa. |
| `PATCH /api/emergencia/{id}/dar-alta` | Registra la salida; 409 si ya tenía alta, 422 si es anterior al ingreso. |
| `POST /api/pagos` | Solo sobre facturas Pendientes; el monto no puede superar el saldo pendiente. |
| `PATCH /api/facturas/{id}/estado/{nuevoEstadoId}` | Pendiente a Pagada solo con saldo 0; Pendiente a Cancelada solo sin pagos activos; Pagada y Cancelada son terminales. |
| `PATCH /api/medicamentos/{id}/stock` | Suma o resta unidades; no permite stock negativo. |
| `PATCH /api/habitacion/{id}/estado/{nuevoEstadoId}` | El estado debe ser coherente con la ocupación real. |

# Endpoints por módulo

Los 8 módulos de la rúbrica son: Autenticación, Administración, Pacientes, Clínica, Emergencias, Hospitalización, Farmacia y Facturación. Los reportes (resumen e historial) se añaden como módulo transversal.

## Autenticación y usuarios (14 endpoints)

Login, registro, sesión actual, usuarios y roles.

### Autenticación (`/api/auth`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| POST | `/api/auth/login` | Público | Iniciar sesión. Cuerpo: `LoginRequest`. | 200, 400, 401 |
| POST | `/api/auth/register` | ADMIN | Registrar usuario. Cuerpo: `RegisterRequest`. | 200, 400, 401, 409, 422 |
| GET | `/api/auth/me` | Autenticado | Usuario actual. | 200, 401 |
| POST | `/api/auth/cambiar-password` | Autenticado | Cambiar la contraseña propia. Cuerpo: `CambiarPasswordRequest` (`passwordActual`, `nuevaPassword`). | 200, 400, 401, 422 |

### Usuarios (`/api/usuarios`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/usuarios` | ADMIN | Listar usuarios. | 200, 401, 403 |
| GET | `/api/usuarios/{id}` | ADMIN | Obtener usuario por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/usuarios` | ADMIN | Crear usuario. Cuerpo: `UsuariosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/usuarios/{id}` | ADMIN | Actualizar usuario. Cuerpo: `UsuariosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/usuarios/{id}` | ADMIN | Eliminar usuario. | 204, 400, 401, 403, 404, 422 |

### Roles (`/api/rol`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/rol` | ADMIN | Listar roles. | 200, 401, 403 |
| GET | `/api/rol/{id}` | ADMIN | Obtener rol por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/rol` | ADMIN | Registrar rol. Cuerpo: `RolDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/rol/{id}` | ADMIN | Actualizar rol. Cuerpo: `RolDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/rol/{id}` | ADMIN | Eliminar rol. | 204, 400, 401, 403, 404, 409, 422 |

## Administración (43 endpoints)

Estructura del hospital y personal: departamentos, clínicas, especialidades, médicos, enfermeros y turnos.

### Departamentos (`/api/departamentos`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/departamentos` | Autenticado | Listar departamentos. | 200, 401, 403 |
| GET | `/api/departamentos/{id}` | Autenticado | Obtener departamento por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/departamentos` | ADMIN | Registrar departamento. Cuerpo: `DepartamentosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/departamentos/{id}` | ADMIN | Actualizar departamento. Cuerpo: `DepartamentosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/departamentos/{id}` | ADMIN | Eliminar departamento. | 204, 400, 401, 403, 404, 409, 422 |

### Clínicas (`/api/clinicas`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/clinicas` | Autenticado | Listar clínicas. Parámetros: `soloActivas`. | 200, 400, 401, 403 |
| GET | `/api/clinicas/{id}` | Autenticado | Obtener clínica por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/clinicas` | ADMIN | Registrar clínica. Cuerpo: `ClinicasDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/clinicas/{id}` | ADMIN | Actualizar clínica. Cuerpo: `ClinicasDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/clinicas/{id}` | ADMIN | Eliminar clínica. | 204, 400, 401, 403, 404, 409, 422 |

### Especialidad de clínica (`/api/especialidad-clinica`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/especialidad-clinica` | Autenticado | Listar especialidades de clínica. Parámetros: `soloActivas`. | 200, 400, 401, 403 |
| GET | `/api/especialidad-clinica/{id}` | Autenticado | Obtener especialidad de clínica por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/especialidad-clinica` | ADMIN | Registrar especialidad de clínica. Cuerpo: `EspecialidadClinicaDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/especialidad-clinica/{id}` | ADMIN | Actualizar especialidad de clínica. Cuerpo: `EspecialidadClinicaDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/especialidad-clinica/{id}` | ADMIN | Eliminar especialidad de clínica. | 204, 400, 401, 403, 404, 409, 422 |

### Especialidades médicas (`/api/especialidad-medico`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/especialidad-medico` | Autenticado | Listar especialidades médicas. Parámetros: `soloActivas`. | 200, 400, 401, 403 |
| GET | `/api/especialidad-medico/{id}` | Autenticado | Obtener especialidad por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/especialidad-medico` | ADMIN | Registrar especialidad. Cuerpo: `EspecialidadMedicoDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/especialidad-medico/{id}` | ADMIN | Actualizar especialidad. Cuerpo: `EspecialidadMedicoDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/especialidad-medico/{id}` | ADMIN | Eliminar especialidad. | 204, 400, 401, 403, 404, 422 |

### Médicos (`/api/medico`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/medico` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar / buscar médicos. Parámetros: `soloActivos`, `criterio`. | 200, 400, 401, 403 |
| GET | `/api/medico/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener médico por ID. | 200, 400, 401, 403, 422 |
| GET | `/api/medico/colegiado/{noColegiado}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Buscar médico por colegiado. | 200, 401, 403, 422 |
| POST | `/api/medico` | ADMIN | Registrar médico y crear su usuario (la respuesta trae `credenciales`). Cuerpo: `MedicoDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/medico/{id}` | ADMIN | Actualizar médico. Cuerpo: `MedicoDTO`. | 200, 400, 401, 403, 409, 422 |
| PATCH | `/api/medico/{id}/estado` | ADMIN | Activar / desactivar médico. Parámetros: `nuevoEstado`. | 200, 400, 401, 403, 422 |
| DELETE | `/api/medico/{id}` | ADMIN | Eliminar médico. | 204, 400, 401, 403, 422 |

### Médico – especialidades (`/api/medico-especialidades`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/medico-especialidades` | Autenticado | Listar asignaciones. | 200, 401, 403 |
| GET | `/api/medico-especialidades/medico/{idMedico}` | Autenticado | Especialidades de un médico. | 200, 400, 401, 403, 422 |
| GET | `/api/medico-especialidades/especialidad/{idEspecialidad}` | Autenticado | Médicos de una especialidad. | 200, 400, 401, 403, 422 |
| GET | `/api/medico-especialidades/{idEspecialidad}/{idMedico}` | Autenticado | Obtener asignación. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/medico-especialidades` | ADMIN | Asignar especialidad a médico. Cuerpo: `MedicoEspecialidadesDTO`. | 201, 400, 401, 403, 409, 422 |
| DELETE | `/api/medico-especialidades/{idEspecialidad}/{idMedico}` | ADMIN | Quitar especialidad a médico. | 204, 400, 401, 403, 404, 422 |

### Enfermeros (`/api/enfermeros`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/enfermeros` | Autenticado | Listar enfermeros. Parámetros: `soloActivos`. | 200, 400, 401, 403 |
| GET | `/api/enfermeros/{id}` | Autenticado | Obtener enfermero por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/enfermeros` | ADMIN | Registrar enfermero y crear su usuario (la respuesta trae `credenciales`). Cuerpo: `EnfermerosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/enfermeros/{id}` | ADMIN | Actualizar enfermero. Cuerpo: `EnfermerosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/enfermeros/{id}` | ADMIN | Eliminar enfermero. | 204, 400, 401, 403, 404, 409, 422 |

### Turnos (`/api/turnos`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/turnos` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar turnos. | 200, 401, 403 |
| GET | `/api/turnos/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener turno por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/turnos` | ADMIN | Registrar turno. Cuerpo: `TurnosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/turnos/{id}` | ADMIN | Actualizar turno. Cuerpo: `TurnosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/turnos/{id}` | ADMIN | Eliminar turno. | 204, 400, 401, 403, 404, 422 |

## Pacientes (18 endpoints)

Expediente del paciente, alergias y tipos de sangre.

### Pacientes (`/api/paciente`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/paciente` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar / buscar pacientes. Parámetros: `todos`, `q`. | 200, 401, 403 |
| GET | `/api/paciente/busqueda` | ADMIN, CAJERO, RECEPCIONISTA | Búsqueda mínima de pacientes (caja). Parámetros: `q`. | 200, 401, 403 |
| GET | `/api/paciente/{id}/resumen` | ADMIN, CAJERO, RECEPCIONISTA | Resumen mínimo de un paciente (caja). | 200, 401, 403, 404 |
| GET | `/api/paciente/dpi/{dpi}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener paciente por DPI. | 200, 401, 403, 404 |
| GET | `/api/paciente/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener paciente por ID. | 200, 401, 403, 404 |
| POST | `/api/paciente` | ADMIN, RECEPCIONISTA | Registrar paciente. Cuerpo: `PacienteRequest`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/paciente/{id}` | ADMIN, RECEPCIONISTA | Actualizar paciente. Cuerpo: `PacienteRequest`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/paciente/{id}` | ADMIN | Desactivar paciente. | 204, 401, 403, 404, 422 |

### Alergias (`/api/alergias`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/alergias` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar alergias. | 200, 401, 403 |
| GET | `/api/alergias/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener alergia por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/alergias` | ADMIN, MEDICO | Registrar alergia. Cuerpo: `AlergiasDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/alergias/{id}` | ADMIN, MEDICO | Actualizar alergia. Cuerpo: `AlergiasDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/alergias/{id}` | ADMIN | Eliminar alergia. | 204, 400, 401, 403, 404, 409 |

### Tipos de sangre (`/api/tipo-sangre`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/tipo-sangre` | Autenticado | Listar tipos de sangre. | 200, 401, 403 |
| GET | `/api/tipo-sangre/{id}` | Autenticado | Obtener tipo de sangre por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/tipo-sangre` | ADMIN | Registrar tipo de sangre. Cuerpo: `TipoSangreDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/tipo-sangre/{id}` | ADMIN | Actualizar tipo de sangre. Cuerpo: `TipoSangreDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/tipo-sangre/{id}` | ADMIN | Eliminar tipo de sangre. | 204, 400, 401, 403, 404, 409, 422 |

## Clínica (consulta externa) (39 endpoints)

Citas, consultas, diagnósticos y tratamientos.

### Citas (`/api/citas`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/citas` | Autenticado | Listar citas (con filtros). Parámetros: `soloActivas`, `idPaciente`, `idMedico`, `fecha`. | 200, 400, 401, 403 |
| GET | `/api/citas/agenda` | Autenticado | Agenda diaria de un médico. Parámetros: `idMedico`, `fecha`. | 200, 400, 401, 403, 404, 422 |
| GET | `/api/citas/{id}` | Autenticado | Obtener cita por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/citas` | ADMIN, RECEPCIONISTA | Programar cita. Cuerpo: `CitasDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/citas/{id}` | ADMIN, RECEPCIONISTA | Reprogramar / actualizar cita. Cuerpo: `CitasDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| PATCH | `/api/citas/{id}/estado/{nuevoIdEstado}` | ADMIN, RECEPCIONISTA, MEDICO | Cambiar estado de la cita. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/citas/{id}` | ADMIN | Eliminar cita. | 204, 400, 401, 403, 404, 409, 422 |

### Estados de cita (`/api/estado-cita`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/estado-cita` | Autenticado | Listar estados de cita. | 200, 401, 403 |
| GET | `/api/estado-cita/{id}` | Autenticado | Obtener estado por ID. | 200, 400, 401, 403, 404 |
| GET | `/api/estado-cita/validar-transicion` | Autenticado | Validar cambio de estado. Parámetros: `origen`, `destino`. | 200, 401, 403 |
| POST | `/api/estado-cita` | ADMIN | Registrar estado de cita. Cuerpo: `EstadoCitaDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/estado-cita/{id}` | ADMIN | Actualizar estado de cita. Cuerpo: `EstadoCitaDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/estado-cita/{id}` | ADMIN | Eliminar estado de cita. | 204, 400, 401, 403, 404, 422 |

### Consultas (`/api/consultas`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/consultas` | Autenticado | Listar consultas. Parámetros: `soloActivas`, `idCita`. | 200, 400, 401, 403, 422 |
| GET | `/api/consultas/{id}` | Autenticado | Obtener consulta por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/consultas` | ADMIN, MEDICO | Registrar consulta. Cuerpo: `ConsultasDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/consultas/{id}` | ADMIN, MEDICO | Actualizar consulta (solo durante las 24 horas siguientes a la atención; la fecha/hora no cambia). Cuerpo: `ConsultasDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/consultas/{id}` | ADMIN | Eliminar consulta. | 204, 400, 401, 403, 404, 409, 422 |

### Diagnósticos (`/api/diagnosticos`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/diagnosticos` | Autenticado | Listar diagnósticos. Parámetros: `soloActivos`, `idTipoDiagnostico`, `fecha`. | 200, 400, 401, 403 |
| GET | `/api/diagnosticos/{id}` | Autenticado | Obtener diagnóstico por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/diagnosticos` | ADMIN, MEDICO | Registrar diagnóstico. Cuerpo: `DiagnosticosDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/diagnosticos/{id}` | ADMIN, MEDICO | Actualizar diagnóstico. Cuerpo: `DiagnosticosDTO`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/diagnosticos/{id}` | ADMIN | Eliminar diagnóstico. | 204, 400, 401, 403, 404, 409, 422 |

### Tipos de diagnóstico (`/api/tipos-diagnostico`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/tipos-diagnostico` | Autenticado | Listar tipos de diagnóstico. | 200, 401, 403 |
| GET | `/api/tipos-diagnostico/{id}` | Autenticado | Obtener tipo de diagnóstico por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/tipos-diagnostico` | ADMIN | Registrar tipo de diagnóstico. Cuerpo: `TiposDiagnosticoDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/tipos-diagnostico/{id}` | ADMIN | Actualizar tipo de diagnóstico. Cuerpo: `TiposDiagnosticoDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/tipos-diagnostico/{id}` | ADMIN | Eliminar tipo de diagnóstico. | 204, 400, 401, 403, 404, 409, 422 |

### Diagnóstico por consulta (`/api/diagnostico-consulta`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/diagnostico-consulta` | Autenticado | Listar relaciones. | 200, 401, 403 |
| GET | `/api/diagnostico-consulta/consulta/{idConsulta}` | Autenticado | Diagnósticos de una consulta. | 200, 400, 401, 403, 422 |
| GET | `/api/diagnostico-consulta/diagnostico/{idDiagnostico}` | Autenticado | Consultas con un diagnóstico. | 200, 400, 401, 403, 422 |
| GET | `/api/diagnostico-consulta/{idDiagnostico}/{idConsulta}` | Autenticado | Obtener relación. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/diagnostico-consulta` | ADMIN, MEDICO | Asociar diagnóstico a consulta (consulta de menos de 24 horas). Cuerpo: `DiagnosticoConsultaDTO`. | 201, 400, 401, 403, 409, 422 |
| DELETE | `/api/diagnostico-consulta/{idDiagnostico}/{idConsulta}` | ADMIN, MEDICO | Quitar diagnóstico de consulta (consulta de menos de 24 horas). | 204, 400, 401, 403, 404, 422 |

### Tratamientos (`/api/tratamiento`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/tratamiento` | Autenticado | Listar tratamientos. Parámetros: `soloActivos`, `idDiagnostico`. | 200, 400, 401, 403 |
| GET | `/api/tratamiento/{id}` | Autenticado | Obtener tratamiento por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/tratamiento` | ADMIN, MEDICO | Registrar tratamiento. Cuerpo: `TratamientoDTO`. | 201, 400, 401, 403, 404, 422 |
| PUT | `/api/tratamiento/{id}` | ADMIN, MEDICO | Actualizar tratamiento. Cuerpo: `TratamientoDTO`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/tratamiento/{id}` | ADMIN | Eliminar tratamiento. | 204, 400, 401, 403, 404, 422 |

## Emergencias (21 endpoints)

Ingresos a emergencia, triaje, niveles de urgencia y procedimientos.

### Emergencia (`/api/emergencia`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/emergencia` | Autenticado | Listar ingresos a emergencia. Parámetros: `soloActivas`, `idPaciente`. | 200, 400, 401, 403 |
| GET | `/api/emergencia/{id}` | Autenticado | Obtener ingreso por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/emergencia` | ADMIN, MEDICO, ENFERMERO | Registrar ingreso a emergencia. Cuerpo: `EmergenciaDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/emergencia/{id}` | ADMIN, MEDICO, ENFERMERO | Actualizar ingreso. Cuerpo: `EmergenciaDTO`. | 200, 400, 401, 403, 404, 422 |
| PATCH | `/api/emergencia/{id}/dar-alta` | ADMIN, MEDICO, ENFERMERO | Dar de alta de emergencia. Parámetros: `fechaHoraAlta`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/emergencia/{id}` | ADMIN | Eliminar ingreso. | 204, 400, 401, 403, 404, 409, 422 |

### Triaje (`/api/triaje`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/triaje` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar niveles de triaje. | 200, 401, 403 |
| GET | `/api/triaje/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener triaje por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/triaje` | ADMIN, MEDICO, ENFERMERO | Registrar nivel de triaje. Cuerpo: `TriajeDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/triaje/{id}` | ADMIN, MEDICO, ENFERMERO | Actualizar nivel de triaje. Cuerpo: `TriajeDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/triaje/{id}` | ADMIN | Eliminar nivel de triaje. | 204, 400, 401, 403, 404, 409, 422 |

### Niveles de urgencia (`/api/niveles-urgencia`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/niveles-urgencia` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar niveles de urgencia. | 200, 401, 403 |
| GET | `/api/niveles-urgencia/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener nivel por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/niveles-urgencia` | ADMIN | Registrar nivel de urgencia. Cuerpo: `NivelesUrgenciaDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/niveles-urgencia/{id}` | ADMIN | Actualizar nivel de urgencia. Cuerpo: `NivelesUrgenciaDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/niveles-urgencia/{id}` | ADMIN | Eliminar nivel de urgencia. | 204, 400, 401, 403, 404, 422 |

### Procedimientos de emergencia (`/api/procedimientos-emergencia`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/procedimientos-emergencia` | ADMIN, MEDICO, ENFERMERO | Listar procedimientos. | 200, 401, 403 |
| GET | `/api/procedimientos-emergencia/{id}` | ADMIN, MEDICO, ENFERMERO | Obtener procedimiento por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/procedimientos-emergencia` | ADMIN, MEDICO | Registrar procedimiento. Cuerpo: `ProcedimientosEmergenciaDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/procedimientos-emergencia/{id}` | ADMIN, MEDICO | Actualizar procedimiento. Cuerpo: `ProcedimientosEmergenciaDTO`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/procedimientos-emergencia/{id}` | ADMIN | Eliminar procedimiento. | 204, 400, 401, 403, 404, 422 |

## Hospitalización (38 endpoints)

Hospitalizaciones, habitaciones y asignación de enfermeros.

### Hospitalizaciones (`/api/hospitalizaciones`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/hospitalizaciones` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar hospitalizaciones. | 200, 401, 403 |
| GET | `/api/hospitalizaciones/activas` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Pacientes hospitalizados actualmente. | 200, 401, 403 |
| GET | `/api/hospitalizaciones/paciente/{idPaciente}/resumen` | ADMIN, CAJERO, RECEPCIONISTA | Resumen de hospitalizaciones de un paciente (caja). | 200, 400, 401, 403, 404 |
| GET | `/api/hospitalizaciones/paciente/{idPaciente}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Hospitalizaciones de un paciente. | 200, 400, 401, 403, 404 |
| GET | `/api/hospitalizaciones/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener hospitalización por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/hospitalizaciones` | ADMIN, MEDICO, RECEPCIONISTA | Ingresar paciente. Cuerpo: `HospitalizacionesDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/hospitalizaciones/{id}` | ADMIN, MEDICO | Actualizar hospitalización. Cuerpo: `HospitalizacionesDTO`. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/hospitalizaciones/{id}/alta` | ADMIN, MEDICO | Dar de alta. Parámetros: `fechaAlta`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/hospitalizaciones/{id}` | ADMIN | Eliminar hospitalización. | 204, 400, 401, 403, 404, 409 |

### Estados de hospitalización (`/api/estados-hospitalizacion`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/estados-hospitalizacion` | Autenticado | Listar estados de hospitalización. Parámetros: `soloActivos`. | 200, 400, 401, 403 |
| GET | `/api/estados-hospitalizacion/{id}` | Autenticado | Obtener estado por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/estados-hospitalizacion` | ADMIN | Registrar estado de hospitalización. Cuerpo: `EstadosHospitalizacionDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/estados-hospitalizacion/{id}` | ADMIN | Actualizar estado de hospitalización. Cuerpo: `EstadosHospitalizacionDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/estados-hospitalizacion/{id}` | ADMIN | Eliminar estado de hospitalización. | 204, 400, 401, 403, 404, 409, 422 |

### Asignación de enfermeros (`/api/asignacion-enfermeros`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/asignacion-enfermeros` | Autenticado | Listar asignaciones. | 200, 401, 403 |
| GET | `/api/asignacion-enfermeros/{id}` | Autenticado | Obtener asignación por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/asignacion-enfermeros` | ADMIN | Asignar enfermero. Cuerpo: `AsignacionEnfermerosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/asignacion-enfermeros/{id}` | ADMIN | Actualizar asignación. Cuerpo: `AsignacionEnfermerosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/asignacion-enfermeros/{id}` | ADMIN | Eliminar asignación. | 204, 400, 401, 403, 404, 422 |
| GET | `/api/asignacion-enfermeros/enfermero/{idEnfermero}` | Autenticado | Asignaciones de un enfermero. | 200, 400, 401, 403, 404 |
| GET | `/api/asignacion-enfermeros/hospitalizacion/{idHospitalizacion}` | Autenticado | Enfermeros de una hospitalización. Parámetros: `soloActivas`. | 200, 400, 401, 403, 404 |

### Habitaciones (`/api/habitacion`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/habitacion` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Listar habitaciones. Parámetros: `soloActivas`, `soloDisponibles`, `piso`, `idTipoHabitacion`, `idEstadoHabitacion`. | 200, 400, 401, 403 |
| GET | `/api/habitacion/{id}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Obtener habitación por ID. | 200, 400, 401, 403, 404 |
| GET | `/api/habitacion/numero/{numero}` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA | Buscar habitación por número. | 200, 401, 403, 404, 422 |
| POST | `/api/habitacion` | ADMIN | Registrar habitación. Cuerpo: `HabitacionDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/habitacion/{id}` | ADMIN | Actualizar habitación. Cuerpo: `HabitacionDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| PATCH | `/api/habitacion/{id}/estado/{nuevoEstadoId}` | ADMIN | Cambiar estado de habitación. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/habitacion/{id}` | ADMIN | Eliminar habitación. | 204, 400, 401, 403, 404, 422 |

### Tipos de habitación (`/api/tipos-habitacion`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/tipos-habitacion` | Autenticado | Listar tipos de habitación. | 200, 401, 403 |
| GET | `/api/tipos-habitacion/{id}` | Autenticado | Obtener tipo de habitación por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/tipos-habitacion` | ADMIN | Registrar tipo de habitación. Cuerpo: `TiposHabitacionDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/tipos-habitacion/{id}` | ADMIN | Actualizar tipo de habitación. Cuerpo: `TiposHabitacionDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/tipos-habitacion/{id}` | ADMIN | Eliminar tipo de habitación. | 204, 400, 401, 403, 404, 409, 422 |

### Estados de habitación (`/api/estado-habitacion`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/estado-habitacion` | Autenticado | Listar estados de habitación. | 200, 401, 403 |
| GET | `/api/estado-habitacion/{id}` | Autenticado | Obtener estado por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/estado-habitacion` | ADMIN | Registrar estado de habitación. Cuerpo: `EstadoHabitacionDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/estado-habitacion/{id}` | ADMIN | Actualizar estado de habitación. Cuerpo: `EstadoHabitacionDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/estado-habitacion/{id}` | ADMIN | Eliminar estado de habitación. | 204, 400, 401, 403, 404, 409, 422 |

## Farmacia (10 endpoints)

Medicamentos, stock y medicamentos recetados en los tratamientos.

### Medicamentos (`/api/medicamentos`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/medicamentos` | Autenticado | Listar / buscar medicamentos. Parámetros: `soloActivos`, `bajoStock`, `criterio`. | 200, 400, 401, 403 |
| GET | `/api/medicamentos/{id}` | Autenticado | Obtener medicamento por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/medicamentos` | ADMIN, FARMACEUTICO, MEDICO | Registrar medicamento. Cuerpo: `MedicamentosDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/medicamentos/{id}` | ADMIN, FARMACEUTICO | Actualizar medicamento. Cuerpo: `MedicamentosDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| PATCH | `/api/medicamentos/{id}/stock` | ADMIN, FARMACEUTICO | Ajustar stock. Parámetros: `cambio`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/medicamentos/{id}` | ADMIN | Eliminar medicamento. | 204, 400, 401, 403, 404, 409, 422 |

### Tratamiento – medicamentos (`/api/tratamiento-medicamento`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/tratamiento-medicamento` | ADMIN, MEDICO, FARMACEUTICO | Listar medicamentos recetados. Parámetros: `idTratamiento`, `idMedicamento`. | 200, 400, 401, 403 |
| GET | `/api/tratamiento-medicamento/{idMedicamento}/{idTratamiento}` | ADMIN, MEDICO, FARMACEUTICO | Obtener receta. | 200, 400, 401, 403, 404 |
| POST | `/api/tratamiento-medicamento` | ADMIN, MEDICO | Recetar medicamento. Cuerpo: `TratamientoMedicamentoDTO`. | 201, 400, 401, 403, 409, 422 |
| DELETE | `/api/tratamiento-medicamento/{idMedicamento}/{idTratamiento}` | ADMIN, MEDICO | Quitar medicamento del tratamiento. | 204, 400, 401, 403, 404 |

## Facturación (35 endpoints)

Facturas, líneas de detalle, cuentas, pagos y sus catálogos.

### Facturas (`/api/facturas`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/facturas` | Autenticado | Listar facturas. Parámetros: `soloActivas`, `idPaciente`, `idHospitalizacion`, `idEstadoFactura`, `desde`, `hasta`. | 200, 400, 401, 403, 422 |
| GET | `/api/facturas/{id}` | Autenticado | Obtener factura por ID. | 200, 400, 401, 403, 404 |
| GET | `/api/facturas/{id}/saldo` | ADMIN, CAJERO, RECEPCIONISTA | Saldo de una factura. | 200, 400, 401, 403, 404 |
| POST | `/api/facturas` | ADMIN, CAJERO, RECEPCIONISTA | Emitir factura. Cuerpo: `FacturasDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/facturas/{id}` | ADMIN, CAJERO | Actualizar factura. Cuerpo: `FacturasDTO`. | 200, 400, 401, 403, 404, 422 |
| PATCH | `/api/facturas/{id}/estado/{nuevoEstadoId}` | ADMIN, CAJERO | Cambiar estado de factura. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/facturas/{id}` | ADMIN | Anular factura. | 204, 400, 401, 403, 404, 409, 422 |

### Detalle de factura (`/api/detalle-factura`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/detalle-factura` | Autenticado | Listar líneas de factura. Parámetros: `idFactura`. | 200, 400, 401, 403, 422 |
| GET | `/api/detalle-factura/{id}` | Autenticado | Obtener línea por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/detalle-factura` | ADMIN, CAJERO, RECEPCIONISTA | Agregar línea a factura. Cuerpo: `DetalleFacturaDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/detalle-factura/{id}` | ADMIN, CAJERO | Actualizar línea. Cuerpo: `DetalleFacturaDTO`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/detalle-factura/{id}` | ADMIN | Eliminar línea. | 204, 400, 401, 403, 404, 422 |

### Cuentas (`/api/cuentas`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/cuentas` | Autenticado | Listar cuentas. Parámetros: `soloActivas`, `idPaciente`, `idHospitalizacion`, `estado`. | 200, 400, 401, 403, 422 |
| GET | `/api/cuentas/{id}` | Autenticado | Obtener cuenta por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/cuentas` | ADMIN, CAJERO | Abrir cuenta. Cuerpo: `CuentasDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/cuentas/{id}` | ADMIN, CAJERO | Actualizar cuenta. Cuerpo: `CuentasDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| PATCH | `/api/cuentas/{id}/ajustar-saldo` | ADMIN, CAJERO | Ajustar saldo. Parámetros: `monto`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/cuentas/{id}` | ADMIN | Cerrar / eliminar cuenta. | 204, 400, 401, 403, 404, 409, 422 |

### Pagos (`/api/pagos`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/pagos` | ADMIN, CAJERO | Listar pagos. | 200, 401, 403 |
| GET | `/api/pagos/{id}` | ADMIN, CAJERO | Obtener pago por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/pagos` | ADMIN, CAJERO | Registrar pago. Cuerpo: `PagosDTO`. | 201, 400, 401, 403, 422 |
| PUT | `/api/pagos/{id}` | ADMIN | Actualizar pago. Cuerpo: `PagosDTO`. | 200, 400, 401, 403, 404, 422 |
| DELETE | `/api/pagos/{id}` | ADMIN | Anular pago. | 204, 400, 401, 403, 404, 422 |
| GET | `/api/pagos/factura/{idFactura}` | ADMIN, CAJERO | Pagos de una factura. Parámetros: `soloActivos`. | 200, 400, 401, 403, 404 |
| GET | `/api/pagos/cuenta/{idCuenta}` | ADMIN, CAJERO | Pagos de una cuenta. Parámetros: `soloActivos`. | 200, 400, 401, 403, 404 |

### Estados de factura (`/api/estados-factura`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/estados-factura` | Autenticado | Listar estados de factura. Parámetros: `soloActivos`. | 200, 400, 401, 403 |
| GET | `/api/estados-factura/{id}` | Autenticado | Obtener estado por ID. | 200, 400, 401, 403, 404 |
| POST | `/api/estados-factura` | ADMIN | Registrar estado de factura. Cuerpo: `EstadosFacturaDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/estados-factura/{id}` | ADMIN | Actualizar estado de factura. Cuerpo: `EstadosFacturaDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/estados-factura/{id}` | ADMIN | Eliminar estado de factura. | 204, 400, 401, 403, 404, 422 |

### Métodos de pago (`/api/metodos-pago`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/metodos-pago` | ADMIN, CAJERO, RECEPCIONISTA | Listar métodos de pago. | 200, 401, 403 |
| GET | `/api/metodos-pago/{id}` | ADMIN, CAJERO, RECEPCIONISTA | Obtener método de pago por ID. | 200, 400, 401, 403, 404, 422 |
| POST | `/api/metodos-pago` | ADMIN | Registrar método de pago. Cuerpo: `MetodosPagoDTO`. | 201, 400, 401, 403, 409, 422 |
| PUT | `/api/metodos-pago/{id}` | ADMIN | Actualizar método de pago. Cuerpo: `MetodosPagoDTO`. | 200, 400, 401, 403, 404, 409, 422 |
| DELETE | `/api/metodos-pago/{id}` | ADMIN | Eliminar método de pago. | 204, 400, 401, 403, 404, 409, 422 |

## Reportes (2 endpoints)

Resumen del hospital (inicio) e historial clínico del paciente.

### Reportes (`/api/reportes`)

| Método | Ruta | Roles | Descripción | Respuestas |
|---|---|---|---|---|
| GET | `/api/reportes/resumen` | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA, CAJERO | Resumen del hospital (dashboard). | 200, 401, 403 |
| GET | `/api/reportes/paciente/{idPaciente}/historial` | ADMIN, MEDICO, ENFERMERO | Historial completo de un paciente. Parámetros: `incluirAnulados`. | 200, 400, 401, 403, 404, 422 |

## Documentación interactiva (Swagger / OpenAPI)

- **Swagger UI:** `http://localhost:8080/swagger-ui.html` en local o `https://hospital-api-5jqk.onrender.com/swagger-ui.html` en producción. Pulsa **Authorize**, pega el token obtenido en `POST /api/auth/login` y podrás probar cualquier endpoint desde el navegador.
- **Especificación OpenAPI 3 (JSON):** [`docs/openapi.json`](openapi.json), exportada desde `http://localhost:8080/v3/api-docs`. Sirve para importar la API en Postman, Insomnia o generadores de clientes.
- Cada endpoint tiene `@Operation` con su resumen y descripción, e indica los roles permitidos; cada método declara sus respuestas (éxito, 400, 404, 409 y 422 según corresponda) con descripción concreta y el esquema de error `ErrorResponse` con ejemplos; 401 y 403 se declaran en todos los endpoints protegidos.
