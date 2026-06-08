-- 操作审计日志模块升级脚本
USE lab3392;

CREATE TABLE IF NOT EXISTS operation_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operator_username VARCHAR(50) NOT NULL,
  operation_type VARCHAR(20) NOT NULL,
  target_type VARCHAR(50) NOT NULL,
  target_id BIGINT NOT NULL,
  before_snapshot TEXT,
  after_snapshot TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_operation_logs_operator_id (operator_id),
  INDEX idx_operation_logs_operation_type (operation_type),
  INDEX idx_operation_logs_created_at (created_at),
  INDEX idx_operation_logs_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
