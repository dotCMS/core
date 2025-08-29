
#!/bin/bash

# PostgreSQL Production Database Report Script
# Builds a CSV of intel by querying the prod databases.
# Usage: ./postgres_prod_report.sh [host] [ports] [username]
# ports can be a single port or comma-separated list: 15432 or 15432,10016,10006
# Note: You need to be connected via SDM for this to work

set -e

HOST=${1:-localhost}
PORTS="10050,10069,10070,10071,10072,10073"
USERNAME=${3:-postgres}
CSV_FILE="dotcms_database_report_$(date +%Y%m%d_%H%M%S).csv"

echo "PostgreSQL Production Database Report"
echo "Host: $HOST, Ports: $PORTS, User: $USERNAME"
echo "======================================"
echo

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo "Error: psql command not found. Please install PostgreSQL client."
    exit 1
fi

# Getting old data
DATA_QUERY="select folder.identifier as identifier,folder.inode as inode from template,folder where template.theme = folder.identifier group by template.theme,folder.inode,folder.identifier";




# Convert comma-separated ports to array
IFS=',' read -ra PORT_ARRAY <<< "$PORTS"

# Loop through each port
for PORT in "${PORT_ARRAY[@]}"; do
    # Trim whitespace
    PORT=$(echo "$PORT" | sed 's/^ *//;s/ *$//')

    echo "Processing port: $PORT" >&2

    # psql options to suppress messages for current port
    PSQLOPTS="-h $HOST -p $PORT -U $USERNAME -q -t -A"

    # Get list of databases containing 'prod' in their name for this port
    PROD_DBS=$(psql $PSQLOPTS -d postgres -c "SELECT datname FROM pg_database WHERE datname LIKE '%prod%' AND datistemplate = false;" 2>/dev/null | sed 's/^ *//' | grep -v '^$' || echo "")

    if [ -z "$PROD_DBS" ]; then
        echo "No databases found with 'prod' in their name on port $PORT." >&2
        continue
    fi
    mkdir -p ./theme-restore-data/$PORT/
    echo "Found databases on port $PORT: $PROD_DBS" >&2

    # Loop through each production database for this port
    while IFS= read -r db; do
        if [ -n "$db" ]; then
            echo "Processing database: $db on port $PORT" >&2
            psql $PSQLOPTS --csv -c "$DATA_QUERY" $db >>  ./theme-restore-data/$PORT/$db-test.csv

        fi
    done <<< "$PROD_DBS"
done

echo
echo "Report completed."
echo "CSV file saved as: $CSV_FILE"
