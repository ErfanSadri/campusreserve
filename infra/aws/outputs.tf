output "alb_dns_name" { value = aws_lb.api.dns_name }
output "application_base_url" { value = "http://${aws_lb.api.dns_name}" }
output "ecr_repository_url" { value = aws_ecr_repository.api.repository_url }
output "rds_endpoint" { value = aws_db_instance.postgres.address }
output "redis_endpoint" { value = aws_elasticache_replication_group.redis.primary_endpoint_address }
output "msk_bootstrap_brokers_sasl_iam" { value = aws_msk_cluster.this.bootstrap_brokers_sasl_iam }
