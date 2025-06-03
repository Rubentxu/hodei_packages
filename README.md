# Hodei Packages - Monorepo

Este es el monorepo para el proyecto Hodei Packages, un sistema de gestión de artefactos de alto rendimiento.

## Estructura del Repositorio

*   `/backend`: Contiene el código fuente del backend desarrollado en Kotlin (multi-módulo con Gradle).
*   `/frontend`: Contiene el código fuente del frontend desarrollado con Angular.
*   `/libs`: (Futuro) Contendrá bibliotecas compartidas entre diferentes partes del proyecto.
*   `/docs`: Contiene documentación del proyecto (arquitectura, decisiones de diseño, guías de usuario, etc.). El `memory-bank` es una subsección crítica aquí.
*   `/scripts`: Contiene scripts de utilidad para el desarrollo, build, despliegue, etc.
*   `/memory-bank`: Documentación viva del proyecto, esencial para el desarrollo continuo y la comprensión del sistema.

## Cómo Empezar

Consulte los `README.md` específicos dentro de los directorios `backend/` y `frontend/` para obtener instrucciones sobre cómo construir, ejecutar y probar cada parte del proyecto.

Los scripts de alto nivel para tareas comunes (linting, formatting, building, running, testing) se pueden encontrar en el archivo `package.json` en la raíz de este monorepo (a ser creado en Tarea 1.4).

## Desarrollo Local

1. Iniciar servicios:
```bash
docker-compose up -d
gradle :backend:app:run
```

2. Ejecutar tests:
```bash
gradle test
```

3. Acceder a:
- API: http://localhost:8080
- PGAdmin: http://localhost:5050

## Banco de Memoria

La documentación evolutiva y el contexto del proyecto se mantienen en el directorio `memory-bank/`. Es crucial consultarlo y mantenerlo actualizado.

## Requisitos Previos

*   JDK (versión especificada en `backend/build.gradle.kts` o `techContext.md`)
*   Node.js y npm (versiones especificadas en `frontend/package.json` o `techContext.md`)
*   Angular CLI (versión especificada en `techContext.md`)
*   Git

(Se añadirán más detalles a medida que el proyecto evolucione)
