# Progress: Estado del Proyecto Hodei Packages

## 1. Estado General del Proyecto

*   **Fase Actual:** Planificación Detallada de Fase 2 - Implementación del MVP (Refinando desglose de tareas).
*   **Descripción:** Se ha completado la Fase 1 de Scaffolding. Actualmente, se está actualizando `progress.md` con un desglose más granular de tareas para la Fase 2 (MVP), incluyendo subtareas de desarrollo y testing.

## 2. Último Hito Alcanzado

*   **Hito:** Detallado UC-001 (Registro y Login de Usuario) en `usecases.md`.
*   **Fecha:** 2025-06-02
*   **Detalles:** Fase 1 de Scaffolding completada. `project-prd.md` analizado para planificación detallada. UC-001 definido.

## 3. Estado TDD Actual

*   **ROJO (Planificación y Próxima Funcionalidad):** La Fase 1 de Scaffolding está completa. Se está detallando el plan de tareas del MVP. Una vez finalizado, se procederá a detallar los siguientes Casos de Uso y luego a escribir las pruebas de aceptación/integración para UC-001.

## 4. Hitos y Tareas Planificadas

### Fase 0: Configuración y Planificación (Completada)
*   [x] Lectura y análisis del `project-prd.md`.
*   [x] Creación del directorio `memory-bank`.
*   [x] Generación de archivos iniciales del Banco de Memoria.
*   [x] Definición detallada de tareas de scaffolding.

### Fase 1: Scaffolding del Proyecto (Completada)
*   **[x] Tarea 1.1: Diseñar y Crear Estructura Base del Monorepo.**
*   **[x] Tarea 1.2: Inicializar Proyecto Backend Kotlin (Multi-módulo con Gradle).**
*   **[x] Tarea 1.3: Inicializar Proyecto Frontend Angular.**
*   **[x] Tarea 1.4: Configurar Linters, Formateadores y Scripts de Build/Ejecución.**
*   **[x] Tarea 1.5: Establecer CI/CD Pipeline Básico (Placeholder).**
*   **[x] Tarea 1.6: Análisis de `project-prd.md` y Planificación Detallada Inicial del MVP.**
*   **[x] Tarea 1.7: Detallar UC-001 (Registro y Login de Usuario) en `usecases.md`.**

---
### Fase 2: Implementación MVP (En Planificación Detallada / Próxima a Iniciar)

