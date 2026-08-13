resource "aws_msk_configuration" "this" {
  name              = "${local.name_prefix}-kafka"
  kafka_versions    = ["3.7.x"]
  server_properties = <<-PROPERTIES
    auto.create.topics.enable=true
    default.replication.factor=2
    min.insync.replicas=1
    num.partitions=1
  PROPERTIES
}

resource "aws_msk_cluster" "this" {
  cluster_name           = "${local.name_prefix}-kafka"
  kafka_version          = "3.7.x"
  number_of_broker_nodes = 2

  broker_node_group_info {
    instance_type   = var.msk_broker_instance_type
    client_subnets  = aws_subnet.data[*].id
    security_groups = [aws_security_group.kafka.id]
    storage_info {
      ebs_storage_info { volume_size = var.msk_ebs_volume_size_gb }
    }
  }

  client_authentication {
    sasl {
      iam = true
    }
  }
  encryption_info {
    encryption_at_rest_kms_key_arn = null
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
  configuration_info {
    arn      = aws_msk_configuration.this.arn
    revision = aws_msk_configuration.this.latest_revision
  }
  enhanced_monitoring = "DEFAULT"
}
