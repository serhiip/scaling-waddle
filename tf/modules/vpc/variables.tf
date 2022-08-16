variable "vpc_name" {
  type        = string
  description = "Cluster location"
}

variable "project_id" {
  type = string
}

variable "vpc_subnet_name" {
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
