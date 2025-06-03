# Use Cases (Casos de Uso) - Sistema Hodei Packages

**Nota Importante:** La implementación detallada y las pruebas TDD para los siguientes casos de uso comenzarán después de completar el **scaffolding inicial del proyecto** (estructura de monorepo, proyectos base Kotlin y Angular, y configuraciones de build).

## 1. Formato de Casos de Uso

Cada caso de uso se definirá con la siguiente estructura:

*   **ID del Caso de Uso:** UC-XXX
*   **Nombre del Caso de Uso:** Descripción breve y concisa.
*   **Actor(es) Primario(s):** Quién inicia el caso de uso.
*   **Resumen:** Breve descripción del objetivo del caso de uso.
*   **Precondiciones:** Estado del sistema necesario antes de que el caso de uso pueda comenzar.
*   **Flujo Principal (Pasos):** Secuencia de acciones para un escenario exitoso.
*   **Flujos Alternativos/Excepciones:** Otros escenarios y manejo de errores.
*   **Postcondiciones:** Estado del sistema después de una ejecución exitosa.
*   **Requisitos No Funcionales Relacionados:** (Opcional) Rendimiento, seguridad, etc.
*   **Prioridad:** (MVP, v1.0, v2.0, etc.)
*   **Diagrama de Secuencia (Mermaid):** (Opcional, pero recomendado para flujos complejos)

## 2. Casos de Uso del MVP (Versión 0.1 - Base Funcional)

A continuación, se listan los placeholders para los casos de uso clave del MVP, que se detallarán progresivamente. Estos se derivan de los "Requerimientos funcionales" y el alcance del MVP en `projectbrief.md`.

### 2.1 Autenticación y Gestión de Usuarios (RBAC Básico)

*   **ID del Caso de Uso:** UC-001
*   **Nombre del Caso de Uso:** Registro del Primer Administrador y Login General de Usuarios
*   **Actor(es) Primario(s):**
    *   Para Registro del Primer Administrador: Administrador del Sistema (durante la configuración inicial o primer arranque).
    *   Para Login: Cualquier Usuario Registrado (incluyendo el Administrador).
*   **Resumen:**
    *   Permite la creación del primer usuario administrador del sistema si aún no existe y se cumplen las condiciones de configuración inicial.
    *   Permite a los usuarios registrados autenticarse en el sistema para acceder a sus funcionalidades mediante nombre de usuario/email y contraseña.
*   **Precondiciones:**
    *   **Para Registro del Primer Administrador:**
        *   El sistema está en un estado que permite la creación del primer administrador (ej. no existe ningún usuario con rol de administrador global, o se provee un token/mecanismo de setup especial).
        *   La base de datos de usuarios está accesible.
    *   **Para Login de Usuario:**
        *   El usuario que intenta iniciar sesión ya ha sido registrado en el sistema y su cuenta está activa.
        *   El sistema está operativo y el servicio de autenticación está accesible.
*   **Flujo Principal (Pasos):**

    *   **A. Registro del Primer Administrador (Ejecutado una sola vez o bajo condiciones especiales de setup)**
        1.  El Administrador del Sistema invoca un endpoint o mecanismo seguro destinado al registro del primer administrador (ej. `POST /api/setup/register-admin`).
        2.  Proporciona las credenciales deseadas para el administrador (ej: nombre de usuario, contraseña, email).
        3.  El Sistema (Backend):
            a.  Verifica que se cumplen las condiciones para registrar el primer administrador (ej. no hay otros administradores, o se presenta un token de setup válido).
            b.  Valida los datos de entrada (ej. fortaleza de la contraseña, formato del email, unicidad del nombre de usuario/email).
            c.  Genera un hash seguro de la contraseña proporcionada (utilizando un algoritmo como bcrypt o Argon2).
            d.  Crea una nueva entidad de usuario con los datos proporcionados y le asigna el rol de "Administrador Global" (o el rol más alto definido en el sistema).
            e.  Almacena el nuevo usuario administrador en la base de datos.
            f.  Responde al Administrador del Sistema con un mensaje de éxito (ej. HTTP 201 Created).

    *   **B. Login de Usuario Registrado**
        1.  El Usuario accede a la interfaz de login (ej. página de login en el Frontend).
        2.  El Usuario ingresa sus credenciales (nombre de usuario o email y contraseña).
        3.  El Usuario envía el formulario de login.
        4.  El Frontend envía una solicitud al endpoint de login del Backend (ej. `POST /api/auth/login`) con las credenciales.
        5.  El Sistema (Backend):
            a.  Busca en la base de datos un usuario que coincida con el nombre de usuario/email proporcionado.
            b.  Si se encuentra el usuario y está activo, compara el hash de la contraseña proporcionada con el hash almacenado para ese usuario.
            c.  Si las credenciales son válidas:
                i.  Genera un token de sesión (ej. JSON Web Token - JWT) que incluye identificadores del usuario (ID, username), sus roles y/o permisos, y una marca de tiempo de expiración.
                ii. Devuelve el token JWT al Frontend (ej. en el cuerpo de una respuesta HTTP 200 OK).
            d.  El Frontend recibe el token JWT:
                i.  Almacena el token de forma segura (ej. `localStorage`, `sessionStorage`, o cookie HttpOnly si es aplicable).
                ii. Redirige al usuario a la página principal post-autenticación (ej. dashboard) o actualiza el estado de la UI para reflejar la sesión iniciada.
