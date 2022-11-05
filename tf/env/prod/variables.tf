variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "europe-west6"
}

variable "zones" {
  type    = list(string)
  default = ["europe-west6a"]
}

variable "service_account" {
  type = string
}

variable "avro_publisher_image_name" {
  type = string
}

variable "data_producer_image_name" {
  type = string
}

variable "data_consumer_image_name" {
  type = string
}

variable "notifier_image_name" {
  type = string
}

variable "schema_registry_port" {
  type    = number
  default = 8081
}

variable "kafka_port" {
  type  = number
  default = 9092
}

variable "notifier_tag" {
  type = string
  default = "latest"
}
