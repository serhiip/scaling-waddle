resource "kubernetes_deployment" "avro_publisher" {
  metadata {
    name = "avro-publisher"
    labels = {
      type = "test"
      app  = "avro-publisher"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "test"
        app  = "avro-publisher"
      }
    }

    strategy {
      type = "Recreate"
    }

    template {
      metadata {
        labels = {
          type = "test"
          app  = "avro-publisher"
        }
      }

      spec {
        container {
          image = "gcr.io/${var.project_id}/${var.avro_publisher_image_name}:latest"
          name  = "avro-publisher"

          resources {
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
            requests = {
              cpu    = "125m"
              memory = "50Mi"
            }
          }

          env {
            name  = "SCHEMA_REGISTRY_HOST"
            value = kubernetes_service.schema_registry.metadata.0.name
          }

          env {
            name  = "SCHEMA_REGISTRY_PORT"
            value = var.schema_registry_port
          }
        }
      }
    }
  }
}
