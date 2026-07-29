-- Repro harness for issue #36544.
--
-- pg_stat_statements is the primary instrument for this spike: the `calls` count
-- for `LISTEN cluster_actions` is a direct counter of PGListener instantiations,
-- because JDBCPubSubImpl.listener() re-issues LISTEN for every subscribed topic
-- each time it constructs a new listener (JDBCPubSubImpl.java:63-66).
--
-- In the production incident that counter read 3,687 over a single 600s startup.

CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
