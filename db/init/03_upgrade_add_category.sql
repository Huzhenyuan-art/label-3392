USE lab3392;

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_add_product_category$$

CREATE PROCEDURE upgrade_add_product_category()
BEGIN
    DECLARE col_exists INT;
    DECLARE table_exists INT;
    DECLARE default_cat_id BIGINT;

    SELECT COUNT(*) INTO table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'products';

    IF table_exists = 1 THEN
        SELECT COUNT(*) INTO col_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'products'
          AND column_name = 'category_id';

        IF col_exists = 0 THEN
            ALTER TABLE products
            ADD COLUMN category_id BIGINT NOT NULL DEFAULT 1
            COMMENT '产品分类ID，关联product_categories.id';

            ALTER TABLE products
            ADD INDEX idx_products_category_id (category_id);

            IF NOT EXISTS (
                SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = 'products'
                  AND constraint_name = 'fk_products_category'
            ) THEN
                ALTER TABLE products
                ADD CONSTRAINT fk_products_category
                FOREIGN KEY (category_id)
                REFERENCES product_categories(id)
                ON DELETE RESTRICT
                ON UPDATE CASCADE;
            END IF;
        ELSE
            SELECT id INTO default_cat_id
            FROM product_categories
            WHERE code = 'LAPTOP' OR status = 'ACTIVE'
            ORDER BY id ASC
            LIMIT 1;

            IF default_cat_id IS NOT NULL THEN
                UPDATE products
                SET category_id = default_cat_id
                WHERE category_id IS NULL OR category_id = 0;
            END IF;
        END IF;
    END IF;
END$$

DELIMITER ;

CALL upgrade_add_product_category();

DROP PROCEDURE IF EXISTS upgrade_add_product_category;
