CREATE TABLE IF NOT EXISTS shopping.order (
  id TEXT,
  order_datetime TIMESTAMP,
  order_qty INT,
  product_id INT,
  PRIMARY KEY ((id))
);

CREATE TABLE IF NOT EXISTS inventory.product (
  id INT,
  product_name TEXT,
  stock INT,
  PRIMARY KEY ((id))
);