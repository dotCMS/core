-- =====================================================================
-- Database creation and initialization
-- =====================================================================
-- Replicated database: replicates DDL (CREATE/ALTER/DROP) across all nodes via Keeper,
-- and coordinates refreshable MV execution so only one replica runs each refresh cycle.
CREATE DATABASE IF NOT EXISTS analytics
    ENGINE = Replicated('/clickhouse/databases/analytics', '{shard}', '{replica}');
USE analytics;

CREATE ROW POLICY IF NOT EXISTS rp_admin_user
ON analytics.*
FOR SELECT
    USING customer_id = 'customer1'
    AND environment = 'cluster1';