*   **Flujos Alternativos/Excepciones:**

    *   **A. Registro del Primer Administrador:**
        *   A3a.1: No se cumplen las condiciones para registrar el primer admin (ej. ya existe un admin, token de setup inválido): El Sistema rechaza la solicitud (ej. HTTP 409 Conflict o 403 Forbidden) con un mensaje explicativo.
        *   A3b.1: Datos de entrada inválidos (ej. contraseña demasiado débil, formato de email incorrecto, nombre de usuario ya en uso por un intento previo fallido): El Sistema rechaza la solicitud (ej. HTTP 400 Bad Request) indicando los errores de validación.
        *   A3e.1: Error al guardar en la base de datos: El Sistema devuelve un error interno (ej. HTTP 500 Internal Server Error).

    *   **B. Login de Usuario Registrado:**
        *   B5a.1: Usuario no encontrado en la base de datos: El Sistema devuelve un error de autenticación (ej. HTTP 401 Unauthorized, mensaje genérico de "credenciales inválidas").
        *   B5b.1: Contraseña incorrecta: El Sistema devuelve un error de autenticación (ej. HTTP 401 Unauthorized, mensaje genérico de "credenciales inválidas").
        *   B5b.2: Cuenta de usuario inactiva o bloqueada: El Sistema devuelve un error específico (ej. HTTP 403 Forbidden, "Cuenta inactiva/bloqueada").
        *   (Opcional) B5.X: Múltiples intentos de login fallidos para un usuario: El Sistema podría implementar un bloqueo temporal de la cuenta o requerir un CAPTCHA.
        *   B5c.i.1: Error al generar el token JWT: El Sistema devuelve un error interno (ej. HTTP 500 Internal Server Error).
*   **Postcondiciones:**

    *   **A. Registro del Primer Administrador (Tras ejecución exitosa):**
        *   Existe un nuevo usuario con rol de administrador en el sistema.
        *   El mecanismo para registrar el primer administrador podría quedar deshabilitado o requerir condiciones diferentes para futuras invocaciones.
    *   **B. Login de Usuario Registrado (Tras ejecución exitosa):**
        *   El usuario tiene una sesión activa reconocida por el sistema.
        *   El Frontend posee un token de sesión válido que se utilizará para autenticar solicitudes subsecuentes a recursos protegidos.
*   **Requisitos No Funcionales Relacionados:**
    *   **Seguridad:**
        *   Las contraseñas deben almacenarse utilizando algoritmos de hash fuertes y salteados (ej. bcrypt, scrypt, Argon2).
        *   Los tokens JWT deben ser firmados digitalmente (ej. HS256, RS256) para prevenir su manipulación.
        *   Los tokens JWT deben tener una fecha de expiración relativamente corta y se debe considerar un mecanismo de refresco de token si se requieren sesiones largas.
        *   La comunicación entre Frontend y Backend debe ser sobre HTTPS.
        *   Proteger contra ataques comunes (CSRF si se usan cookies, XSS en el frontend, enumeración de usuarios).
    *   **Rendimiento:** El proceso de login debe completarse en un tiempo aceptable (ej. < 500ms bajo carga normal).
    *   **Usabilidad:** Mensajes de error claros para el usuario en caso de fallo de login.
