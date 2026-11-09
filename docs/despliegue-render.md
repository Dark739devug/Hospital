# Despliegue en Render

Guía para publicar la API, la base de datos y el frontend en [Render](https://render.com). Se crean tres recursos, en este orden:

1. **Base de datos** PostgreSQL.
2. **Backend** (Web Service con Docker; Render no tiene un entorno Java nativo, por eso se usa `hospital/Dockerfile`).
3. **Frontend** (Static Site).

> **Antes de empezar, ten en cuenta el plan gratuito de Render**
> - La base de datos gratuita **expira a los 30 días** de crearla (luego hay 14 días de gracia antes de borrarla). Créala cerca de la fecha de entrega.
> - El backend gratuito **se duerme tras 15 minutos sin tráfico** y tarda cerca de un minuto en despertar. Antes de la demostración en vivo, abre la URL de Swagger un par de minutos antes.
> - El servicio gratuito tiene 512 MB de memoria; el `Dockerfile` ya limita la JVM para que quepa.

---

## 1. Base de datos

1. En el panel de Render: **New → Postgres**.
2. Nombre: `hospital-db` (el que quieras). Región: la misma que usarás para el backend. Plan: **Free**.
3. Al crearse, en la página de la base copia estos datos de **Connections**:
   - *Hostname* interno, *Port*, *Database*, *Username*, *Password* (para el backend).
   - *External Database URL* (para crear las tablas desde tu computadora).

### Crear las tablas

Desde la carpeta del repositorio, con `psql` instalado (viene con PostgreSQL):

```bash
psql "<External Database URL>" -f hospital/Hosp_postgres.sql
```

En Windows, si `psql` no está en el PATH: `"C:\Program Files\PostgreSQL\18\bin\psql.exe" "<External Database URL>" -f hospital\Hosp_postgres.sql`.

El script crea las 38 tablas y los catálogos iniciales. Los roles y el usuario `admin` los crea la API la primera vez que arranca.

### Actualizar una base que ya estaba desplegada

Si la base se creó con una versión anterior del script, ejecuta los scripts de `hospital/migraciones/` que falten, en orden:

```bash
psql "<External Database URL>" -f hospital/migraciones/002_password_temporal.sql
psql "<External Database URL>" -f hospital/migraciones/003_factura_consulta.sql
```

El `002` agrega a `usuarios` las columnas de la contraseña temporal y el `003` agrega a `facturas` la consulta que las originó. La API también agrega esas columnas sola al arrancar; si no puede, lo indica en los logs de Render.

---

## 2. Backend (API)

1. **New → Web Service** → conecta el repositorio de GitHub.
2. Configuración:

| Campo | Valor |
|---|---|
| Branch | `main` |
| Root Directory | `hospital` |
| Runtime / Language | **Docker** |
| Dockerfile Path | `./Dockerfile` (relativo a `hospital`) |
| Instance Type | Free |
| Health Check Path | `/v3/api-docs` |

3. **Environment Variables** (pestaña *Environment*):

| Variable | Valor |
|---|---|
| `DB_HOST` | *Hostname* interno de la base |
| `DB_PORT` | `5432` |
| `DB_NAME` | *Database* de la base |
| `DB_USER` | *Username* de la base |
| `DB_PASSWORD` | *Password* de la base |
| `JWT_SECRET` | Un texto aleatorio de al menos 32 caracteres (puedes usar *Generate*) |
| `ADMIN_PASSWORD` | Contraseña del usuario `admin` en producción (no uses la de desarrollo) |
| `CORS_ORIGINS` | URL del frontend, por ejemplo `https://hospital-web.onrender.com` (la tendrás en el paso 3; vuelve a editarla después) |

`PORT` no hace falta: Render la define y el backend la usa (`server.port=${PORT:8080}`).

4. **Create Web Service**. El primer despliegue tarda varios minutos (descarga dependencias y compila). Cuando termine, la API queda en `https://<nombre>.onrender.com`.

### Comprobar

- `https://<nombre>.onrender.com/swagger-ui.html` → Swagger UI.
- `POST /api/auth/login` con `admin` y tu `ADMIN_PASSWORD` → devuelve el token.
- `GET /api/auth/me` sin token → **401**.

---

## 3. Frontend

1. **New → Static Site** → el mismo repositorio.
2. Configuración:

| Campo | Valor |
|---|---|
| Branch | `main` |
| Root Directory | `frontend` |
| Build Command | `npm ci && npm run build` |
| Publish Directory | `dist` |

3. **Environment Variables**:

| Variable | Valor |
|---|---|
| `VITE_API_URL` | URL del backend, por ejemplo `https://hospital-api.onrender.com` (sin `/` al final) |
| `NODE_VERSION` | `22` |

`VITE_API_URL` se lee **al compilar**: si la cambias, vuelve a desplegar el sitio (*Manual Deploy → Clear build cache & deploy*).

4. **Redirects/Rewrites** (pestaña del sitio): agrega una regla para que las rutas de React funcionen al recargar la página:

| Source | Destination | Action |
|---|---|---|
| `/*` | `/index.html` | Rewrite |

5. Copia la URL del sitio y ponla en `CORS_ORIGINS` del backend (paso 2.3). Guarda: el backend se vuelve a desplegar solo.

### Comprobar

- Abre la URL del sitio, inicia sesión con `admin`.
- Recarga la página en una ruta interna (por ejemplo `/pacientes`): debe seguir funcionando (si da 404, falta la regla de *Rewrite*).
- Si el login falla con un error de red, revisa `CORS_ORIGINS` (debe ser exactamente la URL del frontend, con `https://` y sin `/` final) y `VITE_API_URL`.

---

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El backend no arranca: `password authentication failed` | `DB_USER`/`DB_PASSWORD` incorrectos o se usó el host externo en vez del interno. |
| El backend no arranca: `relation ... does not exist` | No se ejecutó `Hosp_postgres.sql` sobre la base de Render. |
| La primera petición tarda ~1 minuto | El servicio gratuito estaba dormido. |
| Swagger "Try it out" falla con *Failed to fetch* | Se abrió Swagger con `http://` en vez de `https://`. |
| El frontend muestra "Network Error" | `VITE_API_URL` mal escrita o `CORS_ORIGINS` sin la URL del frontend. |
| `/pacientes` da 404 al recargar | Falta la regla de *Rewrite* `/*` → `/index.html`. |