*   **Sub-Fase 2.1: Autenticación y Autorización (RBAC)**

    *   **Tarea 2.1.1: UC-001 - Registro del Primer Administrador y Login General**
        *   [x] **SubTarea 2.1.1.1:** Detallar UC-001 en `usecases.md` (Completada).
        *   [ ] **SubTarea 2.1.1.2:** Backend - Diseño e implementación del modelo de datos (Usuario, Rol, Permiso).
        *   [ ] **SubTarea 2.1.1.3:** Backend - Implementación del endpoint de registro del primer administrador.
        *   [ ] **SubTarea 2.1.1.4:** Backend - Implementación del endpoint de login (generación JWT, hashing contraseñas).
        *   [ ] **SubTarea 2.1.1.5:** Backend - Escritura de pruebas unitarias y de integración para autenticación (registro y login).
        *   [ ] **SubTarea 2.1.1.6:** Frontend - Diseño e implementación de la vista/componente de Login.
        *   [ ] **SubTarea 2.1.1.7:** Frontend - Implementación de la lógica de login (llamada API, almacenamiento de token).
        *   [ ] **SubTarea 2.1.1.8:** Frontend - Escritura de pruebas unitarias y e2e para el flujo de login.

    *   **Tarea 2.1.2: UC-002 - Gestión de Tokens y Sesión**
        *   [ ] **SubTarea 2.1.2.1:** Detallar UC-002 (Gestión de Tokens y Sesión) en `usecases.md`.
        *   [ ] **SubTarea 2.1.2.2:** Backend - Implementación de la validación de JWT (middleware/interceptor).
        *   [ ] **SubTarea 2.1.2.3:** Backend - Implementación del endpoint de logout (invalidación de token si aplica).
        *   [ ] **SubTarea 2.1.2.4:** Backend - Escritura de pruebas unitarias y de integración para gestión de tokens y sesión.
        *   [ ] **SubTarea 2.1.2.5:** Frontend - Implementación del manejo de expiración/refresh de token.
        *   [ ] **SubTarea 2.1.2.6:** Frontend - Implementación de la lógica de logout.
        *   [ ] **SubTarea 2.1.2.7:** Frontend - Implementación de la protección de rutas autenticadas (guards).
        *   [ ] **SubTarea 2.1.2.8:** Frontend - Escritura de pruebas unitarias y e2e para gestión de sesión.

    *   **Tarea 2.1.3: UC-003 - Roles y Permisos Básicos**
        *   [ ] **SubTarea 2.1.3.1:** Detallar UC-003 (Roles y Permisos Básicos) en `usecases.md`.
        *   [ ] **SubTarea 2.1.3.2:** Backend - Definición e implementación de roles (admin, deployer, reader) y permisos asociados.
        *   [ ] **SubTarea 2.1.3.3:** Backend - Implementación de la lógica de autorización en endpoints protegidos.
        *   [ ] **SubTarea 2.1.3.4:** Backend - Escritura de pruebas unitarias y de integración para RBAC.
        *   [ ] **SubTarea 2.1.3.5:** Frontend - Adaptación de la UI para mostrar/ocultar elementos según roles/permisos.
        *   [ ] **SubTarea 2.1.3.6:** Frontend - Escritura de pruebas para la UI basada en roles.

    *   **Tarea 2.1.4: UC-004 - Gestión Básica de Usuarios (Admin UI)**
        *   [ ] **SubTarea 2.1.4.1:** Detallar UC-004 (Gestión Básica de Usuarios por Admin) en `usecases.md`.
        *   [ ] **SubTarea 2.1.4.2:** Backend - Implementación de Endpoints CRUD para usuarios (accesibles solo por admin).
        *   [ ] **SubTarea 2.1.4.3:** Backend - Escritura de pruebas unitarias y de integración para la gestión de usuarios.
        *   [ ] **SubTarea 2.1.4.4:** Frontend - Diseño e implementación de vistas de admin para listar, crear, editar y (des)habilitar usuarios.
        *   [ ] **SubTarea 2.1.4.5:** Frontend - Escritura de pruebas unitarias y e2e para la UI de gestión de usuarios.