*   **Prioridad:** MVP
*   **Diagrama de Secuencia (Mermaid):**

    *   **Flujo de Login de Usuario:**
        ```mermaid
        sequenceDiagram
            participant User as Usuario (Navegador)
            participant FE as Frontend (App Angular)
            participant BE as Backend (API Kotlin)
            participant DB as Base de Datos

            User->>FE: Ingresa credenciales (username/email, password) y envía
            FE->>BE: POST /api/auth/login (credentials)
            BE->>DB: Consultar Usuario por username/email
            DB-->>BE: Datos del Usuario (si existe, incluye hash de contraseña)
            alt Usuario encontrado Y Contraseña válida Y Cuenta activa
                BE->>BE: Generar Token JWT (payload: userId, username, roles, exp)
                BE-->>FE: HTTP 200 OK (response: { token: "jwt_token_string" })
                FE->>FE: Almacenar token de forma segura
                FE->>User: Redirigir a Dashboard / Actualizar UI a estado "logueado"
            else Credenciales inválidas o Usuario no activo/encontrado
                BE-->>FE: HTTP 401 Unauthorized (Error: "Credenciales inválidas" o "Cuenta no activa")
                FE->>User: Mostrar mensaje de error apropiado
            end
        ```

    *   **Flujo de Registro del Primer Administrador (Simplificado):**
        ```mermaid
        sequenceDiagram
            participant SysAdmin as Admin. Sistema (CLI/Script/Setup UI)
            participant BE as Backend (API Kotlin)
            participant DB as Base de Datos

            SysAdmin->>BE: POST /api/setup/register-admin (admin_username, admin_password, admin_email, setup_key?)
            BE->>BE: Validar condición de "primer admin" / validez de setup_key
            alt Condición para registrar primer admin es VÁLIDA
                BE->>BE: Validar datos de entrada (fortaleza contraseña, formato email, etc.)
                alt Datos de entrada VÁLIDOS
                    BE->>BE: Hashear contraseña (bcrypt/Argon2)
                    BE->>DB: Crear Usuario (username, hashed_password, email, role: 'GLOBAL_ADMIN', active: true)
                    DB-->>BE: Confirmación de creación de usuario
                    BE-->>SysAdmin: HTTP 201 Created (Mensaje: "Administrador creado exitosamente")
                else Datos de entrada INVÁLIDOS
                    BE-->>SysAdmin: HTTP 400 Bad Request (Errores de validación)
                end
            else Condición para registrar primer admin es INVÁLIDA
                BE-->>SysAdmin: HTTP 403 Forbidden/409 Conflict (Mensaje: "No se puede registrar admin" o "Admin ya existe")
            end
        ```
*   **ID del Caso de Uso:** UC-002
*   **Nombre del Caso de Uso:** Gestión de Tokens de Sesión y Ciclo de Vida de la Sesión
*   **Actor(es) Primario(s):**
    *   Usuario Autenticado (a través del Frontend)
    *   Sistema (Backend y Frontend)
*   **Resumen:** Este caso de uso describe cómo el sistema gestiona los tokens de sesión (JWT) después de una autenticación exitosa (UC-001), incluyendo su validación para solicitudes protegidas, el proceso de logout, y el manejo de la expiración del token.
*   **Precondiciones:**
    *   El Usuario se ha autenticado exitosamente (ver UC-001) y el Frontend ha recibido y almacenado un token JWT.
    *   El Backend está configurado para emitir y validar tokens JWT.
*   **Flujo Principal (Pasos):**

    *   **A. Validación de Token para Solicitud Protegida:**
        1.  El Usuario (a través del Frontend) intenta acceder a un recurso protegido del Backend (ej. `GET /api/user/profile`).
        2.  El Frontend adjunta el token JWT almacenado a la cabecera de la solicitud (ej. `Authorization: Bearer <token>`).
        3.  El Backend recibe la solicitud.
        4.  Un middleware/interceptor de autenticación en el Backend intercepta la solicitud.
            a.  Extrae el token JWT de la cabecera.
            b.  Verifica la firma del token utilizando la clave secreta/pública configurada.
            c.  Verifica que el token no haya expirado.
            d.  (Opcional) Verifica otras claims del token (ej. issuer, audience).
        5.  Si el token es válido:
            a.  El middleware permite que la solicitud continúe hacia el controlador del recurso solicitado.
            b.  (Opcional) La información del usuario (ej. ID, roles) extraída del token se hace disponible para el contexto de la solicitud.
            c.  El Backend procesa la solicitud y devuelve la respuesta apropiada (ej. HTTP 200 OK con los datos del perfil).
        6.  El Frontend recibe la respuesta y la procesa.

    *   **B. Logout de Usuario:**
        1.  El Usuario (a través del Frontend) hace clic en la opción "Logout" o "Cerrar Sesión".
        2.  El Frontend:
            a.  Elimina el token JWT almacenado localmente (ej. de `localStorage` o `sessionStorage`).
            b.  (Opcional, si se implementa) Envía una solicitud al Backend a un endpoint de logout (ej. `POST /api/auth/logout`) para invalidar el token en el servidor (si se usa una lista de revocación o sesiones en BD).
            c.  Redirige al usuario a la página de login o a una página pública.
            d.  Actualiza el estado de la UI para reflejar que no hay sesión activa.
        3.  (Si se implementó B2b) El Backend recibe la solicitud de logout:
            a.  Si utiliza una lista de revocación de tokens (blacklist), añade el JTI (JWT ID) del token a la lista hasta su expiración natural.
            b.  Si gestiona sesiones en base de datos, marca la sesión como inválida.
            c.  Responde al Frontend con un mensaje de éxito (ej. HTTP 200 OK).

    *   **C. Manejo de Token Expirado (por el Frontend):**
        1.  El Frontend realiza una solicitud a un endpoint protegido del Backend (como en el Flujo A).
        2.  El Backend valida el token y detecta que ha expirado (Flujo A, paso 4c).
        3.  El Backend responde con un error de autenticación indicando token expirado (ej. HTTP 401 Unauthorized, con un código de error específico o mensaje).
        4.  El Frontend intercepta esta respuesta de error:
            a.  Elimina el token JWT expirado almacenado localmente.
            b.  Redirige al usuario a la página de login.
            c.  Muestra un mensaje al usuario indicando que su sesión ha expirado y necesita volver a iniciar sesión.
            d.  (Opcional, si se implementa refresh token) Intenta obtener un nuevo token de acceso usando un refresh token antes de redirigir.

