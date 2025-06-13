# Tech Context: Hodei Packages

## 1. Stack Tecnológico

- **Lenguaje**: Kotlin
- **Framework Backend**: Ktor
- **Sistema de Build**: Gradle con Kotlin DSL
- **Base de Datos**: PostgreSQL (a través de un adaptador de persistencia)
- **Almacenamiento de Artefactos**: Sistema de archivos local (a través de un adaptador de almacenamiento)

## 2. Herramientas de Calidad y Pruebas

- **Framework de Pruebas**: Kotest
- **Mocks**: MockK
- **Aserciones**: AssertJ o las aserciones de Kotest.
- **Pruebas BDD**: Se utilizará Kotest para escribir pruebas que sigan la estructura de los escenarios Gherkin (Given, When, Then).

## 3. Configuración de Desarrollo

- **Linter/Formateador**: Se utilizará `ktlint` para mantener un estilo de código consistente y limpio en todo el proyecto. La configuración de Gradle incluirá una tarea para verificar y aplicar el formato automáticamente.