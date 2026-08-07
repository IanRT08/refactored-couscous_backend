# AWSHEF — Backend

- **Universidad Tecnológica de Emiliano Zapata — UTEZ**
- **APLICACIÓN WEB DE MONITOREO DE VARIABLES CLIMÁTICAS PARA SISTEMA HÍBRIDO EÓLICO FOTOVOLTAICO**
- **Centro Nacional de Investigación y Desarrollo Tecnológico — CENIDET**

---

## Equipo de Desarrollo

| Nombre Completo | Rol / Tareas Principales | Usuario GitHub |
| :--- | :--- | :--- |
| Ian Alejandro Rivera Torres | Backend Developer (Módulos Impares): Control de accesos, integración de API externa, análisis gráfico y motor de reportes. | @IanRT08 |
| Dana Bahena Díaz | Backend Developer (Módulos Pares): Gestión de administración/permisos, dashboard, cálculos estadísticos y servicio de alertas. | @lum-i3 |
---

## Descripción del Proyecto

**¿Qué hace el sistema?**

El backend centraliza la adquisición, almacenamiento y exposición de datos provenientes de un sistema híbrido eólico-fotovoltaico instalado en CENIDET. Sus responsabilidades principales son:

- Conectarse de forma automática y continua a dos fuentes de telemetría externas (estación meteorológica Ambient Weather y registradores eléctricos ThingSpeak) para obtener lecturas en tiempo real cada 30–60 segundos.
- Detectar y rellenar brechas en el historial de datos, tanto al iniciar el servidor como en caso de recuperación tras una falla de conectividad.
- Permitir a los administradores iniciar importaciones históricas bajo demanda para recuperar meses o años de registros pasados.
- Calcular estadísticas agregadas (promedio, máximo, mínimo, moda) sobre cualquier rango de fechas seleccionado.
- Generar reportes descargables en formato Excel (.xlsx) y PDF con datos tabulados, estadísticas y gráficas de series de tiempo.
- Gestionar usuarios con múltiples roles, control de acceso basado en JWT, historial de auditoría y flujos de verificación por correo electrónico.
- Exponer una API REST que el frontend consume para dashboards, gráficas, administración y descarga de reportes.

**Objetivo:**

Automatizar y optimizar el análisis de variables climáticas y eléctricas de un sistema híbrido eólico-fotovoltaico, mediante el desarrollo de una aplicación web que centralice la información para su monitoreo y visualización en tiempo real.

---

## Stack Tecnológico y Características

| Tecnología | Versión | Uso |
| :--- | :--- | :--- |
| Java | 21 | Lenguaje principal |
| Spring Boot | 4.0.6 | Framework base |
| Spring Security + JWT | 7.0.5 / JJWT 0.12.6 | Autenticación y autorización |
| Spring Data JPA + Hibernate | 7.x | Acceso a base de datos |
| MySQL | 8.x | Base de datos relacional |
| Spring Mail (Jakarta Mail) | 4.x | Envío de correos transaccionales |
| Apache POI | 5.2.3 | Generación de reportes Excel |
| Apache FOP | 2.11 | Generación de reportes PDF |
| JFreeChart | 1.5.4 | Gráficas de series de tiempo |
| Lombok | — | Reducción de código repetitivo |
| Bean Validation | — | Validación de datos de entrada |

### API REST — Endpoints principales

**`/api/auth` — Autenticación**
- `POST /login` — Inicia sesión y devuelve un token JWT
- `POST /register` — Registra una nueva cuenta de usuario
- `POST /verify-account` — Confirma la cuenta mediante código OTP enviado por correo
- `POST /forgot-password` — Solicita el inicio del flujo de recuperación de contraseña
- `POST /verify-reset-code` — Valida el código OTP de recuperación
- `POST /reset-password` — Establece la nueva contraseña

**`/api/telemetria` — Lecturas**
- `GET /resumen` — Última lectura climática disponible *(público, sin autenticación)*
- `GET /publica/tabla` — Tabla paginada de lecturas anteriores al mes en curso *(público)*
- `GET /climatica` — Lecturas climáticas paginadas por rango de fechas
- `GET /electrica` — Lecturas eléctricas paginadas por rango de fechas y fuente

