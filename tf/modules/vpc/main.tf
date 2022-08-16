resource "google_compute_network" "vpc" {
  name                    = var.vpc_name
  project                 = var.project_id
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name                     = var.vpc_subnet_name
  region                   = var.region
  network                  = google_compute_network.vpc.id
  private_ip_google_access = true
  ip_cidr_range            = "10.10.0.0/24"
  project                  = var.project_id
  secondary_ip_range = [
    {
      range_name    = "${var.vpc_name}-01-gke-01-pods"
      ip_cidr_range = "10.0.0.0/14"
    },
    {
      range_name    = "${var.vpc_name}-01-gke-01-services"
      ip_cidr_range = "10.4.0.0/19"
    }
  ]
}

output "ip_range_name_pods" {
  value = google_compute_subnetwork.subnet.secondary_ip_range[0].range_name
}

output "ip_range_name_services" {
  value = google_compute_subnetwork.subnet.secondary_ip_range[1].range_name
}

output "name" {
  value = google_compute_network.vpc.name
}

output "subnet_name" {
  value = google_compute_subnetwork.subnet.name
}
