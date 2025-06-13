# Project Brief: Hodei Packages

## 1. Objetivos Principales

El objetivo de este proyecto es desarrollar un gestor de artefactos universal llamado **Hodei Packages**. El sistema debe ser capaz de alojar y servir paquetes para múltiples tecnologías, comenzando con Maven, npm y Python (PyPI). La API debe ser compatible con las herramientas cliente nativas de cada ecosistema (ej. `mvn`, `npm`, `pip`, `twine`).

## 2. Requisitos Clave

- **Soporte Multi-Repositorio**: El sistema debe permitir la creación y gestión de múltiples repositorios, cada uno con un identificador único (`repoId`).
- **Compatibilidad de Formatos**:
  - **Maven**: Subida (`PUT`) y descarga (`GET`) de artefactos (`.jar`, `.pom`) y metadatos (`maven-metadata.xml`).
  - **npm**: Publicación (`PUT`) y descarga (`GET`) de paquetes, y autenticación (`POST /-/npm/v1/login`).
  - **PyPI**: Subida (`POST`) de paquetes (`.whl`, `.tar.gz`) y listado de paquetes (`GET /simple/{package}/`).
- **Autenticación y Seguridad**:
  - Implementar autenticación Basic Auth para Maven y PyPI.
  - Implementar autenticación Bearer Token (JWT) para npm.
  - El acceso de lectura a los artefactos será generalmente público, pero la escritura (publicación/subida) siempre requerirá autenticación.
- **Búsqueda y Descarga Centralizada**:
  - Un endpoint `/search` para buscar artefactos en todos los repositorios.
  - Un endpoint `/content/{hash}` para descargar un artefacto directamente usando su hash, independientemente del repositorio.

## 3. Impacto BDD y Calidad

Los objetivos y requisitos definidos en este documento serán la base para la creación de Casos de Uso y escenarios BDD. Cada funcionalidad (ej. "Subir un artefacto Maven") se definirá como un comportamiento observable y se validará con escenarios Gherkin, asegurando que la implementación sea robusta, correcta y cumpla con las expectativas del negocio.