# Use Cases & BDD Scenarios: Hodei Packages

## Caso de Uso 1: Gestión de Artefactos Maven

### Escenario 1.1: Subir un artefacto Maven con éxito

**Given** un repositorio Maven llamado "libs-release" existe en el sistema
**And** un usuario autenticado con credenciales válidas
**When** el usuario sube el artefacto "com.example:my-app:1.0.0:my-app-1.0.0.jar" al repositorio "libs-release"
**Then** el sistema responde con un código de estado 201 (Created)
**And** el artefacto "my-app-1.0.0.jar" y su POM "my-app-1.0.0.pom" se almacenan en el sistema asociados al repositorio "libs-release"

### Escenario 1.2: Intentar subir un artefacto sin autenticación

**Given** un repositorio Maven llamado "libs-release" existe
**When** un cliente intenta subir un artefacto sin proporcionar credenciales
**Then** el sistema responde con un código de estado 401 (Unauthorized)

## Caso de Uso 2: Gestión de Paquetes npm

### Escenario 2.1: Publicar un paquete npm con éxito

**Given** un repositorio npm llamado "npm-private" existe
**And** un usuario se ha autenticado usando `npm login` y ha obtenido un Bearer Token válido
**When** el usuario publica el paquete "my-private-lib" usando `npm publish`
**Then** el sistema responde con un código de estado 201 (Created)
**And** el paquete "my-private-lib" se almacena en el sistema

## Caso de Uso 3: Gestión de Paquetes PyPI

### Escenario 3.1: Subir un paquete Python con éxito

**Given** un repositorio PyPI llamado "pypi-internal" existe
**And** un usuario ha configurado sus credenciales en `.pypirc`
**When** el usuario sube el paquete "my-tool-0.1.0.whl" usando `twine upload`
**Then** el sistema responde con un código de estado 201 (Created)
**And** el paquete "my-tool-0.1.0.whl" se almacena en el sistema