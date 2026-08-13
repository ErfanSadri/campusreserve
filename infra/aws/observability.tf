resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${local.name_prefix}/reservation-api"
  retention_in_days = var.log_retention_days
}

resource "aws_budgets_budget" "demo" {
  count        = var.budget_alert_email != null && var.budget_limit_amount != null ? 1 : 0
  name         = "${local.name_prefix}-monthly-alert"
  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_amount)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
  }
}