*   **Flujos Alternativos/Excepciones:**

    *   **A. Validación de Token:**
        *   A4b.1: Firma del token inválida (token manipulado): El Backend responde con HTTP 401 Unauthorized (o 403 Forbidden). El Frontend trata esto como un token inválido (similar a expirado).
        *   A4c.1: Token expirado: Ver Flujo C.
        *   A4.1: No se proporciona token en la solicitud: El Backend responde con HTTP 401 Unauthorized.
        *   A4.2: Formato de token incorrecto: El Backend responde con HTTP 400 Bad Request o 401 Unauthorized.

    *   **B. Logout de Usuario:**
        *   B2b.1: (Si se implementa logout en backend) El endpoint de logout del backend no está disponible o falla: El Frontend procede a eliminar el token localmente y actualizar la UI, pero puede registrar el error. La sesión en el backend podría seguir activa hasta la expiración natural del token si no hay lista de revocación.

    *   **C. Manejo de Token Expirado:**
        *   C4d.1: (Si se implementa refresh token) El intento de refrescar el token falla (ej. refresh token también expirado o inválido): El Frontend procede con la redirección a login.

*   **Postcondiciones:**

    *   **A. Validación de Token Exitosa:** El usuario accede al recurso protegido.
    *   **B. Logout Exitoso:** La sesión del usuario se considera terminada en el Frontend. El token JWT ya no es utilizable por el Frontend. Si se implementa, el token puede estar invalidado en el Backend.
    *   **C. Manejo de Token Expirado Exitoso:** El usuario es redirigido a la página de login.
*   **Requisitos No Funcionales Relacionados:**
    *   **Seguridad:**
        *   Los tokens JWT deben ser firmados (no solo encriptados).
        *   Usar HTTPS para todas las comunicaciones que transmitan tokens.
        *   Considerar el uso de `HttpOnly` cookies para almacenar tokens si el frontend y backend comparten el mismo dominio de primer nivel (para mitigar XSS), aunque `localStorage` es común para SPAs con APIs en diferentes dominios (requiere protección XSS en el frontend).
        *   Los tokens deben tener una vida útil corta (ej. 15-60 minutos para tokens de acceso) y considerar un mecanismo de refresh token para sesiones más largas.
        *   El logout del lado del servidor (lista de revocación) es más seguro pero añade complejidad. Un logout solo del lado del cliente es más simple pero el token sigue siendo válido hasta que expira.
    *   **Rendimiento:** La validación del token en el backend debe ser rápida.
*   **Prioridad:** MVP
*   **Diagrama de Secuencia (Mermaid):**

    *   **Validación de Token para Solicititud Protegida:**
        ```mermaid
        sequenceDiagram
            participant UserFE as Usuario (Frontend)
            participant BE as Backend (API)
            participant AuthMiddleware as Middleware de Autenticación (BE)

            UserFE->>BE: GET /api/recurso-protegido (Header: Authorization: Bearer <token>)
            BE->>AuthMiddleware: Interceptar Solicitud
            AuthMiddleware->>AuthMiddleware: Extraer y Validar JWT (firma, expiración)
            alt Token Válido
                AuthMiddleware-->>BE: Permitir Solicitud (Contexto de Usuario opcional)
                BE->>BE: Procesar solicitud del recurso
                BE-->>UserFE: HTTP 200 OK (Datos del Recurso)
            else Token Inválido/Expirado
                AuthMiddleware-->>BE: Denegar Solicitud
                BE-->>UserFE: HTTP 401 Unauthorized (Error: Token inválido/expirado)
            end
        ```

    *   **Logout de Usuario (con invalidación en Backend opcional):**
        ```mermaid
        sequenceDiagram
            participant UserFE as Usuario (Frontend)
            participant BE as Backend (API)

            UserFE->>UserFE: Clic en "Logout"
            UserFE->>UserFE: Eliminar token de localStorage
            opt Solicitud de Logout al Backend
                UserFE->>BE: POST /api/auth/logout (Header: Authorization: Bearer <token_a_invalidar>)
                BE->>BE: Invalidar token (ej. añadir a blacklist)
                BE-->>UserFE: HTTP 200 OK (Logout exitoso en backend)
            end
            UserFE->>UserFE: Redirigir a /login, Actualizar UI
        ```
