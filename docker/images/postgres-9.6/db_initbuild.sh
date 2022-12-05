#!/usr/bin/env bash
set -e

export POSTGRES_PASSWORD="$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)"
export PGDATA=/tmp/PGDATA

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
	echo BREAKPOINT

# allow the container to be started with `--user`
if [ "$(id -u)" = '0' ]; then
	mkdir -p "$PGDATA"
	chown -R postgres "$PGDATA"
	chmod 700 "$PGDATA"

	mkdir -p /var/run/postgresql
	chown -R postgres /var/run/postgresql
	chmod 775 /var/run/postgresql

	# Create the transaction log directory before initdb is run (below) so the directory is owned by the correct user
	if [ "$POSTGRES_INITDB_XLOGDIR" ]; then
		mkdir -p "$POSTGRES_INITDB_XLOGDIR"
		chown -R postgres "$POSTGRES_INITDB_XLOGDIR"
		chmod 700 "$POSTGRES_INITDB_XLOGDIR"
	fi

	exec su-exec postgres "$BASH_SOURCE" "initbuild"
fi

if [ "$1" = 'initbuild' ]; then
	mkdir -p "$PGDATA"
	chown -R "$(id -u)" "$PGDATA" 2>/dev/null || :
	chmod 700 "$PGDATA" 2>/dev/null || :

	# look specifically for PG_VERSION, as it is expected in the DB dir
	if [ ! -s "$PGDATA/PG_VERSION" ]; then
		file_env 'POSTGRES_INITDB_ARGS'
		if [ "$POSTGRES_INITDB_XLOGDIR" ]; then
			export POSTGRES_INITDB_ARGS="$POSTGRES_INITDB_ARGS --xlogdir $POSTGRES_INITDB_XLOGDIR"
		fi
		eval "initdb --username=postgres $POSTGRES_INITDB_ARGS"

		echo "local all all trust" >> "$PGDATA/pg_hba.conf"
		echo "host all all 0.0.0.0/0 password" >> "$PGDATA/pg_hba.conf"

		# internal start of server in order to allow set-up using psql-client
		# does not listen on external TCP/IP and waits until start finishes
		PGUSER="${PGUSER:-postgres}" \
		pg_ctl -D "$PGDATA" \
			-o "-c listen_addresses=''" \
			-w start

		POSTGRES_USER="postgres"
		POSTGRES_DB="postgres"
		export PGUSER="$POSTGRES_USER"

		psql=( psql -v ON_ERROR_STOP=1 )

		"${psql[@]}" --username postgres <<-EOSQL
			ALTER USER "postgres" WITH SUPERUSER PASSWORD '$POSTGRES_PASSWORD' ;
		EOSQL
		

		PGUSER="${PGUSER:-postgres}" \
		pg_ctl -D "$PGDATA" -m fast -w stop

		cd "$PGDATA" && tar czf /tmp/db.tgz ./
	fi
fi