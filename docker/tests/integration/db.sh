printUsage () {
  echo ""
  echo "Usage:"
  echo '-d      database: (postgres as default) -> One of ["postgres", "mysql", "oracle", "mssql"]'
  echo ""
  echo "============================================================================"
  echo "============================================================================"
  echo "         Examples:"
  echo ""
  echo "         ./db.sh"
  echo "         ./db.sh -d postgres"
  echo "         ./db.sh -d mysql"
  echo "         ./db.sh -d oracle"
  echo "         ./db.sh -d mssql"
  echo "============================================================================"
  echo "============================================================================"
  echo ""
  echo "Note:"
  echo "[postgres] as default database if -d option is not use."
}

while getopts "d:h" option; do
  case ${option} in

  d) database=${OPTARG} ;;
  h) printUsage
    exit 1
    ;;
  *)
    printUsage
    exit 1
    ;;
  esac
done

#  One of ["postgres", "mysql", "oracle", "mssql"]
if [ -z "${database}" ]; then
  echo ""
  echo " >>> database parameter NOT FOUND, setting [postgres] as default DB"
  echo ""
  database=postgres
fi

# Starting the container for the build image
export databaseType=${database}
docker-compose -f ${database}-docker-compose.yml \
  up \
  --abort-on-container-exit

# Required code, without it the script will exit and won't be able to down the containers properly
dbReturnCode=$?

# Cleaning up
docker-compose -f ${database}-docker-compose.yml down
