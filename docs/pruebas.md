# Pruebas

Estrategia y resultados de las pruebas automáticas del backend. Documentos relacionados: [README](../README.md), [arquitectura](arquitectura.md), [API](api.md).

## 1. Estrategia

La rúbrica pide pruebas unitarias con mocks, **al menos 5 con sentido por módulo**. Se probó la **capa de lógica (servicios)** con **JUnit 5 y Mockito**:

- Cada `*ServiceImpl` se prueba de forma aislada: los repositorios y servicios de los que depende se sustituyen por mocks (`@Mock`) y se inyectan con `@InjectMocks`. No se necesita base de datos, ni servidor, ni red.
- Se usa `MockitoExtension` con stubs estrictos: una prueba falla si declara un `when(...)` que nunca se usa, lo que evita pruebas con mocks sobrantes.
- Cada prueba es un caso de negocio con nombre descriptivo (`metodo_condicion_resultado`) y comprueba el resultado **y los efectos**: qué se guarda, qué no se guarda y qué excepción se lanza.
- Los casos se eligen según las convenciones de errores del proyecto:

| Tipo de caso | Qué comprueba | Excepción esperada | HTTP |
|---|---|---|---|
| Éxito | Datos guardados o devueltos correctamente, valores por defecto (activo, fechas), textos recortados. | ninguna | 200 / 201 / 204 |
| No encontrado | El recurso de la URL no existe. | `RecursoNoEncontradoException` | 404 |
| Regla de negocio violada | Referencia inexistente o inactiva, fechas incoherentes, transición no permitida, stock o saldo insuficiente. | `ReglaNegocioException` | 422 |
| Conflicto | Duplicados, o desactivar algo con dependientes activos. | `DuplicadoException`, `IllegalStateException` | 409 |
| Sin efectos al fallar | `verify(repositorio, never()).save(any())` tras el error. | (junto con los anteriores) | |

- Además hay pruebas de validación de DTO (Bean Validation, que produce los 400), del filtro JWT (usuario desactivado da 401) y del servicio de autenticación.

## 2. Cómo ejecutar las pruebas

Requisitos: JDK 21 o superior. No hace falta PostgreSQL ni ninguna variable de entorno.

```powershell
cd hospital
.\mvnw.cmd clean test          # Windows
```

```bash
cd hospital
./mvnw clean test              # Linux o macOS
```

Resultado esperado de la última ejecución completa (`clean test`):