*   **ID del Caso de Uso:** UC-003
*   **Nombre del Caso de Uso:** Definición y Conceptualización de Roles y Permisos del MVP
*   **Actor(es) Primario(s):** Arquitecto del Sistema / Desarrollador (durante el diseño e implementación inicial), Sistema (para la aplicación interna de estos roles y permisos).
*   **Resumen:** Este caso de uso define los roles de usuario básicos para el MVP (`ADMIN`, `USER`) y los conjuntos de permisos asociados conceptualmente a cada uno. Describe cómo el sistema interpreta y utiliza estos roles para controlar el acceso a funcionalidades y datos. No cubre la gestión dinámica de roles o permisos por un administrador en esta fase (eso sería post-MVP), sino su estructura fundamental y aplicación inicial.
*   **Precondiciones:**
    *   La arquitectura del sistema ha sido definida (ej. Hexagonal).
    *   Se ha decidido implementar un sistema de Autenticación (UC-001) y Autorización basada en Roles (RBAC).
    *   Se ha definido la gestión de tokens de sesión (UC-002).
*   **Flujo Principal (Pasos):**

    1.  **Definición de Roles del MVP:**
        a.  Se establece el rol `ADMIN`. Este rol tiene acceso completo a todas las funcionalidades del sistema, incluyendo la gestión de usuarios, la administración de repositorios y la configuración del sistema.
        b.  Se establece el rol `USER`. Este rol tiene acceso limitado, principalmente para interactuar con repositorios Maven (publicar artefactos en repositorios donde tenga permiso de escritura, y descargar/resolver artefactos de repositorios públicos o a los que tenga acceso de lectura).

    2.  **Definición Conceptual de Permisos para el MVP:**
        a.  Se identifican las acciones clave del sistema que requieren control de acceso. Ejemplos de permisos conceptuales (la granularidad exacta de implementación puede variar):
            *   `users:create`, `users:read_list`, `users:read_details`, `users:update_role`, `users:delete`
            *   `repositories:create`, `repositories:read_list`, `repositories:read_details`, `repositories:update_metadata`, `repositories:delete`
            *   `artifacts:upload_to_repoX`, `artifacts:download_from_repoX` (donde repoX es un repo específico o un tipo de repo)
            *   `system:view_logs`, `system:manage_settings`
        b.  Para el MVP, la asignación de estos permisos a los roles es implícita y directa en la lógica de negocio y protección de endpoints:
            *   El rol `ADMIN` posee (o se le concede acceso equivalente a) todos los permisos definidos.
            *   El rol `USER` posee (o se le concede acceso equivalente a) permisos como `artifacts:upload_to_own_or_assigned_repo`, `artifacts:download_from_accessible_repo`, `repositories:read_list` (para repositorios públicos/accesibles).

    3.  **Asignación Inicial de Roles:**
        a.  El primer usuario creado durante la configuración inicial del sistema (ver UC-001) se asigna automáticamente al rol `ADMIN`.
        b.  Los usuarios creados posteriormente por un `ADMIN` (ver UC-004) se asignarán por defecto al rol `USER` (esta es la política para el MVP).

    4.  **Aplicación de Roles y Permisos en el Sistema (Backend):**
        a.  Los endpoints de la API del Backend se protegen utilizando la información de rol (y, conceptualmente, los permisos asociados) del usuario autenticado. Esta información se extrae del token JWT (ver UC-002).
        b.  La lógica de autorización en el backend verifica si el rol del usuario le permite realizar la acción solicitada.
            *   Ejemplo: Un endpoint `POST /api/admin/users` (para crear un usuario) requerirá que el solicitante tenga el rol `ADMIN`.
            *   Ejemplo: Un endpoint `PUT /api/maven/{repoName}/{path}` (para publicar un artefacto) verificará que el usuario (con rol `USER` o `ADMIN`) tenga permisos de escritura sobre el repositorio `{repoName}`. (La gestión detallada de permisos por repositorio es parte de UC-005 y UC-006, pero el rol es el primer nivel de chequeo).

    5.  **Reflejo de Roles en el Sistema (Frontend):**
        a.  El Frontend recibe el rol del usuario como parte de la información de perfil después del login (ej. en el payload del token JWT o mediante una solicitud GET al perfil del usuario).
        b.  La interfaz de usuario (UI) se adapta dinámicamente para mostrar u ocultar opciones de menú, botones o secciones enteras basadas en el rol del usuario.
            *   Ejemplo: Un panel de "Administración del Sistema" solo será visible y accesible para usuarios con el rol `ADMIN`.
            *   Ejemplo: El botón "Crear Repositorio" solo será visible para usuarios `ADMIN`.

