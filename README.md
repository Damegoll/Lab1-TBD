# Lab1-TBD

Aplicación full-stack compuesta por una API REST en Java 21 / Spring Boot 3.5, frontend Vue/Vite y PostgreSQL. El backend usa `JdbcTemplate` y no utiliza JPA, Hibernate ni otro ORM: los repositorios ejecutan SQL explícito. El código Java y sus archivos de construcción están agrupados en `backend/`; el frontend está en `frontend/`.

## Despliegue local con Docker Compose

Compose levanta PostgreSQL, la API y el frontend. En Windows, instala e inicia Docker Desktop con el motor de contenedores Linux y Docker Compose v2 habilitados. Espera a que Docker Desktop indique que el motor está en ejecución antes de ejecutar estos comandos desde PowerShell, en la raíz del proyecto:

```powershell
docker compose up --build -d
docker compose ps -a
docker compose logs --tail 100 api
docker compose logs --tail 100 frontend
```

La primera vez, confirma con `docker compose ps -a` que `db-init` terminó con código 0 y que `postgres`, `api` y `frontend` están iniciados. Compose aplica `schema.sql` y `triggers_y_procedures.sql` automáticamente. El frontend Vue/Vite se ejecuta en su propio contenedor. Los datos de demostración **no** se cargan solos; para agregarlos a la base ya iniciada, ejecuta desde la raíz del proyecto:

```powershell
Get-Content -Raw "backend\src\main\resources\SQLResources\datos_prueba.sql" |
  docker exec -i lab-tbd-postgres psql -U labtbd -d lab_tbd -v ON_ERROR_STOP=1
```

El frontend queda en `http://localhost:5173`, la API en `http://localhost:8080` y PostgreSQL queda disponible desde tu PC en `localhost:5433`. Para detener los contenedores sin borrar la base:

```powershell
docker compose down
```

Para borrar también los datos persistidos (acción destructiva):

```powershell
docker compose down -v
```

La primera ejecución construye las imágenes del backend y el frontend; puede tardar mientras Maven y npm descargan dependencias. El backend espera a que PostgreSQL esté saludable y el inicializador SQL termine antes de arrancar.

### Ejecutar solo el backend desde Maven (opcional)

Este modo es opcional; el uso recomendado para levantar toda la aplicación es Docker Compose. Con Maven 3.9+ instalado, se puede ejecutar solo la API fuera de Docker mientras PostgreSQL permanece en Compose:

```powershell
docker compose up -d postgres
docker compose run --rm db-init
Get-Content -Raw "backend\src\main\resources\SQLResources\datos_prueba.sql" |
  docker exec -i lab-tbd-postgres psql -U labtbd -d lab_tbd -v ON_ERROR_STOP=1
Set-Location backend
mvn spring-boot:run
```

`db-init` aplica primero `schema.sql` y luego `triggers_y_procedures.sql`; el comando siguiente carga los datos de demostración. Al iniciar, Spring también aplica `schema.sql`. Si usas un PostgreSQL propio en vez del de Compose, configura `DB_URL`, `DB_USER` y `DB_PASSWORD`, y aplica manualmente los tres archivos en este orden: `schema.sql`, `triggers_y_procedures.sql`, `datos_prueba.sql`.

## Estado del despliegue

Compose es un despliegue local de desarrollo, no una configuración para nube/producción. No hay todavía pipeline CI/CD, TLS, proxy inverso ni infraestructura cloud. En cada `docker compose up`, el servicio `db-init` aplica idempotentemente el esquema y los triggers/procedimientos antes de iniciar la API. La fixture de demostración es un paso separado y puede ejecutarse más de una vez sin duplicar sus registros.

