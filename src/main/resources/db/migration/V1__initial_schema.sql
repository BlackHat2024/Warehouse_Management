CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(100) NOT NULL,
                       surname VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(200) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       PRIMARY KEY (id)
);

CREATE TABLE items (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(200) NOT NULL UNIQUE,
                       quantity BIGINT NOT NULL,
                       unit_price DECIMAL(12,2) NOT NULL,
                       package_volume BIGINT NOT NULL,
                       PRIMARY KEY (id)
);

CREATE TABLE trucks (
                        vin VARCHAR(64) NOT NULL,
                        license_plate VARCHAR(32) NOT NULL UNIQUE,
                        container_volume BIGINT NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE,
                        PRIMARY KEY (vin)
);

CREATE TABLE orders (
                        order_number BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        client_id BIGINT NOT NULL,
                        submitted_date TIMESTAMP NULL DEFAULT NULL,
                        status VARCHAR(32) NOT NULL,
                        PRIMARY KEY (order_number),
                        CONSTRAINT fk_order_client
                            FOREIGN KEY (client_id) REFERENCES users(id)
);

CREATE TABLE order_items (
                             order_id BIGINT UNSIGNED NOT NULL,
                             item_id BIGINT NOT NULL,
                             requested_qty BIGINT NOT NULL,
                             price DECIMAL(12,2) NOT NULL,
                             volume BIGINT NOT NULL,
                             PRIMARY KEY (order_id, item_id),
                             CONSTRAINT fk_oi_order
                                 FOREIGN KEY (order_id) REFERENCES orders(order_number),
                             CONSTRAINT fk_oi_item
                                 FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE deliveries (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            order_id BIGINT UNSIGNED NOT NULL UNIQUE,
                            scheduled_date DATE NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_delivery_order
                                FOREIGN KEY (order_id) REFERENCES orders(order_number)
);

CREATE TABLE delivery_trucks (
                                 delivery_id BIGINT NOT NULL,
                                 truck_id VARCHAR(64) NOT NULL, -- match trucks.vin
                                 PRIMARY KEY (delivery_id, truck_id),
                                 CONSTRAINT fk_dt_delivery
                                     FOREIGN KEY (delivery_id) REFERENCES deliveries(id),
                                 CONSTRAINT fk_dt_truck
                                     FOREIGN KEY (truck_id) REFERENCES trucks(vin)
);

CREATE INDEX idx_orders_status_submitted ON orders(status, submitted_date);