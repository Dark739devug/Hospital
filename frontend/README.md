# Hospital - Frontend web

Interfaz web del proyecto API Hospital (Arquitectura de Sistemas, UMES).
Stack: Vite + React 19 (JavaScript), React Router, axios, Bootstrap 4.6 y reactstrap 9 con el tema Now UI Dashboard (Creative Tim).

Este README cubre solo la interfaz. La visión general del proyecto, la creación de la base de datos, las variables de entorno del backend y cómo levantarlo están en el [README de la raíz](../README.md); la arquitectura, la API y las pruebas, en [`docs/`](../docs/arquitectura.md) ([API](../docs/api.md), [pruebas](../docs/pruebas.md)).

## Requisitos

- Node.js 20.19 o superior (probado con Node 24) y npm.
- El backend (`../hospital`, Spring Boot) **debe estar corriendo en http://localhost:8080** con su base de datos PostgreSQL (pasos en el [README de la raíz](../README.md#puesta-en-marcha)).

## Puesta en marcha

```bash
cd frontend
npm install
cp .env.example .env     # en Windows (PowerShell): Copy-Item .env.example .env
npm run dev
```

La aplicación queda en http://localhost:5173. El puerto está fijado (`strictPort`) porque es el único origen que permite el CORS del backend (`app.cors.allowed-origins`).