El esquema reside en `backend/src/main/resources/SQLResources/schema.sql`; los triggers y procedimientos en `backend/src/main/resources/SQLResources/triggers_y_procedures.sql`; los datos de demostración en `backend/src/main/resources/SQLResources/datos_prueba.sql`. La inicialización crea las tablas de usuarios, billeteras y sus movimientos, estaciones, vehículos, viajes, tarifas, traslados y auditoría. Ejecuta la fixture después de que se hayan aplicado el esquema y los triggers/procedimientos.

El fixture crea los usuarios `UsuarioConDinero` (saldo 100), `UsuarioSinDinero` (saldo 0), `UsuarioSaldoNegativo` (saldo -1) y `OperadorFlota`. Comparten la contraseña `Prueba123!QA2026`. También crea cuatro estaciones de demostración: `DemoCentro` (5 vehículos), `DemoNorte` (3), `DemoSur` (3) y `DemoVacia` (sin vehículos). Centro y Norte permiten probar un rebalanceo de scooter; Centro incluye además un scooter con batería inferior al 20%.

## Desarrollo del frontend

El frontend Vue/Vite está en `frontend/` y Compose lo ejecuta en el contenedor `lab-tbd-frontend`. Para iniciar o reconstruir ambos proyectos, usa `docker compose up --build -d` desde la raíz. Para ver su salida en vivo:

```powershell
docker compose logs -f frontend
```

Vite sirve el frontend en `http://localhost:5173`, y el navegador accede a la API en `http://localhost:8080`. `VITE_API_URL` se configura para el frontend en `docker-compose.yml`. El contenedor instala las dependencias con `npm ci`; `node_modules/` y `dist/` están excluidos de Git.

## Autenticación y permisos

- Las rutas `/api/v1/auth/register`, `/api/v1/auth/login` y `/actuator/health` son públicas.
- El registro público siempre asigna el rol `USUARIO`; no acepta el rol desde el cliente.
- El resto de rutas requiere `Authorization: Bearer <JWT>`.
- Roles disponibles: `USUARIO`, `OPERADOR` y `ADMIN`. Los endpoints también pueden aplicar autorización por método con `@PreAuthorize`.
- La contraseña se almacena con BCrypt. Los tokens son stateless y duran 60 minutos por defecto.
- Usuario administrador inicial de desarrollo: `admin` / `admin12345`. Se crea solo si no existe.

Ejemplo de inicio de sesión:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"admin","password":"admin12345"}
```

La respuesta incluye `token`, `tokenType`, `username` y `rol`. Usa el token en las siguientes peticiones:

```http
Authorization: Bearer <token>
```

`401 Unauthorized` significa que falta o no es válido el token; `403 Forbidden` significa que el usuario autenticado no tiene el rol requerido.

## Configuración

| Variable | Valor local por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/lab_tbd` | URL JDBC. En Compose se reemplaza para usar el host `postgres`. |
| `DB_USER` | `labtbd` | Usuario PostgreSQL. |
| `DB_PASSWORD` | `labtbd` | Contraseña local PostgreSQL. |
| `PORT` | `8080` | Puerto HTTP de la API. |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos, separados por coma. |
| `JWT_SECRET` | Secreto de desarrollo incluido en configuración | Clave HMAC de al menos 32 bytes. Debe sustituirse fuera de local. |
| `JWT_EXPIRATION_MINUTES` | `60` | Vigencia del JWT. |
| `ADMIN_USERNAME` | `admin` | Usuario administrador inicial. |
| `ADMIN_PASSWORD` | `admin12345` | Contraseña inicial; cambiar fuera de local. |
| `ADMIN_EMAIL` | `admin@localhost` | Email del administrador inicial. |

Para probar otro secreto local, define `JWT_SECRET` en el entorno antes de levantar la aplicación. En producción no uses las credenciales ni el secreto por defecto; usa secretos robustos administrados fuera del repositorio.

## Rutas implementadas

