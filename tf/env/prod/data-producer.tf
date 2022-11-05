resource "kubernetes_deployment" "data_producer" {

  depends_on = [kubernetes_service.kafka]

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

        # volume {
        #   persistent_volume_claim {
        #     claim_name = "${kubernetes_persistent_volume_claim.producer.metadata.0.name}"
        #   }
        # }

        init_container {
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
            value = local.schema_registry_name
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
