-- 购物车模块升级脚本
USE lab3392;

CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id),
  INDEX idx_cart_items_user_id (user_id),
  INDEX idx_cart_items_product_id (product_id),
  CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
    REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