*   **Flujos Alternativos/Excepciones:**
    *   Este caso de uso es principalmente descriptivo de la configuración del sistema. Las excepciones (como "Acceso Denegado") se manejan en los casos de uso que *utilizan* estos roles y permisos para proteger recursos (ej. UC-001, UC-002, UC-004, UC-005, etc.).

*   **Postcondiciones:**
    *   Los roles `ADMIN` y `USER` están definidos y documentados como parte del diseño del sistema.
    *   Los permisos conceptuales asociados a estos roles para el MVP están identificados.
    *   El sistema tiene una base clara para implementar la lógica de autorización RBAC en los componentes del backend y del frontend.
*   **Requisitos No Funcionales Relacionados:**
    *   **Seguridad:** El mecanismo de RBAC debe ser implementado de forma segura para prevenir el acceso no autorizado y la escalada de privilegios.
    *   **Mantenibilidad:** Aunque para el MVP los roles y sus permisos son relativamente fijos, el diseño subyacente debe ser lo suficientemente flexible para permitir una gestión más granular y dinámica de roles y permisos en futuras versiones (Post-MVP).
    *   **Claridad:** Las responsabilidades de cada rol deben ser claras y bien definidas.
*   **Prioridad:** MVP
*   **Diagrama de Secuencia (Mermaid):**
    *   Ilustración de cómo un endpoint protegido utiliza la información de rol para autorización:
        ```mermaid
        sequenceDiagram
            participant UserFE as Usuario (Frontend)
            participant BE_API as Backend API Endpoint (Ej: /api/admin/users)
            participant AuthMiddleware as Middleware de Autenticación (BE)
            participant AuthZ_Logic as Lógica de Autorización (BE)

            UserFE->>BE_API: POST /api/admin/users (Payload: datos_nuevo_usuario, Token JWT con rol='ADMIN')
            BE_API->>AuthMiddleware: Validar Token JWT (Ver UC-002)
            AuthMiddleware-->>BE_API: Token Válido, Usuario (claims: {..., role: 'ADMIN'})
            
            BE_API->>AuthZ_Logic: VerificarPermiso(usuario_claims, accion='crear_usuario')
            AuthZ_Logic->>AuthZ_Logic: Lógica (rol 'ADMIN' tiene permiso para 'crear_usuario')
            AuthZ_Logic-->>BE_API: Acceso Permitido
            
            BE_API->>BE_API: Procesar creación de usuario...
            BE_API-->>UserFE: HTTP 201 Created (Usuario creado)

            %% Escenario Alternativo: Usuario sin permisos intenta acceder
            UserFE->>BE_API: POST /api/admin/users (Payload: datos_nuevo_usuario, Token JWT con rol='USER')
            BE_API->>AuthMiddleware: Validar Token JWT (Ver UC-002)
            AuthMiddleware-->>BE_API: Token Válido, Usuario (claims: {..., role: 'USER'})

            BE_API->>AuthZ_Logic: VerificarPermiso(usuario_claims, accion='crear_usuario')
            AuthZ_Logic->>AuthZ_Logic: Lógica (rol 'USER' NO tiene permiso para 'crear_usuario')
            AuthZ_Logic-->>BE_API: Acceso Denegado
            
            BE_API-->>UserFE: HTTP 403 Forbidden (No tienes permisos suficientes)
        end
        ```
*   **ID del Caso de Uso:** UC-004
*   **Nombre del Caso de Uso:** Gestión Básica de Usuarios por Administrador
*   **Actor(es) Primario(s):** Administrador (Usuario con rol `ADMIN`)
*   **Resumen:** Este caso de uso permite a un Administrador gestionar usuarios del sistema. Para el MVP, esto incluye crear nuevos usuarios (con rol `USER` por defecto), listar usuarios existentes, ver detalles de un usuario y modificar el rol de un usuario existente (entre `ADMIN` y `USER`).
*   **Precondiciones:**
    *   El Administrador ha iniciado sesión en el sistema (ver UC-001) y su token de sesión es válido (ver UC-002).
    *   El sistema tiene definidos los roles `ADMIN` y `USER` (ver UC-003).
    *   Existe una interfaz (Frontend) y endpoints (Backend) para estas operaciones, protegidos para acceso exclusivo de `ADMIN`.