*   **Sub-Fase 2.2: Gestión de Repositorios Maven**

    *   **Tarea 2.2.1: UC-005 - Creación/Configuración de Repositorios Maven**
        *   [ ] **SubTarea 2.2.1.1:** Detallar UC-005 en `usecases.md`.
        *   [ ] **SubTarea 2.2.1.2:** Backend - Diseño e implementación del modelo de datos (Repositorio, TipoRepo, ConfigProxy).
        *   [ ] **SubTarea 2.2.1.3:** Backend - Implementación de API para CRUD de repositorios Maven.
        *   [ ] **SubTarea 2.2.1.4:** Backend - Implementación de la configuración de almacenamiento (sistema de archivos).
        *   [ ] **SubTarea 2.2.1.5:** Backend - Escritura de pruebas unitarias y de integración para gestión de repositorios.
        *   [ ] **SubTarea 2.2.1.6:** Frontend - Diseño e implementación de UI para CRUD de repositorios (admin).
        *   [ ] **SubTarea 2.2.1.7:** Frontend - Escritura de pruebas para UI de gestión de repositorios.

    *   **Tarea 2.2.2: UC-006 - Publicación de Artefactos Maven (`mvn deploy`)**
        *   [ ] **SubTarea 2.2.2.1:** Detallar UC-006 en `usecases.md`.
        *   [ ] **SubTarea 2.2.2.2:** Backend - Implementación del endpoint `PUT` para artefactos Maven (POMs, JARs, metadatos).
        *   [ ] **SubTarea 2.2.2.3:** Backend - Implementación de la validación de permisos (rol `deployer`).
        *   [ ] **SubTarea 2.2.2.4:** Backend - Implementación del almacenamiento de artefactos (estructura de directorios correcta).
        *   [ ] **SubTarea 2.2.2.5:** Backend - Implementación de la actualización de `maven-metadata.xml` (snapshots y releases).
        *   [ ] **SubTarea 2.2.2.6:** Backend - Escritura de pruebas de integración (simulando `mvn deploy` con cliente HTTP).

    *   **Tarea 2.2.3: UC-007 - Resolución de Artefactos Maven (Descarga)**
        *   [ ] **SubTarea 2.2.3.1:** Detallar UC-007 en `usecases.md`.
        *   [ ] **SubTarea 2.2.3.2:** Backend - Implementación del endpoint `GET` para artefactos Maven.
        *   [ ] **SubTarea 2.2.3.3:** Backend - Implementación de la lógica de repositorios proxy (cacheo de artefactos).
        *   [ ] **SubTarea 2.2.3.4:** Backend - Implementación de la lógica de repositorios group (búsqueda en múltiples repos).
        *   [ ] **SubTarea 2.2.3.5:** Backend - Implementación de la validación de permisos (rol `reader`).
        *   [ ] **SubTarea 2.2.3.6:** Backend - Escritura de pruebas de integración (simulando descarga con cliente HTTP).

    *   **Tarea 2.2.4: UC-008 - UI Básica para Repositorios y Artefactos Maven**
        *   [ ] **SubTarea 2.2.4.1:** Detallar UC-008 en `usecases.md`.
        *   [ ] **SubTarea 2.2.4.2:** Frontend - Diseño e implementación de vista para listar repositorios.
        *   [ ] **SubTarea 2.2.4.3:** Frontend - Diseño e implementación de vista para navegar/listar artefactos en un repositorio.
        *   [ ] **SubTarea 2.2.4.4:** Frontend - Escritura de pruebas unitarias y e2e para la UI de exploración de Maven.

*   **Sub-Fase 2.3: Rendimiento, Estabilidad y Documentación (MVP)**
    *   **Tarea 2.3.1: Optimización y Pruebas de Carga Básicas**
        *   [ ] **SubTarea 2.3.1.1:** Backend - Identificar y optimizar rutas críticas de API (upload/download).
        *   [ ] **SubTarea 2.3.1.2:** Realizar pruebas de carga simples (ej. k6, Gatling) para endpoints clave.
    *   **Tarea 2.3.2: Logging y Monitorización Básicos**
        *   [ ] **SubTarea 2.3.2.1:** Backend - Configurar e implementar logging estructurado y centralizado (si es posible).
        *   [ ] **SubTarea 2.3.2.2:** Frontend - Implementar manejo de errores global y logging básico de errores.
    *   **Tarea 2.3.3: Documentación Inicial del MVP**
        *   [ ] **SubTarea 2.3.3.1:** Redactar guía de inicio rápido (instalación, admin inicial, uso básico).
        *   [ ] **SubTarea 2.3.3.2:** Generar y publicar documentación de API endpoints principales (Swagger/OpenAPI).

---
### Fase 3: Post-MVP (Iteración 1) (Planificada)
*   (Las tareas de esta fase se detallarán con subtareas cuando nos acerquemos a ella)
*   **Sub-Fase 3.1: Soporte para Repositorios npm**
*   **Sub-Fase 3.2: Mejoras de Seguridad**
*   **Sub-Fase 3.3: Mejoras UI/UX y Administración**

