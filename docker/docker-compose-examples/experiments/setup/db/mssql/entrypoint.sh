#!/bin/bash
wait_time=15s

# wait for SQL Server to come up
echo ""
echo "---"
echo importing data will start in $wait_time...
sleep $wait_time

echo ""
echo "---"
echo importing data...

# run the init script to create the DB and the tables in /table
/opt/mssql-tools/bin/sqlcmd -S 0.0.0.0 -U sa -P ${MSSQL_SA_PASSWORD} -i ./init-scripts/init.sql

echo data imported...