```
Tests run: 407, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

El único omitido es la prueba de integración opcional (ver sección 5), que solo corre si existe la variable `DB_PASSWORD`. Los informes quedan en `hospital/target/surefire-reports/`.

Ejecutar solo una clase o un método:

```powershell
.\mvnw.cmd test -Dtest=PagosServiceImplTest
.\mvnw.cmd test -Dtest=PagosServiceImplTest#crear_lanza422_siElMontoSuperaElSaldoPendiente_yNoGuarda
```

## 3. Pruebas por módulo de la rúbrica

La rúbrica define 8 módulos. La tabla agrupa las clases de prueba por módulo; las pruebas de validación de DTO (`ValidacionDtosClinicosTest` y `ValidacionDtosAdministracionTest`) se reparten según el DTO que validan.

| Módulo | Pruebas | Clases de prueba que lo cubren |
|---|---:|---|
| Autenticación | 44 | `AuthServiceImplTest` (9), `UsuariosServiceImplTest` (18), `RolServiceImplTest` (5), `UsuarioDetailsServiceTest` (4), `RegisterRequestTest` (4), `JwtAuthenticationFilterTest` (3), validación del DTO de rol (1) |
| Administración | 32 | `MedicoServiceImplTest` (12), `MedicoEspecialidadesServiceImplTest` (5), `DependenciasYDefectosAdministracionTest` (9: departamentos, enfermeros, clínicas y especialidades de clínica), `OtrosCatalogosAdministracionTest` (2: especialidades médicas y turnos), validación de DTO de personal y catálogos (4) |
| Pacientes | 29 | `PacienteServiceImplTest` (15), `TipoSangreServiceImplTest` (7), `AlergiasServiceImplTest` (6), validación de DTO (1) |
| Clínica | 68 | `CitasServiceImplTest` (16), `DiagnosticosServiceImplTest` (10), `TratamientoServiceImplTest` (10), `DiagnosticoConsultaServiceImplTest` (9), `ConsultasServiceImplTest` (8), `TiposDiagnosticoServiceImplTest` (7), estados de cita en `OtrosCatalogosAdministracionTest` (3), validación de DTO clínicos (5) |
| Emergencias | 41 | `EmergenciaServiceImplTest` (18), `TriajeServiceImplTest` (9), `NivelesUrgenciaServiceImplTest` (6), `ProcedimientosEmergenciaServiceImplTest` (5), validación de DTO (3) |
| Hospitalización | 59 | `HospitalizacionesServiceImplTest` (21), `HabitacionServiceImplTest` (12), `TiposHabitacionServiceImplTest` (8), `AsignacionEnfermerosServiceImplTest` (7), `EstadosHospitalizacionServiceImplTest` (7), estado de habitación en `OtrosCatalogosAdministracionTest` (1), validación de DTO (3) |
| Farmacia | 25 | `MedicamentosServiceImplTest` (17), `TratamientoMedicamentoServiceImplTest` (6), validación de DTO (2) |
| Facturación | 81 | `FacturasServiceImplTest` (22), `CuentasServiceImplTest` (19), `PagosServiceImplTest` (17), `DetalleFacturaServiceImplTest` (10), `EstadosFacturaServiceImplTest` (8), método de pago en `DependenciasYDefectosAdministracionTest` (1), validación de DTO (4) |
| **Subtotal de los 8 módulos** | **379** | |
| Reportes (módulo transversal) | 7 | `ReportesServiceImplTest` (7) |
| Catálogo de estados (transversal) | 4 | `EstadosCatalogoTest` (4) |
| **Total de pruebas unitarias** | **390** | |
| Integración (omitida sin base de datos) | 1 | `HospitalApplicationTests` |
| **Total reportado por Maven** | **391** | 390 ejecutadas + 1 omitida |

Todos los módulos superan con holgura el mínimo de 5 pruebas con sentido que pide la rúbrica.

### Servicios con reglas de negocio y su cobertura

| Servicio | Casos cubiertos |
|---|---|
| `CitasServiceImpl` | Estado inicial válido (Programada o Confirmada, si no 422), fecha pasada (422), médico con cita activa a la misma hora (409), validación de transiciones de estado, 404 y borrado lógico. |
| `DiagnosticosServiceImpl` | Alta con tipo activo y valores por defecto, tipo inexistente o inactivo (422), 404 al buscar o actualizar, no desactivar si está en una consulta o tiene tratamientos (409), borrado lógico, filtros. |
| `TratamientoServiceImpl` | Alta válida, diagnóstico inexistente (404) o inactivo (422), fecha de fin anterior al inicio (422), 404, no eliminar con medicamentos asociados (422), borrado lógico. |
| `DiagnosticoConsultaServiceImpl` | Vínculo válido, consulta o diagnóstico inexistente o inactivo (422), par duplicado (409), ids nulos (422), 404 al buscar o quitar. |
| `TriajeServiceImpl` | Alta con nivel recortado, nivel duplicado (409), 404, no desactivar si hay emergencias activas (409), borrado lógico. |
| `HospitalizacionesServiceImpl` | Paciente con hospitalización activa (409), habitación sin cupo o en mantenimiento (422), alta que libera la habitación, no modificar una hospitalización finalizada. |
| `PagosServiceImpl` y `FacturasServiceImpl` | Pago parcial y total (la factura pasa a Pagada), monto mayor al saldo (422), factura no Pendiente (422), anulación de pagos, transiciones válidas del estado de factura, importes calculados por el servidor y no por el cliente. |
| `MedicamentosServiceImpl` | Ajuste de stock, stock negativo o insuficiente (422), medicamento inactivo. |
| Catálogos (`TiposDiagnostico`, `TiposHabitacion`, `EstadosFactura`, `EstadosHospitalizacion`, `NivelesUrgencia`, `MedicoEspecialidades`) | Alta con normalización de nombres base, duplicados (409), protección de valores base (no renombrar, no eliminar), no eliminar si hay registros que los usan, borrado lógico. |

## 4. Qué no cubren estas pruebas

- Las pruebas son unitarias: no ejercitan Spring, las anotaciones `@PreAuthorize`, el mapeo JPA ni las consultas reales de los repositorios.
- No hay pruebas automáticas de controladores (`MockMvc`) ni de la interfaz web; esas rutas se verificaron a mano con Swagger y con el frontend.
- Las 220 rutas y sus roles se documentan en [api.md](api.md), generadas a partir del código.

## 5. Prueba de integración opcional (con base de datos)

`HospitalApplicationTests` levanta el contexto completo de Spring Boot y necesita una base PostgreSQL creada con `hospital/Hosp_postgres.sql`. Para que `mvnw test` funcione en cualquier equipo, solo se ejecuta si existe la variable de entorno `DB_PASSWORD` (`@EnabledIfEnvironmentVariable`); sin ella aparece como **omitida**.

Para ejecutarla:

```powershell
$env:DB_PASSWORD = "<contraseña de PostgreSQL>"
# opcionales: DB_URL y DB_USER si no son los valores por defecto
cd hospital
.\mvnw.cmd test -Dtest=HospitalApplicationTests
```

```bash
export DB_PASSWORD='<contraseña de PostgreSQL>'
cd hospital && ./mvnw test -Dtest=HospitalApplicationTests
```

La prueba verifica que el contexto arranca: conexión a la base, entidades JPA, configuración de seguridad y `DataInitializer`. `JWT_SECRET` y `ADMIN_PASSWORD` no hacen falta para esta prueba: la clase fija valores propios solo de prueba.