**`/api/estadisticas` — Estadísticas**
- `GET /climatica` — Promedio, máximo, mínimo y moda de variables climáticas en un período
- `GET /electrica` — Mismos agregados para variables eléctricas, por fuente
- `GET /electrica/combinada` — Estadísticas combinadas del sistema eólico + fotovoltaico
- `GET /climatica/resumen` — Resumen de cinco períodos para el dashboard (hoy, ayer, 7 días, 30 días, 1 año)
- `GET /fechas` — Fechas extremas disponibles en la base de datos (para limitar selectores de fecha)

**`/api/reportes` — Reportes descargables**
- `GET /descargar` — Genera y descarga un reporte en PDF o Excel con parámetros de rango, tipo (climático/eléctrico), fuente y variables seleccionadas

**`/api/solicitudes` — Solicitudes de permiso de descarga**
- `POST /` — El usuario envía una solicitud de acceso a reportes
- `GET /mis-solicitudes` — Consulta las solicitudes propias
- `GET /` — Lista todas las solicitudes *(administrador)*
- `PUT /resolver` — Aprueba o rechaza una solicitud *(administrador)*

**`/api/alertas` — Alertas**
- `GET /sistema` — Alertas públicas del sistema *(sin autenticación)*
- `GET /` — Alertas del usuario autenticado
- `GET|PUT /configuracion` — Preferencias de notificaciones del usuario

**`/api/admin` — Administración de usuarios**
- `GET /usuarios` — Lista paginada y filtrable de usuarios
- `GET|PUT /usuarios/{id}` — Detalle y edición de un usuario
- `PUT /usuarios/{id}/estado` — Cambio de estado (activo / bloqueado / desactivado)
- `PUT /usuarios/{id}/permiso-descarga` — Activar o desactivar permiso de descarga directo
- `GET|POST /administradores` — Listar y crear administradores *(superadministrador)*
- `PUT /administradores/{id}/tipo` — Cambiar tipo de administrador *(superadministrador)*
- `GET /historial` — Historial de acciones global paginado
- `GET /historial/{id}` — Historial de acciones de un usuario específico

**`/api/admin/respaldo-historico` — Importación histórica**
- `GET /estado` — Estado del proceso de importación (en curso / inactivo) por fuente
- `GET /detectar-inicio` — Detecta la fecha del dato más antiguo disponible en cada API externa
- `POST /climatico` — Inicia la importación histórica de datos climáticos *(superadministrador)*
- `POST /electrico` — Inicia la importación histórica de datos eléctricos *(superadministrador)*

**`/api/sync` — Estado de sincronización**
- `GET /estado` — Número de fallos consecutivos y estado de error por fuente de datos

**`/api/user` — Perfil propio**
- `GET|PUT /profile` — Consulta y actualización del perfil del usuario autenticado
- `PUT /change-password` — Cambio de contraseña
- `POST /request-deactivation` — Solicita un OTP para iniciar la baja de la cuenta
- `POST /deactivate` — Confirma y ejecuta la desactivación de la cuenta

### Servicios externos conectados

| Servicio | Protocolo | Datos obtenidos |
| :--- | :--- | :--- |
| **Ambient Weather REST API** | HTTPS | Temperatura, viento, humedad, radiación solar, presión atmosférica, precipitación |
| **ThingSpeak REST API** | HTTPS | Voltaje, corriente, potencia y Voc de los canales fotovoltaico y eólico |
| **Servidor SMTP** | SMTP/TLS | Envío de OTPs, credenciales de administrador y notificaciones de solicitudes |

### Tareas programadas y asíncronas

| Tarea | Tipo | Frecuencia / Disparador |
| :--- | :--- | :--- |
| Sincronización climática (Ambient Weather) | `@Scheduled` | Cada 60 segundos |
| Sincronización eléctrica (ThingSpeak) | `@Scheduled` | Cada 30 segundos |
| Purga de tokens OTP expirados | `@Scheduled` | Cada 60 minutos |
| Recuperación de brechas al inicio | `@EventListener` | Al arrancar el servidor |
| Recuperación de brechas post-falla | `@EventListener` | Al restaurarse una conexión fallida |
| Importación histórica (climática/eléctrica) | `@Async` | Bajo demanda por administrador |
| Envío de correos electrónicos | `@Async` | En cada evento de notificación |

---

## Capturas de Pantalla

*(Sección pendiente)*

---

## Instalación

*(Sección pendiente)*
