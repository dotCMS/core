-- 1. Create the user
CREATE USER 'cust-001' IDENTIFIED BY 'abc' DEFAULT DATABASE analytics;
-- 2. Grant necessary privileges
GRANT SELECT ON analytics.* TO 'cust-001';
-- 3. Create the row policy to filter by customer_id
CREATE ROW POLICY 'cust-001-policy' ON analytics.* USING customer_id='cust-001' TO 'cust-001';

--- 4.  Allow from any host
ALTER USER 'cust-001' HOST ANY;
--- 5 - Grant Write permissions
GRANT INSERT ON analytics.events TO `cust-001`;