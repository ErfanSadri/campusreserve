resource "aws_security_group" "alb" {
  name   = "${local.name_prefix}-alb"
  vpc_id = aws_vpc.this.id
}

resource "aws_security_group" "ecs" {
  name   = "${local.name_prefix}-ecs"
  vpc_id = aws_vpc.this.id

}

# Standalone rules avoid an ALB/ECS security-group dependency cycle while
# retaining the intended public-ALB-only ingress boundary.
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}
resource "aws_vpc_security_group_egress_rule" "alb_to_ecs" {
  security_group_id            = aws_security_group.alb.id
  referenced_security_group_id = aws_security_group.ecs.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}
resource "aws_vpc_security_group_ingress_rule" "ecs_from_alb" {
  security_group_id            = aws_security_group.ecs.id
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
}
# Private tasks use the single NAT gateway for image pulls, AWS APIs, and
# external package/service traffic; service ingress remains SG-restricted.
resource "aws_vpc_security_group_egress_rule" "ecs_all" {
  security_group_id = aws_security_group.ecs.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_security_group" "postgres" {
  name   = "${local.name_prefix}-postgres"
  vpc_id = aws_vpc.this.id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_security_group" "redis" {
  name   = "${local.name_prefix}-redis"
  vpc_id = aws_vpc.this.id
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}

resource "aws_security_group" "kafka" {
  name   = "${local.name_prefix}-kafka"
  vpc_id = aws_vpc.this.id
  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }
}
