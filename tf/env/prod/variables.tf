variable project_id {
  type = string
}

variable region {
  type = string
  default = "europe-west6"
}

variable zones {
  type = list(string)
  default = ["europe-west6a"]
}

variable service_account {
  type = string
}
