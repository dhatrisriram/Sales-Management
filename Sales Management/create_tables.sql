-- Create tables for the Sales Management Quotes module
Create Database if not exists polymorphs;
USE polymorphs;
CREATE TABLE IF NOT EXISTS customers (
                            customer_id INT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(100) NOT NULL,
                            email VARCHAR(100) UNIQUE,
                            phone VARCHAR(15),
                            region VARCHAR(50),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS deals (
                            deal_id INT PRIMARY KEY AUTO_INCREMENT,
                            customer_id INT,
                            amount DOUBLE,
                            stage VARCHAR(50),
                            status VARCHAR(50),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                        );
                        
CREATE TABLE IF NOT EXISTS quotes (
                            quote_id INT PRIMARY KEY AUTO_INCREMENT,
                            customer_id INT,
                            deal_id INT,
                            total_amount DOUBLE,
                            discount DOUBLE,
                            final_amount DOUBLE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
                            FOREIGN KEY (deal_id) REFERENCES deals(deal_id)
                        );

CREATE TABLE IF NOT EXISTS quote_items (
                            item_id INT PRIMARY KEY AUTO_INCREMENT,
                            quote_id INT,
                            product_name VARCHAR(100),
                            quantity INT,
                            price DOUBLE,
                            FOREIGN KEY (quote_id) REFERENCES quotes(quote_id)
                        );
