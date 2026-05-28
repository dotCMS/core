# Single Node OS Migration Stack

Runs two OpenSearch clusters side-by-side for ES → OpenSearch migration testing:

| Service | Version | URL |
|---|---|---|
| OpenSearch 1.x (primary) | 1.3.x | https://localhost:9200 |
| OpenSearch 3.x (shadow) | 3.4.0 | https://localhost:9201 |
| OS 1.x Dashboards | 1.3.x | http://localhost:5601 |
| OS 3.x Dashboards | 3.0.0 | http://localhost:5602 |
| dotCMS | latest | http://localhost:8082 |
| Glowroot | - | http://localhost:4000 |
| PostgreSQL | 18 | localhost:5432 |

## Quick Start

```bash
docker compose up -d
```

## Startup Order

1. PostgreSQL + OpenSearch 1.x + OpenSearch 3.x start in parallel
2. Provisioning scripts wait for healthchecks, then create `dotcms-es-user` on both clusters
3. dotCMS starts after both provisioners complete

## Credentials

### Admin (cluster management)

| Cluster | Username | Password |
|---|---|---|
| OpenSearch 1.x | `admin` | `admin` |
| OpenSearch 3.x | `admin` | `FENMSCuN4wsRps4ftYwxsx8PfkBEYiAv2025` |

### Provisioned user (dotCMS)

| Field | Value |
|---|---|
| Username | `dotcms-es-user` |
| Password | `dotcmsEsUser7Kx9mQvR3nW2026` |

## Sample curl Commands

### OpenSearch 1.x — admin

```bash
# List indices
curl -sk https://localhost:9200/_cat/indices?v -u admin:admin

# List internal users
curl -sk https://localhost:9200/_plugins/_security/api/internalusers?pretty -u admin:admin
```

### OpenSearch 1.x — provisioned user

```bash
# List indices
curl -sk https://localhost:9200/_cat/indices?v -u dotcms-es-user:dotcmsEsUser7Kx9mQvR3nW2026
```

### OpenSearch 3.x — admin

```bash
# List indices
curl -sk https://localhost:9201/_cat/indices?v -u admin:FENMSCuN4wsRps4ftYwxsx8PfkBEYiAv2025

# List internal users
curl -sk https://localhost:9201/_plugins/_security/api/internalusers?pretty -u admin:FENMSCuN4wsRps4ftYwxsx8PfkBEYiAv2025
```

### OpenSearch 3.x — provisioned user

```bash
# List indices
curl -sk https://localhost:9201/_cat/indices?v -u dotcms-es-user:dotcmsEsUser7Kx9mQvR3nW2026
```

## Migration Phases

Controlled by `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE` in docker-compose.yml:

| Phase | Writes | Reads | Description |
|---|---|---|---|
| 0 | OS 1.x only | OS 1.x only | Migration not started |
| 1 | OS 1.x + OS 3.x | OS 1.x only | Dual-write, validate OS 3.x data |
| 2 | OS 1.x + OS 3.x | OS 3.x only | Reads from OS 3.x, ES fallback |
| 3 | OS 3.x only | OS 3.x only | Migration complete |

Currently set to **Phase 1** (dual-write).

## Provisioning Script

`opensearch.py` creates on each cluster:
- Internal user (`dotcms-es-user`)
- Role (`dotcms-role`)
- Role mapping (`dotcms-role` → `dotcms-es-user`)
- Action groups (cluster, index, all-indices permissions)

Run standalone:
```bash
./opensearch.py --admin-user admin --admin-pass admin --password mypass --customer dotcms --host localhost --port 9200
```
