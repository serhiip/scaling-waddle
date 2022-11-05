resource "kubernetes_deployment" "data_consumer" {

  depends_on = [kubernetes_service.kafka]

  metadata {
    name = "data-consumer"
    labels = {
      type = "test"
      app  = "data-consumer"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "test"
        app  = "data-consumer"
      }
    }

    strategy {
      type = "Recreate"
    }

    template {
      metadata {
        labels = {
          type = "test"
          app  = "data-consumer"
        }
      }

      spec {
        container {
          image = "gcr.io/${var.project_id}/${var.data_consumer_image_name}:latest"
          name  = "data-consumer"

          resources {
            limits = {
              cpu    = "250m"
              memory = "256Mi"
            }
            requests = {
              cpu    = "125m"
              memory = "50Mi"
            }
          }

          env {
            name  = "KAFKA_HOST"
            value = kubernetes_service.kafka.metadata.0.name
          }

          env {
            name  = "KAFKA_PORT"
            value = var.kafka_port
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
