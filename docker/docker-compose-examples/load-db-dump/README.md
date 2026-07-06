# dotCMS with Database Dump Load

A single dotCMS instance that bootstraps its database from an existing PostgreSQL dump on first startup, instead of from a starter site.

## Stack

| Service | Image                            | Notes |
| --- |----------------------------------| --- |
| `db` | `pgvector/pgvector:pg18`         | PostgreSQL with pgvector |
| `opensearch` | `opensearchproject/opensearch:1` | Single-node search, ports `9200` / `9600` |
| `dotcms` | `dotcms/dotcms:latest`           | App server, ports `8082` (HTTP), `8443` (HTTPS), `4000` (Glowroot UI) |

dotCMS loads the SQL dump via the `DB_LOAD_DUMP_SQL` environment variable, which points at `/data/dump/dump.sql` inside the container. That path is mounted from a local `.sql` file you provide.

## Usage

#### Environment setup

1. **Required — point the dump mount at your local SQL file.** Edit the `dotcms` service `volumes` entry and replace the placeholder with the path to your dump:

   ```yaml
   - {local_path_DB.sql}.sql:/data/dump/dump.sql
   ```

   For example:

   ```yaml
   - ./my-backup.sql:/data/dump/dump.sql
   ```

   > The dump is only loaded into a fresh/empty database. If the `dbdata` volume already contains data, the load is skipped — run `docker-compose down -v` first to start clean.

2. A custom starter can be set by uncommenting and updating this line (note: normally not needed when loading from a dump):

   ```yaml
   #CUSTOM_STARTER_URL: 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20260629/starter-20260629.zip'
   ```

3. HTTPS is optional. To enable it, uncomment the SSL cert env vars and the `certs` volume mount (a cert can be created with the [mkcert](https://github.com/FiloSottile/mkcert) tool):

   ```yaml
   #CMS_SSL_CERTIFICATE_FILE: '/certs/localhost.pem'
   #CMS_SSL_CERTIFICATE_KEY_FILE: '/certs/localhost-key.pem'
   #- ${HOME}/.dotcms/certs:/certs
   ```

#### Run the example

```bash
docker-compose up
```

Once started, dotCMS is available at:

- http://localhost:8082 — log in with the admin credentials from the loaded dump (the user/password come from the database you imported, not the `DOT_INITIAL_ADMIN_PASSWORD` value)
- https://localhost:8443 (if SSL is enabled)
- http://localhost:4000 — Glowroot profiling UI (enabled by default; **do not enable in production**)

#### Shut down instances

```bash
docker-compose down       # stop and remove containers (keeps the database volume)
docker-compose down -v    # also remove volumes (db, search, shared) — required to re-load the dump
```

**Important note:** `ctrl+c` does not destroy instances.
