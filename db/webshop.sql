CREATE TABLE IF NOT EXISTS customer_profiles (
  customer_id    INT NOT NULL,
  loyalty_points INT NOT NULL DEFAULT 0,
  PRIMARY KEY (customer_id),
  CONSTRAINT fk_cp_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_status_history (
  id         INT NOT NULL AUTO_INCREMENT,
  order_id   INT NOT NULL,
  old_status VARCHAR(50) DEFAULT NULL,
  new_status VARCHAR(50) NOT NULL,
  changed_at DATETIME NOT NULL,
  changed_by VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_osh_order_time (order_id, changed_at),
  CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB;

# hinnat kelpoisuusajan mukaan, ei päällekkäisiä jaksoja
CREATE TABLE IF NOT EXISTS product_prices (
  product_id INT NOT NULL,
  price      DECIMAL(10,2) NOT NULL,
  valid_from DATE NOT NULL,
  valid_to   DATE NOT NULL,
  PERIOD FOR valid_time (valid_from, valid_to),
  PRIMARY KEY (product_id, valid_time WITHOUT OVERLAPS),
  CONSTRAINT fk_pp_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;


# rajoitteet

ALTER TABLE products
  ADD CONSTRAINT IF NOT EXISTS chk_products_price CHECK (price >= 0),
  ADD CONSTRAINT IF NOT EXISTS chk_products_stock CHECK (stock_quantity >= 0);

ALTER TABLE orderitems
  ADD CONSTRAINT IF NOT EXISTS chk_orderitems_quantity CHECK (quantity > 0),
  ADD CONSTRAINT IF NOT EXISTS chk_orderitems_price CHECK (unit_price >= 0);

ALTER TABLE product_prices
  ADD CONSTRAINT IF NOT EXISTS chk_pp_price CHECK (price >= 0);


# temporaalisuus, kirjausaika

# tuotteen nimen hinnan ja kuvauksen historia talteen
# saldo jätetään pois koska se muuttuu joka tilauksella
ALTER TABLE products ADD SYSTEM VERSIONING;
SET @@system_versioning_alter_history = KEEP;
ALTER TABLE products MODIFY stock_quantity INT NOT NULL DEFAULT 0 WITHOUT SYSTEM VERSIONING;

# osoitehistoria, vanhan tilauksen toimitusosoite saadaan as of tilausaika
ALTER TABLE customeraddresses ADD SYSTEM VERSIONING;


# indeksit

# asiakkaan haku sähköpostilla
CREATE INDEX IF NOT EXISTS idx_email ON customers (email);

# kategorian tuotteet hinnan mukaan järjestettynä
CREATE INDEX IF NOT EXISTS idx_products_category_price ON products (category_id, price);

# asiakkaan tilaukset uusin ensin
CREATE INDEX IF NOT EXISTS idx_orders_customer_date ON orders (customer_id, order_date);

# status yksin huono indeksi mutta päivämäärän kanssa event löytää rivit nopeasti
CREATE INDEX IF NOT EXISTS idx_orders_status_date ON orders (status, order_date);

# tekstihaku tuotteista
CREATE FULLTEXT INDEX IF NOT EXISTS ft_products_name_desc ON products (name, description);

ANALYZE TABLE customers, products, orders, orderitems;


# funktio, voimassa oleva hinta tiettynä päivänä

DROP FUNCTION IF EXISTS fn_effective_price;
DELIMITER //
CREATE FUNCTION fn_effective_price(p_product_id INT, p_date DATE)
RETURNS DECIMAL(10,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
  DECLARE v_price DECIMAL(10,2);

  SELECT pp.price INTO v_price
    FROM product_prices pp
   WHERE pp.product_id = p_product_id
     AND p_date >= pp.valid_from
     AND p_date < pp.valid_to
   LIMIT 1;

  IF v_price IS NULL THEN
    SELECT p.price INTO v_price FROM products p WHERE p.id = p_product_id;
  END IF;

  RETURN v_price;
END //
DELIMITER ;


# näkymät

# tuotekatalogi asiakkaalle, merge joten indeksit toimii
CREATE OR REPLACE ALGORITHM = MERGE VIEW v_product_catalog AS
SELECT p.id                        AS product_id,
       p.name                      AS name,
       p.description               AS description,
       p.price                     AS list_price,
       COALESCE(pp.price, p.price) AS effective_price,
       (pp.price IS NOT NULL)      AS on_campaign,
       p.stock_quantity            AS stock_quantity,
       (p.stock_quantity > 0)      AS in_stock,
       p.category_id               AS category_id,
       c.name                      AS category_name,
       p.supplier_id               AS supplier_id,
       s.name                      AS supplier_name
  FROM products p
  LEFT JOIN productcategories c ON c.id = p.category_id
  LEFT JOIN suppliers s ON s.id = p.supplier_id
  LEFT JOIN product_prices pp ON pp.product_id = p.id
       AND CURDATE() >= pp.valid_from
       AND CURDATE() < pp.valid_to;

# tilausten yhteenveto
# alikyselyt eikä group by, muuten temptable laskisi koko taulun joka kerta
CREATE OR REPLACE ALGORITHM = MERGE VIEW v_order_summary AS
SELECT o.id                                   AS order_id,
       o.customer_id                          AS customer_id,
       CONCAT(c.first_name, ' ', c.last_name) AS customer_name,
       o.order_date                           AS order_date,
       o.delivery_date                        AS delivery_date,
       o.status                               AS status,
       (SELECT COUNT(*) FROM orderitems oi WHERE oi.order_id = o.id) AS item_rows,
       (SELECT COALESCE(SUM(oi.quantity), 0) FROM orderitems oi WHERE oi.order_id = o.id) AS total_quantity,
       (SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0) FROM orderitems oi WHERE oi.order_id = o.id) AS total_amount
  FROM orders o
  STRAIGHT_JOIN customers c ON c.id = o.customer_id;


# liipaisimet
# tilat new > paid > shipped > delivered, new tai paid voi perua, new voi vanhentua

DELIMITER //

CREATE OR REPLACE TRIGGER orders_status_init
AFTER INSERT ON orders
FOR EACH ROW
BEGIN
  INSERT INTO order_status_history (order_id, old_status, new_status, changed_at, changed_by)
  VALUES (NEW.id, NULL, NEW.status, NOW(), CURRENT_USER());
END //

CREATE OR REPLACE TRIGGER orders_status_log
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
  IF NOT (OLD.status <=> NEW.status) THEN
    INSERT INTO order_status_history (order_id, old_status, new_status, changed_at, changed_by)
    VALUES (NEW.id, OLD.status, NEW.status, NOW(), CURRENT_USER());
  END IF;
END //

# vain sallitut tilasiirtymät, toimituspäivä asetetaan automaattisesti
CREATE OR REPLACE TRIGGER trg_orders_status_guard
BEFORE UPDATE ON orders
FOR EACH ROW
BEGIN
  IF NOT (OLD.status <=> NEW.status) THEN
    IF NOT (
         (OLD.status = 'NEW' AND NEW.status IN ('PAID', 'CANCELLED', 'EXPIRED'))
      OR (OLD.status = 'PAID' AND NEW.status IN ('SHIPPED', 'CANCELLED'))
      OR (OLD.status = 'SHIPPED' AND NEW.status = 'DELIVERED')
    ) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid order status transition';
    END IF;

    IF NEW.status = 'DELIVERED' AND NEW.delivery_date IS NULL THEN
      SET NEW.delivery_date = NOW();
    END IF;
  END IF;
END //

# rivin hinta tulee aina kannasta, rivejä vain uuteen tilaukseen
CREATE OR REPLACE TRIGGER trg_orderitems_before_insert
BEFORE INSERT ON orderitems
FOR EACH ROW
BEGIN
  DECLARE v_status VARCHAR(50);

  SELECT status INTO v_status FROM orders WHERE id = NEW.order_id;
  IF v_status IS NULL OR v_status <> 'NEW' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order items can only be added to orders in status NEW';
  END IF;

  SET NEW.unit_price = fn_effective_price(NEW.product_id, CURDATE());
END //

# bonuspisteet kun tilaus toimitettu, 1 piste per euro
CREATE OR REPLACE TRIGGER trg_orders_loyalty_points
AFTER UPDATE ON orders
FOR EACH ROW
FOLLOWS orders_status_log
BEGIN
  DECLARE v_points INT;

  IF NEW.status = 'DELIVERED' AND NOT (OLD.status <=> 'DELIVERED') THEN
    SELECT COALESCE(FLOOR(SUM(quantity * unit_price)), 0) INTO v_points
      FROM orderitems WHERE order_id = NEW.id;

    INSERT INTO customer_profiles (customer_id, loyalty_points)
    VALUES (NEW.customer_id, v_points)
    ON DUPLICATE KEY UPDATE loyalty_points = loyalty_points + v_points;
  END IF;
END //

DELIMITER ;


# ajastettu tapahtuma
# maksamaton tilaus vanhenee 30 min jälkeen ja varatut tuotteet palautetaan varastoon
# korvaa vanhan delete_vold eventin joka ei palauttanut saldoa

DROP EVENT IF EXISTS delete_vold;
DROP EVENT IF EXISTS ev_expire_unpaid_orders;

DELIMITER //
CREATE EVENT ev_expire_unpaid_orders
ON SCHEDULE EVERY 1 MINUTE
DO
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_expired_orders;
  END;

  START TRANSACTION;

  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_expired_orders (id INT PRIMARY KEY);

  # lukitaan rivit ettei maksu mene ristiin
  INSERT INTO tmp_expired_orders (id)
  SELECT id FROM orders
   WHERE status = 'NEW'
     AND order_date < NOW() - INTERVAL 30 MINUTE
     FOR UPDATE;

  UPDATE products p
    JOIN (SELECT oi.product_id, SUM(oi.quantity) AS qty
            FROM orderitems oi
            JOIN tmp_expired_orders t ON t.id = oi.order_id
           GROUP BY oi.product_id) r ON r.product_id = p.id
     SET p.stock_quantity = p.stock_quantity + r.qty;

  UPDATE orders o
    JOIN tmp_expired_orders t ON t.id = o.id
     SET o.status = 'EXPIRED';

  COMMIT;

  DROP TEMPORARY TABLE IF EXISTS tmp_expired_orders;
END //
DELIMITER ;

SET GLOBAL event_scheduler = ON;


# käyttäjät ja oikeudet, vähimmät tarvittavat

CREATE USER IF NOT EXISTS 'webshop_app'@'localhost' IDENTIFIED BY 'Webshop_App_2026';

# asiakkaan api ei muokkaa tuotteita, vain saldo päivittyy tilauksissa
GRANT SELECT ON tietokannat.products TO 'webshop_app'@'localhost';
GRANT UPDATE (stock_quantity) ON tietokannat.products TO 'webshop_app'@'localhost';
GRANT SELECT ON tietokannat.productcategories TO 'webshop_app'@'localhost';
GRANT SELECT ON tietokannat.product_prices TO 'webshop_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON tietokannat.customers TO 'webshop_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON tietokannat.customeraddresses TO 'webshop_app'@'localhost';

# tilauksia ei poisteta, vain perutaan
GRANT SELECT, INSERT, UPDATE ON tietokannat.orders TO 'webshop_app'@'localhost';
GRANT SELECT, INSERT, UPDATE ON tietokannat.orderitems TO 'webshop_app'@'localhost';

# nämä kirjoittaa vain liipaisimet
GRANT SELECT ON tietokannat.order_status_history TO 'webshop_app'@'localhost';
GRANT SELECT ON tietokannat.customer_profiles TO 'webshop_app'@'localhost';

GRANT SELECT ON tietokannat.v_product_catalog TO 'webshop_app'@'localhost';
GRANT SELECT ON tietokannat.v_order_summary TO 'webshop_app'@'localhost';
GRANT EXECUTE ON FUNCTION tietokannat.fn_effective_price TO 'webshop_app'@'localhost';

FLUSH PRIVILEGES;
