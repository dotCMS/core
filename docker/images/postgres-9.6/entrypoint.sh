#!/usr/bin/env bash
set -e

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
	local var="$1"
	local fileVar="${var}_FILE"
	local def="${2:-}"
	if [ "${!var:-}" ] && [ "${!fileVar:-}" ]; then
		echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
		exit 1
	fi
	local val="$def"
	if [ "${!var:-}" ]; then
		val="${!var}"
	elif [ "${!fileVar:-}" ]; then
		val="$(< "${!fileVar}")"
	fi
	export "$var"="$val"
	unset "$fileVar"
}

if [ "${1:0:1}" = '-' ]; then
	set -- postgres "$@"
fi

if [ "$(id -u)" = '0' ] && [ ! -s "$PGDATA/PG_VERSION" ]; then
	mkdir -p "$PGDATA"
	chown -R postgres "$PGDATA"
	chmod 700 "$PGDATA"
	cd "$PGDATA"
	tar xzf /tmp/db.tgz && rm /tmp/db.tgz

	mkdir -p /var/run/postgresql
	chown -R postgres /var/run/postgresql
	chmod 775 /var/run/postgresql

    su-exec postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses=''" -w start
	psql=( psql -v ON_ERROR_STOP=1 )

	if [ "$POSTGRES_ADMIN_PASSWORD" ]; then
		echo "Setting postgres admin password"
		"${psql[@]}" --username postgres <<-EOSQL
			ALTER USER "postgres" WITH SUPERUSER PASSWORD '$POSTGRES_ADMIN_PASSWORD' ;
		EOSQL
	fi

	echo "Setting up CMS DB"

	CMS_USER="${POSTGRES_CMS_USER:-dotcmsdbuser}"
	CMS_PASS="${POSTGRES_CMS_PASSWORD:-password}"
	CMS_DB="${POSTGRES_CMS_DBNAME:-dotcms}"

	"${psql[@]}" --username postgres <<-EOSQL
		CREATE DATABASE "$CMS_DB";
		CREATE USER "$CMS_USER" WITH PASSWORD '$CMS_PASS';
    	GRANT ALL PRIVILEGES ON DATABASE "$CMS_DB" to "$CMS_USER";
	EOSQL
		
	su-exec postgres pg_ctl -D "$PGDATA" -m fast -w stop

fi
	
exec su-exec postgres "postgres"
