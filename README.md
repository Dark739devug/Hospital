# API Hospital

Sistema de gestión hospitalaria: API REST (Spring Boot) con autenticación JWT, documentación Swagger y una aplicación web (React) que la consume. Proyecto final del curso **Arquitectura de Sistemas** (UMES).

[COMPLETAR: nombre del hospital, si aplica] · [COMPLETAR: sección, ciclo y catedrático]

## Qué es

Una API para gestionar un hospital privado en Guatemala. Atiende tres áreas asistenciales, apoyadas por farmacia y facturación:

- **Clínica (consulta externa):** pacientes, citas, consultas, diagnósticos y tratamientos.
- **Emergencias:** ingreso con triaje, procedimientos y alta.
- **Hospitalización:** habitaciones, ingreso, asignación de enfermeros y alta.
- **Farmacia:** medicamentos, stock y medicamentos de cada tratamiento.
- **Facturación:** facturas con detalle, cuentas y pagos (moneda GTQ, IVA 12 % configurable).

### Módulos

| Módulo | Contenido |
|---|---|
| Autenticación | Login con JWT, usuarios y roles. |
| Administración | Departamentos, clínicas, especialidades, médicos, enfermeros y turnos. |
| Pacientes | Expediente del paciente, alergias y tipos de sangre. |
| Clínica | Citas, consultas, diagnósticos, tratamientos. |
| Emergencias | Emergencias, niveles de triaje y procedimientos. |
| Hospitalización | Hospitalizaciones, habitaciones y asignación de enfermeros. |
| Farmacia | Medicamentos y medicamentos por tratamiento. |
| Facturación | Facturas, detalle, cuentas y pagos. |

Roles del sistema: `ADMIN`, `MEDICO`, `ENFERMERO`, `RECEPCIONISTA`, `FARMACEUTICO` y `CAJERO`. Son 220 endpoints REST, todos protegidos por token salvo el login.

## Versión publicada

| Parte | URL |
|---|---|
| Aplicación web (Render, Static Site) | https://hospital-web-lgmr.onrender.com |
| API (Render) | https://hospital-api-5jqk.onrender.com |
| Swagger UI | https://hospital-api-5jqk.onrender.com/swagger-ui.html |

Usuario de prueba: [COMPLETAR: usuario y contraseña de demostración, o "solicitarlo al equipo"]. El plan gratuito de Render duerme el servicio tras 15 minutos sin uso: la primera petición puede tardar alrededor de un minuto en responder.

## Tecnologías

| Parte | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation), Hibernate, Lombok |
| Autenticación | JWT con jjwt 0.12.6 (HS256) y contraseñas con BCrypt |
| Documentación | springdoc-openapi 3.1.0 (Swagger UI) |
| Base de datos | PostgreSQL (38 tablas) |
| Pruebas | JUnit 5 y Mockito (391 pruebas, 390 unitarias y 1 de integración opcional) |
| Frontend | Vite 8, React 19, React Router 7, axios, Bootstrap 4.6.2 y reactstrap 9.2.3 con el tema Now UI Dashboard |

## Estructura del repositorio

```
.
├── README.md             Este archivo
├── docs/                 Documentación del proyecto
│   ├── arquitectura.md   Capas, diagramas, seguridad, modelo de datos y decisiones
│   ├── api.md            Referencia de los endpoints, por módulo
│   ├── pruebas.md        Estrategia y resultados de las pruebas
│   └── openapi.json      Especificación OpenAPI exportada de la API
├── hospital/             Backend (Spring Boot)
│   ├── pom.xml
│   ├── mvnw, mvnw.cmd    Maven Wrapper
│   ├── Hosp_postgres.sql Esquema y datos iniciales de PostgreSQL
│   └── src/              Código (un paquete por módulo) y pruebas
└── frontend/             Aplicación web (Vite + React)
    └── README.md
```

## Requisitos

- **JDK 21 o superior** (no hace falta instalar Maven: se incluye el Maven Wrapper).
- **PostgreSQL** (cualquier versión reciente) con `psql` o pgAdmin.
- **Node.js 20.19 o superior** y npm, solo para el frontend.

