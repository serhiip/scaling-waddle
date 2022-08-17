locals {
  zookeeper_client_port         = 2181
  schema_registry_listener_port = 8082
  zookeeper_name                = "zookeeper"
  kafka_name                    = "kafkaaa" // this strange name is intentional, see https://github.com/confluentinc/schema-registry/issues/689#issuecomment-401046885
  schema_registry_name          = "schema-reg"
}

resource "kubernetes_namespace" "kafka_services" {
  metadata {
    name = "kafka-services"
  }
}

resource "kubernetes_deployment" "zookeeper" {
  metadata {
    name = local.zookeeper_name
    labels = {
      type = "kafka"
      app  = "zookeeper"
    }
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "kafka"
        app  = "zookeeper"
      }
    }

    template {
      metadata {
        labels = {
          type = "kafka"
          app  = "zookeeper"
        }
      }

      spec {
        container {
          image = "confluentinc/cp-zookeeper:5.3.0"
          name  = "zookeeper"

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

          port {
            container_port = local.zookeeper_client_port
            host_port      = local.zookeeper_client_port
          }

          env {
            name  = "ZOOKEEPER_CLIENT_PORT"
            value = local.zookeeper_client_port
          }

          env {
            name  = "ZOOKEEPER_TICK_TIME"
            value = 2000
          }
        }
      }
    }
  }
}


resource "kubernetes_service" "zookeeper" {
  metadata {
    name      = local.zookeeper_name
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }
  spec {
    selector = {
      type = kubernetes_deployment.zookeeper.metadata.0.labels.type
      app  = kubernetes_deployment.zookeeper.metadata.0.labels.app
    }

    session_affinity = "ClientIP"

    port {
      port        = local.zookeeper_client_port
      target_port = local.zookeeper_client_port
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_deployment" "kafka" {

  depends_on = [kubernetes_service.zookeeper, kubernetes_deployment.zookeeper]

  metadata {
    name = local.kafka_name
    labels = {
      type = "kafka"
      app  = "kafka"
    }
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "kafka"
        app  = "kafka"
      }
    }

    template {
      metadata {
        labels = {
          type = "kafka"
          app  = "kafka"
        }
      }

      spec {
        container {
          image = "confluentinc/cp-enterprise-kafka:5.3.0"
          name  = "kafka"

          resources {
            limits = {
              cpu    = "1"
              memory = "512Mi"
            }
            requests = {
              cpu    = "125m"
              memory = "50Mi"
            }
          }

          port {
            container_port = var.kafka_port
            host_port      = var.kafka_port
          }

          env {
            name  = "KAFKA_BROKER_ID"
            value = 1
          }

          env {
            name  = "KAFKA_ZOOKEEPER_CONNECT"
            value = "${local.zookeeper_name}:${local.zookeeper_client_port}"
          }

          env {
            name  = "KAFKA_ADVERTISED_LISTENERS"
            value = "PLAINTEXT://${local.kafka_name}:${var.kafka_port}"
          }

          env {
            name  = "KAFKA_AUTO_CREATE_TOPICS_ENABLE"
            value = "PLAINTEXT://${local.kafka_name}:${var.kafka_port}"
          }

          env {
            name  = "KAFKA_AUTO_CREATE_TOPICS_ENABLE"
            value = "true"
          }

          env {
            name  = "KAFKA_DELETE_TOPIC_ENABLE"
            value = "true"
          }

          env {
            name  = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR"
            value = 1
          }

          env {
            name  = "KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS"
            value = 100
          }

          env {
            name  = "BITNAMI_DEBUG"
            value = "true"
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "kafka" {

  metadata {
    name      = local.kafka_name
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }
  spec {
    selector = {
      type = kubernetes_deployment.kafka.metadata.0.labels.type
      app  = kubernetes_deployment.kafka.metadata.0.labels.app
    }

    port {
      port        = var.kafka_port
      target_port = var.kafka_port
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_deployment" "schema_registry" {

  depends_on = [kubernetes_service.kafka, kubernetes_deployment.kafka]

  metadata {
    name = local.schema_registry_name
    labels = {
      type = "kafka"
      app  = "schema-registry"
    }
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        type = "kafka"
        app  = "schema-registry"
      }
    }

    template {
      metadata {
        labels = {
          type = "kafka"
          app  = "schema-registry"
        }
      }

      spec {
        container {
          image = "confluentinc/cp-schema-registry:5.3.0"
          name  = "schema-registry"

          resources {
            limits = {
              cpu    = "500m"
              memory = "256Mi"
            }
            requests = {
              cpu    = "125m"
              memory = "50Mi"
            }
          }

          port {
            container_port = var.schema_registry_port
            host_port      = var.schema_registry_port
          }

          env {
            name  = "KAFKA_BROKER_ID"
            value = 1
          }

          env {
            name  = "SCHEMA_REGISTRY_HOST_NAME"
            value = "schema-registry"
          }

          env {
            name  = "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS"
            value = "${local.kafka_name}:${var.kafka_port}"
          }

          env {
            name  = "SCHEMA_REGISTRY_LISTENERS"
            value = "http://0.0.0.0:${var.schema_registry_port}"
          }

        }
      }
    }
  }
}

resource "kubernetes_service" "schema_registry" {
  metadata {
    name      = local.schema_registry_name
    namespace = kubernetes_namespace.kafka_services.metadata.0.name
  }
  spec {
    selector = {
      type = kubernetes_deployment.schema_registry.metadata.0.labels.type
      app  = kubernetes_deployment.schema_registry.metadata.0.labels.app
    }

    port {
      name        = "listener-port"
      port        = local.schema_registry_listener_port
      target_port = local.schema_registry_listener_port
    }

    port {
      name        = "port"
      port        = var.schema_registry_port
      target_port = var.schema_registry_port
    }


    type = "ClusterIP"
  }
}