*   **Flujo Principal (Pasos):**

    *   **A. Crear Nuevo Usuario (con rol `USER` por defecto):**
        1.  El Administrador navega a la sección de gestión de usuarios en el Frontend y selecciona la opción "Crear Usuario".
        2.  El Frontend muestra un formulario para ingresar los datos del nuevo usuario (ej. nombre de usuario, email, contraseña inicial).
        3.  El Administrador completa el formulario y lo envía.
        4.  El Frontend envía una solicitud `POST` al endpoint del Backend (ej. `/api/admin/users`) con los datos del nuevo usuario.
        5.  El Backend:
            a.  Valida que el solicitante es un `ADMIN` (basado en el token JWT).
            b.  Valida los datos del nuevo usuario (ej. formato de email, complejidad de contraseña, nombre de usuario único).
            c.  Crea el nuevo registro de usuario en la base de datos, asignándole el rol `USER` por defecto y hasheando la contraseña.
            d.  Responde al Frontend con un mensaje de éxito (ej. HTTP 201 Created) y los datos del usuario creado (sin la contraseña).
        6.  El Frontend muestra un mensaje de confirmación al Administrador y actualiza la lista de usuarios.

    *   **B. Listar Usuarios Existentes:**
        1.  El Administrador navega a la sección de gestión de usuarios en el Frontend.
        2.  El Frontend envía una solicitud `GET` al endpoint del Backend (ej. `/api/admin/users`) para obtener la lista de usuarios.
        3.  El Backend:
            a.  Valida que el solicitante es un `ADMIN`.
            b.  Recupera la lista de usuarios de la base de datos (con información relevante como ID, nombre de usuario, email, rol, estado).
            c.  Responde al Frontend con la lista de usuarios (ej. HTTP 200 OK).
        4.  El Frontend muestra la lista de usuarios en una tabla o formato similar.

    *   **C. Ver Detalles de un Usuario Específico (No implementado en MVP, pero conceptualmente parte de gestión):**
        *   (Este flujo se simplifica en MVP, la lista puede mostrar todos los detalles necesarios. Una vista detallada dedicada podría ser post-MVP).

    *   **D. Modificar Rol de un Usuario Existente:**
        1.  El Administrador, desde la lista de usuarios (Flujo B), selecciona un usuario y elige la opción "Modificar Rol".
        2.  El Frontend muestra la información del usuario y una opción para cambiar su rol (ej. un desplegable con `ADMIN`, `USER`).
        3.  El Administrador selecciona el nuevo rol para el usuario y confirma el cambio.
        4.  El Frontend envía una solicitud `PUT` o `PATCH` al endpoint del Backend (ej. `/api/admin/users/{userId}/role`) con el nuevo rol.
        5.  El Backend:
            a.  Valida que el solicitante es un `ADMIN`.
            b.  Valida que el `userId` es válido y que el nuevo rol es permitido.
            c.  (Consideración de seguridad): Impide que un `ADMIN` se quite a sí mismo el rol de `ADMIN` si es el único `ADMIN` activo.
            d.  Actualiza el rol del usuario en la base de datos.
            e.  Responde al Frontend con un mensaje de éxito (ej. HTTP 200 OK).
        6.  El Frontend muestra un mensaje de confirmación y actualiza la información del usuario en la lista.

*   **Flujos Alternativos/Excepciones:**

    *   **A. Crear Nuevo Usuario:**
        *   A4a.1: Solicitante no es `ADMIN`: Backend responde HTTP 403 Forbidden.
        *   A4b.1: Datos inválidos (ej. email no válido, nombre de usuario ya existe): Backend responde HTTP 400 Bad Request con detalles del error. Frontend muestra el error al Administrador.
        *   A4c.1: Error al crear usuario en BD: Backend responde HTTP 500 Internal Server Error.

    *   **B. Listar Usuarios:**
        *   B3a.1: Solicitante no es `ADMIN`: Backend responde HTTP 403 Forbidden.
        *   B3b.1: Error al obtener usuarios de BD: Backend responde HTTP 500 Internal Server Error.

    *   **D. Modificar Rol de Usuario:**
        *   D5a.1: Solicitante no es `ADMIN`: Backend responde HTTP 403 Forbidden.
        *   D5b.1: `userId` no válido o rol no permitido: Backend responde HTTP 400 Bad Request o 404 Not Found.
        *   D5c.1: Intento de eliminar el último rol `ADMIN`: Backend responde HTTP 400 Bad Request o 409 Conflict ("No se puede eliminar el último administrador").
        *   D5d.1: Error al actualizar rol en BD: Backend responde HTTP 500 Internal Server Error.

*   **Postcondiciones:**

    *   **A. Crear Nuevo Usuario:** Un nuevo usuario existe en el sistema con el rol `USER`.
    *   **B. Listar Usuarios:** El Administrador ha visualizado la lista actualizada de usuarios.
    *   **D. Modificar Rol:** El rol del usuario seleccionado ha sido actualizado en el sistema.
*   **Requisitos No Funcionales Relacionados:**
    *   **Seguridad:** Todas las operaciones de gestión de usuarios deben estar estrictamente limitadas a usuarios con rol `ADMIN`. Las contraseñas deben manejarse de forma segura (hasheadas).
    *   **Usabilidad (Frontend):** La interfaz para la gestión de usuarios debe ser clara e intuitiva para el Administrador.
    *   **Auditoría (Post-MVP):** Sería deseable registrar eventos de creación y modificación de usuarios.