## Puesta en marcha

### 1. Crear la base de datos

El esquema no lo genera Hibernate: se crea con el script del repositorio. Crea una base vacía llamada `Hospital` y ejecuta el script sobre ella:

```bash
psql -U postgres -c 'CREATE DATABASE "Hospital"'
psql -U postgres -d Hospital -f hospital/Hosp_postgres.sql
```

Con pgAdmin: crea la base `Hospital`, abre la herramienta de consultas sobre ella y ejecuta el contenido de `hospital/Hosp_postgres.sql`. El script crea las 38 tablas, sus índices y los datos iniciales de los catálogos (tipos de sangre, estados, turnos, niveles de urgencia, etc.).

### 2. Variables de entorno

El backend lee su configuración de variables de entorno (`hospital/src/main/resources/application.properties`):

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC completa de la base (si se define, tiene prioridad sobre las tres siguientes) | `jdbc:postgresql://localhost:5432/Hospital` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | La conexión por partes (así se configura en Render) | `localhost`, `5432`, `Hospital` |
| `PORT` | Puerto HTTP de la API (lo asigna Render) | `8080` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | **Obligatoria, sin valor por defecto** |
| `JWT_SECRET` | Secreto con que se firman los tokens (mínimo 32 caracteres) | **Obligatoria, sin valor por defecto** |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `3600000` (1 hora) |
| `CORS_ORIGINS` | Orígenes permitidos (separados por coma) | `http://localhost:5173` |
| `ADMIN_USER` | Nombre del usuario administrador inicial | `admin` |
| `ADMIN_PASSWORD` | Contraseña del administrador inicial (solo se usa al crearlo la primera vez) | **Obligatoria, sin valor por defecto** |
| `FACTURACION_IVA` | Tasa de IVA de las facturas | `0.12` |
| `PORT` | Puerto HTTP del backend (Render lo define solo) | `8080` |

PowerShell (Windows), en la misma terminal donde arrancarás el backend:

```powershell
$env:DB_PASSWORD = "<tu contraseña de PostgreSQL>"
$env:JWT_SECRET = "<un secreto largo y aleatorio>"
$env:ADMIN_PASSWORD = "<contraseña del administrador>"
# Opcionales
$env:DB_URL = "jdbc:postgresql://localhost:5432/Hospital"
$env:DB_USER = "postgres"
```

Bash (Linux o macOS):

```bash
export DB_PASSWORD='<tu contraseña de PostgreSQL>'
export JWT_SECRET='<un secreto largo y aleatorio>'
export ADMIN_PASSWORD='<contraseña del administrador>'
# Opcionales
export DB_URL='jdbc:postgresql://localhost:5432/Hospital'
export DB_USER='postgres'
```

En lugar de definirlas en cada terminal, puedes copiar `hospital/.env.example` como `hospital/.env` y completarlo: el backend lo lee al arrancar desde la carpeta `hospital` (por ejemplo, desde IntelliJ). Las variables de entorno tienen prioridad sobre el archivo.

No subas contraseñas al repositorio: el archivo `.gitignore` excluye los `.env`.

### 3. Levantar el backend

```powershell
cd hospital
.\mvnw.cmd spring-boot:run
```

```bash
cd hospital
./mvnw spring-boot:run
```

La API queda en **http://localhost:8080**. Al arrancar por primera vez, `DataInitializer` crea los 6 roles y el **usuario administrador inicial**: el nombre es `ADMIN_USER` (`admin` por defecto) y la contraseña la defines con `ADMIN_PASSWORD`. Sin `DB_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD` la aplicación no arranca. Si el administrador ya existe, `DataInitializer` no cambia su contraseña: se cambia desde la pantalla de usuarios. Los demás usuarios los crea el ADMIN (`POST /api/auth/register`, o desde la pantalla de usuarios del frontend).

### 4. Levantar el frontend

```bash
cd frontend
npm install
cp .env.example .env        # PowerShell: Copy-Item .env.example .env
npm run dev
```

La aplicación queda en **http://localhost:5173** (el puerto es fijo porque es el único origen que permite el CORS por defecto). Entra con el usuario administrador. Detalles de la interfaz en [frontend/README.md](frontend/README.md).

