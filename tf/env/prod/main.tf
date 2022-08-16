locals {
  cluster_name = "kafka-tests"
}

data "google_client_config" "default" {}

provider "kubernetes" {
  host                   = "https://${module.k8s_cluster.endpoint}"
  token                  = data.google_client_config.default.access_token
  cluster_ca_certificate = base64decode(module.k8s_cluster.ca_certificate)
}

module "vpc" {
  source = "../../modules/vpc/"

  vpc_name        = "${local.cluster_name}-vpc"
  vpc_subnet_name = "${local.cluster_name}-vpc-subnet"
  region          = var.region
  zones           = var.zones
  project_id      = var.project_id
}

module "k8s_cluster" {
  depends_on = [module.vpc]

  source = "../../modules/k8s/"

  region                 = var.region
  project_id             = var.project_id
  zones                  = var.zones
  vpc_network_name       = module.vpc.name
  vpc_subnet_name        = module.vpc.subnet_name
  cluster_name           = local.cluster_name
  ip_range_name_pods     = module.vpc.ip_range_name_pods
  ip_range_name_services = module.vpc.ip_range_name_services
  service_account        = var.service_account
}

output "cluster_name" {
  value = module.k8s_cluster.name
}

output "cluster_endpoint" {
  value     = module.k8s_cluster.endpoint
  sensitive = true
}
