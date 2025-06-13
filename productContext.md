# Product Context: Hodei Packages

## 1. ¿Por qué existe este proyecto?

En el desarrollo de software moderno, los equipos trabajan con múltiples tecnologías simultáneamente (ej. backend en Java/Kotlin, frontend en TypeScript, scripts en Python). Cada ecosistema tiene su propio sistema de gestión de paquetes (Maven Central, npm Registry, PyPI). Gestionar el acceso, la seguridad y el almacenamiento de artefactos privados a través de diferentes plataformas puede ser complejo y costoso.

Hodei Packages nace para resolver este problema, proporcionando una **solución unificada y auto-alojada** para la gestión de artefactos. Centraliza el almacenamiento, la seguridad y el acceso a paquetes de diferentes formatos, simplificando la infraestructura de CI/CD y desarrollo.

## 2. ¿Cómo debería funcionar?

A alto nivel, Hodei Packages funcionará como un servidor de artefactos políglota. Los desarrolladores y los sistemas de integración continua interactuarán con él utilizando las herramientas estándar a las que ya están acostumbrados (`mvn`, `npm`, `pip`).

- **Para un desarrollador Java/Kotlin**: Configurará su `settings.xml` para apuntar al repositorio Maven de Hodei. Podrá desplegar y consumir artefactos como si estuviera usando Nexus o Artifactory.
- **Para un desarrollador de frontend**: Usará `npm login` para autenticarse contra Hodei y luego `npm publish` para subir sus paquetes privados.
- **Para un científico de datos o desarrollador Python**: Configurará su `.pypirc` y usará `twine upload` para publicar sus librerías. `pip install` funcionará de forma transparente.

La experiencia de usuario debe ser **fluida y transparente**. El objetivo es que el desarrollador no necesite aprender una nueva herramienta cliente, sino simplemente configurar la que ya usa para que apunte a Hodei Packages.