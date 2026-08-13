# AWS deployment architecture

CRV-015 supplies Terraform for a manual, short-lived CampusReserve demo
environment. It has not provisioned AWS resources or produced AWS performance
results.

The request path is internet client → HTTP ALB → private ECS/Fargate API task.
The task connects only to private RDS PostgreSQL, TLS/AUTH ElastiCache Redis,
and IAM/TLS MSK Kafka through security-group rules. It logs to CloudWatch.
Secrets Manager supplies RDS and Redis passwords at ECS startup. MSK uses the
application task role and the AWS MSK IAM Kafka client mechanism; Kafka
credentials are not stored in task environment variables.

Terraform state is designed for an encrypted, versioned, non-public S3 bucket
with native S3 lockfiles. The bootstrap configuration creates that bucket, and
the main backend is configured through an ignored `backend.hcl`. Terraform
state may contain the generated Redis token, so state access must be treated
as secret access.

The demo minimizes cost with a single NAT Gateway, one task, single-AZ RDS,
one Redis node, and two MSK brokers. This reduces resilience and is not a
production HA topology. Refer to [the infrastructure guide](../infra/aws/README.md)
for prerequisites, ECR image bootstrap, manual deployment, smoke checks, cost
warning, and teardown procedure.