## Documentación Swagger

Con el backend en marcha abre **http://localhost:8080/swagger-ui.html**. Para probar los endpoints protegidos:

1. Ejecuta `POST /api/auth/login` con tu usuario y contraseña.
2. Copia el valor de `token` de la respuesta.
3. Pulsa el botón **Authorize** (candado), pega el token y confirma.
4. Ya puedes ejecutar cualquier endpoint con **Try it out**.

Cada endpoint indica en su descripción qué roles pueden usarlo. La especificación completa está en [docs/openapi.json](docs/openapi.json) (también en `http://localhost:8080/v3/api-docs`). Para actualizar el archivo con el backend en marcha (en PowerShell escribir `curl.exe`):

```bash
curl -s http://localhost:8080/v3/api-docs -o docs/openapi.json
```

## Probar los endpoints con curl

**1. Iniciar sesión y obtener el token**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}'
```

Respuesta: `{"token":"<JWT>","type":"Bearer"}`.

**2. Llamar a un endpoint protegido con el token**

```bash
curl -s http://localhost:8080/api/auth/me -H "Authorization: Bearer <JWT>"
curl -s http://localhost:8080/api/diagnosticos -H "Authorization: Bearer <JWT>"
```

En PowerShell:

```powershell
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body (@{ username = "admin"; password = $env:ADMIN_PASSWORD } | ConvertTo-Json)
Invoke-RestMethod -Uri http://localhost:8080/api/auth/me -Headers @{ Authorization = "Bearer $($r.token)" }
```

**3. Qué esperar sin token o sin permiso**

```bash
curl -i http://localhost:8080/api/diagnosticos                       # 401: falta el token
curl -i http://localhost:8080/api/usuarios -H "Authorization: Bearer <JWT de un rol sin permiso>"   # 403
```

Los errores tienen siempre el mismo formato JSON (`timestamp`, `status`, `error`, `message`, `path` y `detalles`); los códigos usados son 400, 401, 403, 404, 409 y 422. Ver [docs/api.md](docs/api.md).

## Ejecutar las pruebas

```powershell
cd hospital
.\mvnw.cmd clean test
```

Son pruebas unitarias con Mockito sobre la capa de servicios: no necesitan base de datos ni variables de entorno. La prueba de integración con PostgreSQL es opcional y solo corre si existe `DB_PASSWORD`. Estrategia, conteo por módulo y detalles en [docs/pruebas.md](docs/pruebas.md).

## Documentación

| Documento | Contenido |
|---|---|
| [docs/arquitectura.md](docs/arquitectura.md) | Dominio, diagrama de capas, componentes, autenticación JWT, roles, errores, modelo de datos y diferencias con el diseño de la entrega 1. |
| [docs/api.md](docs/api.md) | Guía para terceros y tabla de endpoints por módulo (método, ruta, roles, respuestas). |
| [docs/pruebas.md](docs/pruebas.md) | Estrategia de pruebas y resultados. |
| [docs/openapi.json](docs/openapi.json) | Especificación OpenAPI 3 de la API. |
| [docs/despliegue-render.md](docs/despliegue-render.md) | Cómo publicar la base, la API y el frontend en Render. |
| [frontend/README.md](frontend/README.md) | Interfaz web: estructura, módulos por rol y decisiones. |

## Equipo

| Integrante | Carnet | Rol en el proyecto |
|---|---|---|
| [COMPLETAR] | [COMPLETAR] | [COMPLETAR] |
| [COMPLETAR] | [COMPLETAR] | [COMPLETAR] |

Curso: Arquitectura de Sistemas (UMES). Catedrático: [COMPLETAR]. Fecha de entrega: [COMPLETAR].

## Créditos

La interfaz usa el tema **Now UI Dashboard React** de [Creative Tim](https://www.creative-tim.com/product/now-ui-dashboard-react), licencia MIT (se conservan el aviso de copyright y el `LICENSE.md` de la plantilla en `frontend/src/assets/now-ui/`). Las pantallas y componentes son propios.

## Licencia

[COMPLETAR: licencia del proyecto, si aplica]
