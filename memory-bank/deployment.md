# Deployment: Estrategia de Despliegue en Kubernetes

Este documento describe la estrategia de despliegue del Sistema Hodei Packages en un entorno Kubernetes.

## 1. Componentes a Desplegar

Los principales componentes que se desplegarán como aplicaciones contenerizadas en Kubernetes son:

1.  **Backend Service (Kotlin/Native o JVM):** La aplicación principal que expone la API REST y maneja la lógica de negocio.
2.  **Frontend Service (Angular + Nginx):** La aplicación SPA de Angular servida por Nginx.
3.  **PostgreSQL Database:** La base de datos para los metadatos. Podría ser un StatefulSet en K8s o un servicio de base de datos gestionado externo (ej. AWS RDS, Google Cloud SQL).
4.  **Message Broker (Kafka/RabbitMQ):** El sistema de mensajería para la arquitectura dirigida por eventos. Similar a PostgreSQL, podría ser un StatefulSet o un servicio gestionado.
5.  **(Opcional) Almacenamiento de Objetos S3:** Si se usa S3, este es un servicio externo y no se despliega directamente en K8s, pero el backend necesitará configuración para accederlo.

## 2. Estrategia de Despliegue en Kubernetes

```mermaid
C4Context
  title Diagrama de Despliegue en Kubernetes (Simplificado)

  Enterprise_Boundary(enterprise, "Organización") {
    Boundary(k8s_cluster, "Cluster Kubernetes", "") {
      System_Boundary(hodei_ns, "Namespace: hodei-packages") {
        ContainerDb(db, "PostgreSQL", "StatefulSet / External Service", "Almacena metadatos")
        ContainerBroker(broker, "Kafka/RabbitMQ", "StatefulSet / External Service", "Bus de eventos")

        Deployment(backend_deploy, "Backend Deployment", "") {
          Container(backend_app, "Hodei Backend", "Kotlin, Ktor", "API REST, Lógica de negocio")
        }
        Service(backend_svc, "Backend Service", "ClusterIP")

        Deployment(frontend_deploy, "Frontend Deployment", "") {
          Container(frontend_app, "Hodei Frontend", "Angular, Nginx", "Interfaz de Usuario Web")
        }
        Service(frontend_svc, "Frontend Service", "ClusterIP")

        Ingress(ingress, "Ingress Controller", "Nginx Ingress / Traefik")
      }
    }
    System_Ext(s3, "Amazon S3", "Almacenamiento de objetos para binarios de artefactos (opcional)")
  }

  Rel(backend_app, db, "Lee/Escribe metadatos", "JDBC")
  Rel(backend_app, broker, "Publica/Consume eventos", "AMQP/Kafka")
  Rel(backend_app, s3, "Lee/Escribe binarios", "HTTPS/S3 API")
  Rel(frontend_app, backend_svc, "Llama API REST", "HTTPS")
  Rel(ingress, frontend_svc, "Ruta /ui", "HTTPS")
  Rel(ingress, backend_svc, "Ruta /api", "HTTPS")

  UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

**Detalles de los Recursos de Kubernetes:**

*   **Namespace:** Se creará un namespace dedicado (ej. `hodei-packages`) para aislar los recursos del sistema.
*   **Deployments:** Se usarán `Deployments` para gestionar las aplicaciones backend y frontend, permitiendo actualizaciones rolling, rollbacks y escalado de réplicas.
*   **Services:**
    *   `ClusterIP` services para exponer internamente el backend y el frontend a otros componentes dentro del cluster (como el Ingress Controller).
*   **Ingress:** Un `Ingress Controller` (ej. Nginx Ingress, Traefik) gestionará el acceso externo a las aplicaciones, manejando rutas (ej. `/api` para el backend, `/` o `/ui` para el frontend) y terminación SSL/TLS.
*   **StatefulSets (para DB y Broker):** Si PostgreSQL y Kafka/RabbitMQ se despliegan dentro del cluster, se usarán `StatefulSets` para gestionar su estado persistente y identidades de red estables.
*   **PersistentVolumeClaims (PVCs):** Para el almacenamiento persistente de la base de datos, el broker de mensajes y el almacenamiento local de artefactos (si se usa).
*   **ConfigMaps y Secrets:** Para gestionar la configuración de la aplicación (ej. URLs de base de datos, credenciales, configuraciones de S3) y los secretos (contraseñas, claves API) de forma segura.
*   **Horizontal Pod Autoscaler (HPA):** Se configurarán HPAs para el backend (y potencialmente el frontend si es necesario) para escalar automáticamente el número de réplicas basado en métricas como el uso de CPU o memoria.
*   **NetworkPolicies:** Para restringir el tráfico de red entre pods por seguridad.
*   **ResourceQuotas y LimitRanges:** Para controlar el consumo de recursos en el namespace.

## 3. Configuración y Gestión

*   **Helm Charts:** Se crearán Helm charts para empaquetar y gestionar el despliegue de todos los componentes de la aplicación de forma versionada y configurable.
*   **CI/CD:** El pipeline de CI/CD (ver `techContext.md`) será responsable de construir las imágenes Docker, pushearlas a un registry y actualizar los despliegues en Kubernetes (ej. usando `helm upgrade`).

## 4. Almacenamiento de Artefactos

*   **Sistema de Ficheros Local:** Si se usa almacenamiento local, se montará un `PersistentVolume` en los pods del backend. Este volumen debe ser accesible en modo `ReadWriteMany` si hay múltiples réplicas del backend que necesitan acceder a los mismos archivos (ej. usando NFS, GlusterFS, o soluciones de almacenamiento cloud específicas para K8s).
*   **Amazon S3 (o compatible):** El backend se configurará con las credenciales y el endpoint del bucket S3. Esta opción es generalmente preferida para escalabilidad y durabilidad en la nube.

## 5. Consideraciones de Alta Disponibilidad (HA)

*   Ejecutar múltiples réplicas de los pods del backend y frontend.
*   Asegurar que la base de datos y el message broker estén configurados para HA (ej. clúster PostgreSQL, clúster Kafka).
*   Usar un Ingress Controller con múltiples réplicas.
*   Distribuir los nodos del cluster K8s en múltiples zonas de disponibilidad (si el entorno lo permite).

Este plan de despliegue es una guía inicial y se refinará a medida que el proyecto avance y se tomen decisiones más específicas sobre la infraestructura.
