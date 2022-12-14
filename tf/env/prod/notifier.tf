resource "kubernetes_deployment" "notifier" {

  depends_on = [kubernetes_service.kafka]

  metadata {
    name = "notifier"
    labels = {
      type = "test"
      app  = "notifier"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "test"
        app  = "notifier"
      }
    }

    strategy {
      type = "Recreate"
    }

    template {
      metadata {
        labels = {
          type = "test"
          app  = "notifier"
        }
      }

      spec {
        container {
          image             = "gcr.io/${var.project_id}/${var.notifier_image_name}:${var.notifier_tag}"
          image_pull_policy = "Always"
          name              = "notifier"

          resources {
            limits = {
              cpu    = "1000m"
              memory = "512Mi"
            }
            requests = {
              cpu    = "125m"
              memory = "50Mi"
            }
          }

          volume_mount {
            mount_path = "/tmp"
            name       = "ephemeral"
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

        volume {
          name = "ephemeral"
          empty_dir {}
        }
      }
    }
  }
}
