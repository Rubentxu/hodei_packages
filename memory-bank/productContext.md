# Product Context: Sistema de Repositorio de Artefactos

## 1. ¿Por qué existe este proyecto? (El Problema)

En el desarrollo de software moderno, la gestión eficiente de artefactos (bibliotecas compiladas, paquetes, imágenes de contenedores, etc.) es crucial. Los equipos necesitan un lugar centralizado, seguro y rápido para:
*   Almacenar los artefactos que producen.
*   Consumir artefactos de otros equipos o de fuentes públicas de forma controlada.
*   Asegurar la reproducibilidad de las compilaciones.
*   Integrar con herramientas de CI/CD.

Las soluciones existentes pueden ser costosas, complejas de configurar, o no ofrecer el rendimiento o la flexibilidad deseada para arquitecturas nativas de la nube o stacks tecnológicos específicos. Este proyecto nace de la necesidad de una solución de repositorio de artefactos que sea:
*   **De alto rendimiento:** Especialmente optimizada para velocidad y bajo consumo de recursos (gracias a Kotlin/Native).
*   **Moderna:** Construida con tecnologías actuales (Kotlin, Angular) y arquitecturas (Hexagonal, Event-Driven).
*   **Escalable:** Diseñada para la nube y entornos como Kubernetes.
*   **Extensible:** Fácil de adaptar para nuevos tipos de artefactos.
*   **Segura:** Con un fuerte énfasis en el control de acceso y la protección de los binarios.

## 2. ¿Cómo debería funcionar? (La Solución - Alto Nivel)

El sistema funcionará como un servidor central al que los desarrolladores y sistemas de CI/CD pueden conectarse para:

*   **Publicar (Subir) Artefactos:** Los desarrolladores o pipelines de CI podrán subir artefactos (ej. un `.jar` de Maven, un paquete `.tgz` de npm) a repositorios específicos. El sistema validará metadatos y almacenará el artefacto y su información.
*   **Resolver (Descargar) Artefactos:** Las herramientas de construcción (Maven, npm) y los desarrolladores podrán descargar artefactos desde el repositorio para usarlos como dependencias en sus proyectos.
*   **Gestionar Repositorios:** Los administradores podrán crear, configurar y eliminar repositorios. Cada repositorio podrá tener políticas específicas (ej. tipo de artefacto, permisos de acceso).
*   **Gestionar Usuarios y Permisos:** Los administradores podrán gestionar usuarios y asignarles roles que definan qué acciones pueden realizar sobre qué repositorios (RBAC).
*   **Navegar e Inspeccionar:** A través de una interfaz web, los usuarios podrán navegar por los repositorios, buscar artefactos, ver sus metadatos e historial.

La interacción se realizará principalmente a través de:
*   **API RESTful:** Para la integración con herramientas de construcción (Maven, npm CLI) y scripts de automatización.
*   **Interfaz Web (Angular):** Para la administración del sistema, la gestión de usuarios/roles, la navegación de repositorios y la visualización de información.

## 3. Objetivos de la Experiencia del Usuario (UX)

*   **Para Administradores:**
    *   Configuración intuitiva del sistema y los repositorios.
    *   Gestión sencilla de usuarios, roles y permisos.
    *   Visibilidad clara del estado del sistema, uso de almacenamiento y actividad.
    *   Herramientas efectivas para el mantenimiento y la auditoría.
*   **Para Desarrolladores/Usuarios:**
    *   Integración transparente con sus herramientas de desarrollo (Maven, npm).
    *   Proceso de publicación de artefactos claro y directo.
    *   Descarga rápida y fiable de dependencias.
    *   Fácil navegación y búsqueda de artefactos a través de la UI.
    *   Documentación clara sobre cómo usar la API y configurar clientes.
*   **General:**
    *   La interfaz web debe ser moderna, responsiva y fácil de usar.
    *   Los mensajes de error deben ser claros y útiles.
    *   El rendimiento debe ser percibido como rápido tanto en la API como en la UI.