*   **Prioridad:** MVP
*   **Diagrama de Secuencia (Mermaid):**

    *   **Crear Nuevo Usuario:**
        ```mermaid
        sequenceDiagram
            participant AdminFE as Administrador (Frontend)
            participant BE_AdminAPI as Backend API (/api/admin/users)
            participant UserRepo as Repositorio de Usuarios (BE)

            AdminFE->>AdminFE: Rellena formulario nuevo usuario
            AdminFE->>BE_AdminAPI: POST /api/admin/users (datos_usuario)
            BE_AdminAPI->>BE_AdminAPI: Validar solicitante es ADMIN
            BE_AdminAPI->>BE_AdminAPI: Validar datos_usuario
            alt Datos Válidos
                BE_AdminAPI->>UserRepo: CrearUsuario(datos_usuario_procesados, rol='USER')
                UserRepo-->>BE_AdminAPI: Usuario Creado (info_usuario)
                BE_AdminAPI-->>AdminFE: HTTP 201 Created (info_usuario)
                AdminFE->>AdminFE: Mostrar confirmación, actualizar lista
            else Datos Inválidos o No Autorizado
                BE_AdminAPI-->>AdminFE: HTTP 400/403 (Error)
                AdminFE->>AdminFE: Mostrar error
            end
        ```

    *   **Modificar Rol de Usuario:**
        ```mermaid
        sequenceDiagram
            participant AdminFE as Administrador (Frontend)
            participant BE_AdminAPI as Backend API (/api/admin/users/{userId}/role)
            participant UserRepo as Repositorio de Usuarios (BE)

            AdminFE->>AdminFE: Selecciona usuario, elige nuevo rol
            AdminFE->>BE_AdminAPI: PUT /api/admin/users/{userId}/role (nuevo_rol)
            BE_AdminAPI->>BE_AdminAPI: Validar solicitante es ADMIN
            BE_AdminAPI->>BE_AdminAPI: Validar userId y nuevo_rol
            alt Datos Válidos y Permitido
                BE_AdminAPI->>UserRepo: ActualizarRolUsuario(userId, nuevo_rol)
                UserRepo-->>BE_AdminAPI: Rol Actualizado
                BE_AdminAPI-->>AdminFE: HTTP 200 OK
                AdminFE->>AdminFE: Mostrar confirmación, actualizar UI
            else Datos Inválidos, No Autorizado o Regla de Negocio Rota
                BE_AdminAPI-->>AdminFE: HTTP 400/403/404/409 (Error)
                AdminFE->>AdminFE: Mostrar error
            end
        ```

### 2.2 Gestión de Repositorios Maven

*   **UC-005: (Admin) Crear Repositorio Maven**
    *   Prioridad: MVP
*   **UC-006: Publicar Artefacto Maven (JAR, POM)**
    *   Prioridad: MVP
    *   Diagrama de Secuencia (Ejemplo Alto Nivel):
        ```mermaid
        sequenceDiagram
            participant Client as Cliente (Maven CLI / CI)
            participant System as Sistema Hodei
            participant Auth as Módulo de Autenticación
            participant Storage as Módulo de Almacenamiento
            participant Metadata as Módulo de Metadatos

            Client->>System: PUT /api/maven/{repoName}/{path} (artifact, auth)
            System->>Auth: Validar(auth, repoName, 'write')
            Auth-->>System: OK / Error
            alt Acceso Autorizado
                System->>Storage: Guardar Artifact(artifact)
                Storage-->>System: OK (artifact_location)
                System->>Metadata: Registrar/Actualizar Metadatos(artifact_info, artifact_location)
                Metadata-->>System: OK
                System-->>Client: 201 Created
            else Acceso Denegado
                System-->>Client: 401 Unauthorized / 403 Forbidden
            end
        ```
*   **UC-007: Descargar/Resolver Artefacto Maven**
    *   Prioridad: MVP
*   **UC-008: (Admin) Listar Repositorios**
    *   Prioridad: MVP (Backend + Frontend)
*   **UC-009: (Usuario) Navegar Contenido de Repositorio Maven (Frontend)**
    *   Prioridad: MVP (Frontend)

### 2.3 Logging Básico
*   **UC-010: Registrar Operación Crítica del Sistema**
    *   Prioridad: MVP

## 3. Casos de Uso Futuros (Post-MVP / Versión 1.0+)

*   Soporte para repositorios npm (UC-NPM-XXX).
*   Integración con almacenamiento Amazon S3 (UC-S3-XXX).
*   Sistema de eventos con Kafka/RabbitMQ (UC-EVENT-XXX).
*   Gestión avanzada de usuarios y roles en UI.
*   Dashboard con estadísticas.
*   Buscador de artefactos.
*   ... y otros definidos en el `project-prd.md` para v1.0 y v2.0.

Este archivo se actualizará a medida que se detallen los casos de uso y se avance en el ciclo TDD.