---
### Fase 4: Post-MVP (Iteración 2) (Planificada)
*   (Las tareas de esta fase se detallarán con subtareas cuando nos acerquemos a ella)
*   **Sub-Fase 4.1: Soporte para Repositorios Docker**
*   **Sub-Fase 4.2: Integraciones y Escalabilidad**

---
### Fase 5: Visión a Largo Plazo (Conceptual)
*   (Las tareas de esta fase se detallarán con subtareas cuando nos acerquemos a ella)

## 5. Roadmap Visual (Gantt Simplificado - Conceptual)

```mermaid
gantt
    dateFormat  YYYY-MM-DD
    title Roadmap Hodei Packages - Detallado MVP
    excludes    weekends

    section Planificación y Configuración
    Análisis PRD y Banco Memoria :done, p1, 2025-06-01, 1d
    Definir Scaffolding         :done, p2, after p1, 1d

    section Scaffolding (Fase 1)
    Estructura Base Monorepo    :done, task1.1, 2025-06-02, 1d
    Init Backend Kotlin         :done, task1.2, after task1.1, 1d
    Init Frontend Angular       :done, task1.3, after task1.2, 1d
    Config Linters/Scripts      :done, task1.4, after task1.3, 1d 
    CI/CD Placeholder           :done, task1.5, after task1.4, 1d
    Planificación MVP Inicial   :done, task1.6, after task1.5, 1d
    Detallar UC-001             :done, task1.7, after task1.6, 1d
    Refinar Plan MVP Subtareas  :crit, active, task1.8, after task1.7, 1d


    section MVP - Auth & RBAC (Sub-Fase 2.1)
    UC-001 Dev BE (Modelo, Endpoints) :planned, mvp_auth_uc1_devbe, 2025-06-04, 3d
    UC-001 Test BE                    :planned, mvp_auth_uc1_testbe, after mvp_auth_uc1_devbe, 2d
    UC-001 Dev FE (Vista, Lógica)     :planned, mvp_auth_uc1_devfe, after mvp_auth_uc1_testbe, 2d
    UC-001 Test FE                    :planned, mvp_auth_uc1_testfe, after mvp_auth_uc1_devfe, 1d
    
    UC-002 Detalle                    :planned, mvp_auth_uc2_detail, after mvp_auth_uc1_testfe, 1d
    UC-002 Dev BE                     :planned, mvp_auth_uc2_devbe, after mvp_auth_uc2_detail, 2d
    UC-002 Test BE                    :planned, mvp_auth_uc2_testbe, after mvp_auth_uc2_devbe, 1d
    UC-002 Dev FE                     :planned, mvp_auth_uc2_devfe, after mvp_auth_uc2_testbe, 2d
    UC-002 Test FE                    :planned, mvp_auth_uc2_testfe, after mvp_auth_uc2_devfe, 1d

    UC-003 Detalle                    :planned, mvp_auth_uc3_detail, after mvp_auth_uc2_testfe, 1d
    % ... (más subtareas para UC-003 y UC-004)

    section MVP - Gestión Repos Maven (Sub-Fase 2.2)
    UC-005 Detalle                    :planned, mvp_maven_uc5_detail, after mvp_auth_uc3_detail, 1d 
    % ... (más subtareas para UC-005, UC-006, UC-007, UC-008)

    section MVP - Estabilidad y Docs (Sub-Fase 2.3)
    % ... (subtareas para Optimización, Logging, Documentación)
    
    %% Las fases Post-MVP se mantienen a alto nivel en Gantt por ahora
    section Post-MVP Iteración 1 (Fase 3)
    Soporte Repos npm           :planned, p_mvp1_npm, 2025-08-01, 15d

    section Post-MVP Iteración 2 (Fase 4)
    Soporte Repos Docker        :planned, p_mvp2_docker, 2025-09-01, 15d

```

Este documento se actualizará continuamente para reflejar el progreso real.