| Método | Ruta | Acceso |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Público; crea rol `USUARIO` |
| `POST` | `/api/v1/auth/login` | Público |
| `GET` | `/api/v1/usuarios/me` | JWT válido |
| `GET`, `POST` | `/api/v1/usuarios` | `ADMIN` |
| `GET`, `PUT`, `DELETE` | `/api/v1/usuarios/{id}` | `ADMIN`; PUT permite cambiar la contraseña si se incluye; DELETE desactiva |
| `GET`, `POST`, `PUT`, `DELETE` | `/api/v1/estaciones...` | `OPERADOR` o `ADMIN`; DELETE desactiva |
| `GET`, `POST`, `PUT`, `PATCH`, `DELETE` | `/api/v1/vehiculos...` | `OPERADOR` o `ADMIN`; PUT actualiza datos editables; PATCH cambia estado con auditoría y DELETE da de baja |
| `GET`, `POST`, `PUT`, `DELETE` | `/api/v1/tarifas...` | `OPERADOR` o `ADMIN`; DELETE desactiva |
| `GET`, `POST`, `PUT`, `DELETE` | `/api/v1/billeteras...` | `ADMIN` crea; `OPERADOR` o `ADMIN` actualizan saldo con motivo y movimiento contable; DELETE desactiva solo con saldo cero |
| `GET` | `/api/v1/billeteras/me` y `/api/v1/billeteras/{id}/movimientos` | Usuario autenticado; los movimientos son propios |
| `GET` | `/api/v1/billeteras/{id}` y `/api/v1/billeteras` | `OPERADOR` o `ADMIN` |
| `PATCH` | `/api/v1/billeteras/{id}/movimientos/{ABONO\|CARGO}` | `OPERADOR` o `ADMIN`; ajuste auditado |
| `GET` | `/api/v1/viajes/me` | Usuario autenticado; historial propio |
| `GET` | `/api/v1/viajes/{id}` | Propietario del viaje, `OPERADOR` o `ADMIN` |
| `POST` | `/api/v1/viajes` | Usuario autenticado; inicia un viaje propio |
| `POST` | `/api/v1/viajes/{id}/finalizar` | Propietario del viaje; liquida el viaje |
| `PUT` | `/api/v1/viajes/{id}` | Propietario; finaliza el viaje con la estación de destino |
| `DELETE` | `/api/v1/viajes/{id}` | Propietario del viaje; cancela un viaje activo |
| `GET` | `/api/v1/viajes` | `OPERADOR` o `ADMIN`; historial global |
| `GET`, `POST`, `PUT`, `DELETE` | `/api/v1/ordenes-traslado...` | `OPERADOR` o `ADMIN`; PUT ajusta cantidad junto con el inventario; DELETE revierte el traslado si los vehículos siguen disponibles y existe capacidad |
| `GET` | `/api/v1/auditorias`, `/api/v1/auditorias/{id}`, `/api/v1/vehiculos/{id}/auditoria` | `OPERADOR` o `ADMIN` |
| `POST`, `PUT`, `DELETE` | `/api/v1/auditorias...` | Solo `ADMIN`; mantenimiento manual de registros |
| `GET` | `/api/v1/estaciones/disponibles` | Usuario autenticado |
| `GET` | `/api/v1/usuarios/me/resumen-mensual` | Usuario autenticado; propio resumen |
| `GET` | `/api/v1/usuarios/{id}/resumen-mensual` | `ADMIN` |
| `GET` | `/actuator/health` | Público |

Las operaciones que afectan historial tienen reglas específicas: los viajes se finalizan o cancelan mediante operaciones de dominio; los ajustes de billetera dejan movimientos contables; una orden solo se ajusta o elimina si el inventario permite aplicar también la operación inversa. Los endpoints administrativos de auditoría permiten mantenimiento manual y, por ello, pueden alterar el historial; en el flujo normal, los cambios de estado de vehículos siguen siendo registrados por el trigger. Estaciones, usuarios, billeteras, vehículos y tarifas se desactivan o dan de baja lógicamente para conservar referencias.
