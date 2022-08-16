resource "kubernetes_deployment" "data_producer" {

  depends_on = [kubernetes_service.kafka, kubernetes_deployment.avro_publisher]

  metadata {
    name = "data-producer"
    labels = {
      type = "test"
      app  = "data-producer"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "test"
        app  = "data-producer"
      }
    }

    strategy {
      type = "Recreate"
    }

    template {
      metadata {
        labels = {
          type = "test"
          app  = "data-producer"
        }
      }

      spec {
        container {
          image = "gcr.io/${var.project_id}/${var.data_producer_image_name}:latest"
          name  = "data-producer"

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
