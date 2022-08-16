variable "project_id" {
  type = string
}

variable "service_account" {
  type = string
}

variable "region" {
  description = "GCP Compute Region"
  type        = string
}

variable "zones" {
  description = "availability zones"
  type        = list(string)
  default     = ["europe-west6-a"]
}

variable "vpc_network_name" {
  description = "VPC Network Name"
  type        = string
}

variable "vpc_subnet_name" {
  description = "VPC Subnet Name"
  type        = string
}

variable "cluster_name" {
  type = string
}

variable "ip_range_name_pods" {
  type = string
}

variable "ip_range_name_services" {
  type = string
}
