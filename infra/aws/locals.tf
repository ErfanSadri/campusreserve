locals {
  name_prefix        = "${var.project_name}-${var.environment}"
  availability_zones = length(var.availability_zones) > 0 ? var.availability_zones : slice(data.aws_availability_zones.available.names, 0, 2)

  common_tags = {
    Project     = "CampusReserve"
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}
