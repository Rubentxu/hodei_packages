# Project Structure: Hodei Packages

Esta es una representación de la estructura de directorios del proyecto, enfocada en el backend. La arquitectura es hexagonal, pero la capa de la API (adaptador de entrada Ktor) se encuentra en el módulo `app`.

```
backend/
├── domain/                     # Lógica y entidades de negocio puras
│   └── src/main/kotlin/dev/rubentxu/hodei/packages/domain/
├── application/                # Servicios de aplicación y puertos (interfaces)
│   └── src/main/kotlin/dev/rubentxu/hodei/packages/application/
├── infrastructure/             # Adaptadores de salida (BBDD, ficheros, etc.)
│   └── src/main/kotlin/dev/rubentxu/hodei/packages/infrastructure/
├── app/                        # Módulo de ensamblaje y API (Adaptador de entrada)
│   └── src/main/kotlin/dev/rubentxu/hodei/packages/app/
│       ├── Application.kt      # Punto de entrada principal de Ktor
│       ├── plugins/            # Configuración de plugins de Ktor
│       │   ├── Routing.kt      # Configuración central de enrutamiento
│       │   └── Security.kt     # Configuración de seguridad
│       └── features/           # Rutas de la API organizadas por funcionalidad
│           ├── auth/           # Feature de autenticación
│           │   └── routes/
│           │       └── AuthRoutes.kt
│           └── packages/       # Feature de gestión de paquetes (A IMPLEMENTAR)
│               └── routes/
│                   ├── MavenRoutes.kt
│                   ├── NpmRoutes.kt
│                   └── PypiRoutes.kt
└── build.gradle.kts            # Script de build principal del backend
```