Para entrar se necesita un usuario del backend (el administrador inicial lo crea el backend al arrancar con las variables `ADMIN_USER` y `ADMIN_PASSWORD`; ver el [README de la raíz](../README.md#3-levantar-el-backend)).

## Variables de entorno

| Variable       | Descripción                     | Valor por defecto       |
| -------------- | ------------------------------- | ----------------------- |
| `VITE_API_URL` | URL base del backend            | `http://localhost:8080` |

Vite fija `VITE_API_URL` en el momento del `npm run build`: en Vercel se define en las variables de entorno del proyecto (Production) antes de desplegar, y si cambia hay que volver a desplegar. Sin ella, la compilación apunta a `http://localhost:8080`.

## Publicación (Vercel)

El proyecto de Vercel usa `frontend/` como carpeta raíz (preset Vite). `vercel.json` redirige todas las rutas a `index.html` para que React Router resuelva las URL al recargar la página (por ejemplo `/pacientes/5`). El dominio de Vercel debe estar en `CORS_ORIGINS` del backend.

## Scripts

- `npm run dev`: servidor de desarrollo.
- `npm run build`: compilación de producción en `dist/` (las pantallas se cargan por demanda; el paquete principal pesa unos 305 kB).
- `npm run lint`: análisis estático (oxlint).

## Módulos y roles

Los permisos de la interfaz salen de los `@PreAuthorize` del backend y viven en `src/config/menu.js` (cada módulo declara qué rol puede ver, crear, editar y desactivar). Las rutas usan `RoleRoute` con esos mismos permisos, y un 403 del backend se muestra como "Acceso no autorizado".

| Módulo (grupo del menú)                         | Quién lo usa (resumen)                                                        |
| ------------------------------------------------ | ----------------------------------------------------------------------------- |
| Inicio: indicadores del día y accesos            | Todos (los indicadores según rol; FARMACEUTICO solo ve el stock bajo)         |
| Pacientes (+ historial clínico en el detalle)    | ADMIN, MEDICO, ENFERMERO, RECEPCIONISTA (historial clínico: ADMIN, MEDICO, ENFERMERO; cuentas y facturas dentro del historial solo ADMIN) |
| Citas, consultas, diagnósticos, tratamientos     | Citas: personal clínico y recepción; consultas, diagnósticos y tratamientos: ADMIN, MEDICO, ENFERMERO; escritura según el rol clínico |
| Emergencia, triaje, procedimientos               | Personal clínico y recepción (procedimientos: ADMIN, MEDICO, ENFERMERO)       |
| Hospitalizaciones, habitaciones, enfermeros asignados | Personal clínico y recepción; la administración de habitaciones es de ADMIN |
| Farmacia: medicamentos y medicamentos recetados  | ADMIN, FARMACEUTICO, MEDICO (según operación)                                 |
| Facturación: facturas, cuentas, pagos            | ADMIN, CAJERO y RECEPCIONISTA (los pagos solo ADMIN y CAJERO)                 |
| Administración: personal médico, catálogos, usuarios y roles | Consulta para el personal clínico; la escritura es de ADMIN (alergias también MEDICO) |

## Estructura

```
src/
  api/          cliente axios, rutas centralizadas (rutas.js) y una función por endpoint
  auth/         AuthProvider, useAuth, ProtectedRoute, RoleRoute
  config/       menu.js (módulos y permisos) y catalogos.js (configuración del CRUD genérico de catálogos)
  layouts/      MainLayout (barra lateral + cabecera) y AuthLayout
  components/   Table, FormField, ErrorAlert, Loading, ConfirmDialog, Pagination, CatalogoCrud, PacienteSelector, Pestanas...
  hooks/        useApi y hooks de datos compartidos
  pages/        una carpeta por módulo (lista, detalle y formulario)
  utils/        errores del backend, formato de fechas y montos (en centavos)
```

Las pantallas se cargan con `React.lazy` y `Suspense` (el componente `Loading` hace de respaldo).

## Notas

- La sesión usa el JWT del backend (expira en 1 hora) guardado en `localStorage`. Cualquier respuesta 401 cierra la sesión y lleva a `/login`.
- Los errores del backend (`{ status, message, detalles }`) se normalizan en `src/utils/errores.js`: `detalles` se muestra bajo cada campo y `message` en una alerta.
- Las fechas del backend (`LocalDate`/`LocalDateTime`, sin zona) se formatean cortando el texto, sin `Date`, para evitar desfases de zona horaria.
- Los montos (quetzales) se calculan en centavos enteros (`src/utils/dinero.js`) para no arrastrar errores de punto flotante.

## Diseño y créditos

La interfaz usa el tema **Now UI Dashboard React** v1.5.2 de [Creative Tim](https://www.creative-tim.com/product/now-ui-dashboard-react) (licencia MIT, https://github.com/creativetimofficial/now-ui-dashboard-react/tree/1.5.2). De la plantilla solo se incluyen el CSS compilado, la tipografía de iconos Nucleo y una imagen de fondo en `src/assets/now-ui/`, junto con su `LICENSE.md` (el aviso de copyright también está en la cabecera del CSS). Las pantallas y componentes son propios, escritos con `bootstrap` 4.6.2 y `reactstrap` 9.2.3. Las gráficas son barras HTML/CSS (sin librería de gráficos).

## Decisiones de diseño y pendientes del backend

Decisiones:

- La interfaz se adelanta con validaciones en cliente (campos obligatorios, tamaños, montos) pero el backend es la fuente de verdad: siempre se muestra su `message` o sus `detalles`.
- Filtros: se envían al servidor siempre que el backend los combina; la búsqueda por texto se resuelve en el cliente cuando el backend no la ofrece.
- Los roles son fijos del sistema y se muestran en solo lectura; los catálogos protegen los valores base (estados de cita, factura, habitación, hospitalización, tipos base).
- Caja (CAJERO) no puede leer pacientes: usa `PacienteSelector` con los endpoints mínimos `/api/paciente/busqueda` y `/{id}/resumen`.
- Tipografía: Montserrat se carga desde Google Fonts (sin conexión se usa la fuente del sistema).

Pendientes:

- No hay renovación del token: un token vigente de un usuario desactivado deja de funcionar (401), pero al expirar hay que volver a entrar.
- Alojar localmente los archivos woff2 de Montserrat (hoy se cargan desde Google Fonts).
- Faltan pruebas automáticas de interfaz (solo se probó a mano y con las pruebas unitarias del backend).
