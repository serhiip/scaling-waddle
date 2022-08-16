module "gke" {
  source                     = "terraform-google-modules/kubernetes-engine/google"
  project_id                 = var.project_id
  name                       = var.cluster_name
  region                     = var.region
  zones                      = var.zones
  network                    = var.vpc_network_name
  subnetwork                 = var.vpc_subnet_name
  ip_range_pods              = var.ip_range_name_pods
  ip_range_services          = var.ip_range_name_services
  service_account            = var.service_account
  http_load_balancing        = false
  network_policy             = false
  horizontal_pod_autoscaling = true
  filestore_csi_driver       = false
  create_service_account     = false

  node_pools = [
    {
      name                   = "${var.cluster_name}-node-pool"
      machine_type           = "e2-medium"
      node_locations         = join(",", var.zones)
      min_count              = 1
      max_count              = 3
      local_ssd_count        = 0
      spot                   = false
      disk_size_gb           = 10
      disk_type              = "pd-standard"
      image_type             = "COS_CONTAINERD"
      enable_gcfs            = false
      enable_gvnic           = false
      auto_repair            = true
      auto_upgrade           = false
      service_account        = var.service_account
      preemptible            = false
      initial_node_count     = 2
      create_service_account = false
    },
  ]

  node_pools_oauth_scopes = {
    all = [
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/trace.append",
    ]
  }

  node_pools_labels = {
    all = {}

    default-node-pool = {
      default-node-pool = true
    }
  }

  node_pools_metadata = {
    all = {}

    default-node-pool = {
      node-pool-metadata-custom-value = "my-node-pool"
    }
  }

  node_pools_taints = {
    all = []

    default-node-pool = [
      {
        key    = "default-node-pool"
        value  = true
        effect = "PREFER_NO_SCHEDULE"
      },
    ]
  }

  node_pools_tags = {
    all = []

    default-node-pool = [
      "default-node-pool",
    ]
  }
}

output "endpoint" {
  value = module.gke.endpoint
}

output "name" {
  value = module.gke.name
}

output "ca_certificate" {
  value = module.gke.ca_certificate
}
