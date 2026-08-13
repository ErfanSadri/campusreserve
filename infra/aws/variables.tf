variable "aws_region" {
  type    = string
  default = "us-east-1"
}
variable "project_name" {
  type    = string
  default = "campusreserve"
}
variable "environment" {
  type    = string
  default = "demo"
}
variable "vpc_cidr" {
  type    = string
  default = "10.40.0.0/16"
}
variable "availability_zones" {
  type    = list(string)
  default = []
}

variable "rds_instance_class" {
  type    = string
  default = "db.t4g.micro"
}
variable "rds_allocated_storage_gb" {
  type    = number
  default = 20
}
# Keep unset by default: PostgreSQL 18 availability varies by region. Set this
# only after confirming the engine version in the chosen region.
variable "rds_engine_version" {
  type     = string
  default  = null
  nullable = true
}
variable "rds_master_username" {
  type    = string
  default = "campusreserve_admin"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t4g.micro"
}
variable "redis_engine_version" {
  type    = string
  default = "7.1"
}
variable "msk_broker_instance_type" {
  type    = string
  default = "kafka.t3.small"
}
variable "msk_kafka_version" {
  type    = string
  default = "3.9.x"
}
variable "msk_ebs_volume_size_gb" {
  type    = number
  default = 10
}

variable "ecs_cpu" {
  type    = number
  default = 512
}
variable "ecs_memory" {
  type    = number
  default = 1024
}
variable "ecs_desired_count" {
  type    = number
  default = 1
}
variable "container_image_tag" {
  type    = string
  default = "bootstrap-required"
}
variable "log_retention_days" {
  type    = number
  default = 7
}
variable "budget_alert_email" {
  type     = string
  default  = null
  nullable = true
}

variable "budget_limit_amount" {
  description = "Optional monthly AWS Budget amount in USD. Set this with budget_alert_email to enable an alert."
  type        = number
  default     = null
  nullable    = true
}
