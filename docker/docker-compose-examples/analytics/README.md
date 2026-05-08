# Docker Setup

This directory contains the Docker Compose configuration and supporting files for running the
**dotCMS Content Analytics Event Manager** and its ClickHouse cluster locally.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Services](#services)
   - [clickhouse-keeper](#clickhouse-keeper)
   - [clickhouse-01 / clickhouse-02](#clickhouse-01--clickhouse-02)
   - [ca-event-manager](#ca-event-manager)
3. [Directory Layout](#directory-layout)
4. [Running the Stack](#running-the-stack)
   - [ClickHouse only (recommended for development)](#clickhouse-only-recommended-for-development)
   - [Full stack](#full-stack)
5. [Configuration Files](#configuration-files)
6. [Database Initialization](#database-initialization)
7. [Default Credentials](#default-credentials)
8. [Ports at a Glance](#ports-at-a-glance)
9. [Scaling Keeper to Production](#scaling-keeper-to-production)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                      Docker network                          │
│                                                              │
│   ┌──────────────────┐        ┌──────────────────────────┐   │
│   │ clickhouse-keeper│◄──────►│      clickhouse-01       │   │
│   │  (Raft / coord)  │        │  (data node, replica 1)  │   │
│   └──────────────────┘        │  HTTP :8123  TCP :9000   │   │
│           ▲                   └──────────────────────────┘   │
│           │                                  ▲               │
│           │                                  │ replication   │
│           │                   ┌──────────────────────────┐   │
│           └──────────────────►│      clickhouse-02       │   │
│                               │  (data node, replica 2)  │   │
│                               │  HTTP :8124  TCP :9001   │   │
│                               └──────────────────────────┘   │
│                                                              │
│   ┌──────────────────────────────────────┐                   │
│   │          ca-event-manager            │                   │
│   │   Spring Boot app  HTTP :8080        │                   │
│   │   connects to clickhouse-01:8123     │                   │
│   └──────────────────────────────────────┘                   │
└──────────────────────────────────────────────────────────────┘
```

The two ClickHouse data nodes form a **single-shard, two-replica** cluster. `clickhouse-keeper`
provides Raft-based coordination (replication queues, DDL distribution, leader election). DDL
executed on either node is automatically propagated to the other because the `analytics` database
uses the `Replicated` engine.

---

## Services

### clickhouse-keeper

| Property | Value |
|---|---|
| Image | `clickhouse/clickhouse-keeper:25.8` |
| Role | Raft coordination — replication queues, distributed DDL, merge leader election |
| Host port | `9181` (ZooKeeper-compatible client port) |
| Internal Raft port | `9234` (peer-to-peer, not exposed to host) |

ClickHouse Keeper is a lightweight, built-in replacement for Apache ZooKeeper. It runs as a
**single-node Raft group** in this local setup — always the leader, no quorum required.
See [Scaling Keeper to Production](#scaling-keeper-to-production) for HA options.

Both data nodes declare `depends_on: clickhouse-keeper (service_healthy)`, so starting either
data node automatically starts Keeper first.

---

### clickhouse-01 / clickhouse-02

| Property | clickhouse-01 | clickhouse-02 |
|---|---|---|
| Image | `clickhouse/clickhouse-server:25.8` | `clickhouse/clickhouse-server:25.8` |
| Role | Data node, **replica 1** | Data node, **replica 2** |
| HTTP API (host) | `localhost:8123` | `localhost:8124` |
| Native TCP (host) | `localhost:9000` | `localhost:9001` |
| Shard / Replica macro | `shard1` / `replica1` | `shard1` / `replica2` |

Both nodes share the same configuration (users, Keeper address, init SQL) except for their
`macros.xml`, which assigns the unique `{replica}` value used by `ReplicatedMergeTree` engine
paths. All tables use `Replicated*MergeTree` without explicit ZooKeeper paths — the `Replicated`
database engine manages paths automatically.

The application connects to **clickhouse-01 only**. clickhouse-02 exists to verify replication
correctness in integration tests.

---

### ca-event-manager

| Property | Value |
|---|---|
| Image | `ghcr.io/dotcms/dot-ca-event-manager:latest` |
| Role | Spring Boot analytics API |
| Host port | `8080` |
| ClickHouse target | `clickhouse-01:8123` |

The service is only included in the **full stack** (`docker-compose.yml`). For day-to-day
development you typically run the app with `mvn spring-boot:run` on the host and start only the
ClickHouse containers.

---

## Directory Layout

```
analytics/
├── docker-compose.yml                  # Main compose file (full stack)
│
├── conf/
│   ├── keeper/
│   │   └── keeper_config.xml           # Keeper Raft config (single-node)
│   ├── clickhouse-01/
│   │   └── macros.xml                  # {shard=shard1, replica=replica1}
│   ├── clickhouse-02/
│   │   └── macros.xml                  # {shard=shard1, replica=replica2}
│   ├── users.xml                       # ClickHouse admin user definition
│   └── zookeeper.xml                   # Keeper endpoint for data nodes
│
└── init/                               # SQL files run by clickhouse-01 on first start
    ├── 01-init.sql                     # CREATE DATABASE analytics (Replicated engine)
    ├── 10-global.sql                   # Raw events table + data-skipping indexes
    ├── 20-event-data.sql               # Content analytics tables + materialized views
    ├── 30-conversion-data.sql          # Conversion attribution tables + MVs
    ├── 40-session-engagement-data.sql  # Session engagement pipeline tables + MVs
    └── 50-users.sql                    # Default customer user (cust-001)
```

> **Note:** `init/` is mounted on **both data nodes** (as `/docker-entrypoint-initdb.d`). All
> `CREATE` statements use `IF NOT EXISTS`, so when clickhouse-02 runs the same scripts it finds
> objects already replicated and skips them gracefully.

---

## Running the Stack

### ClickHouse only (recommended for development)

Starts Keeper and both data replicas. Run the application separately with `mvn spring-boot:run`.

```bash
cd docker
docker compose up -d clickhouse-01 clickhouse-02
```

Wait for both nodes to be healthy before starting the app:

```bash
docker compose ps
```

### Full stack

```bash
cd docker
docker compose up
```

Pulls `ghcr.io/dotcms/dot-ca-event-manager:latest` from GHCR, starts ClickHouse, and runs the
app on port `8080`.

---

## Configuration Files

| File | Purpose |
|---|---|
| `conf/keeper/keeper_config.xml` | Keeper Raft config: port 9181, single-node group, log/snapshot paths |
| `conf/zookeeper.xml` | Tells each data node where to find Keeper (`clickhouse-keeper:9181`) |
| `conf/clickhouse-01/macros.xml` | Node macros: `{shard}=shard1`, `{replica}=replica1` |
| `conf/clickhouse-02/macros.xml` | Node macros: `{shard}=shard1`, `{replica}=replica2` |
| `conf/users.xml` | Defines the `admin` user (password: `admin`, full access management) |

---

## Database Initialization

The `init/` scripts run in filename order on first start. Both nodes mount `init/` — all
`CREATE` statements use `IF NOT EXISTS`, so scripts are safe to run on both replicas.

| Script | What it creates |
|---|---|
| `01-init.sql` | `analytics` database (`Replicated` engine), admin row policy |
| `10-global.sql` | `analytics.events` — raw event ingestion table (`ReplicatedMergeTree`) |
| `20-event-data.sql` | `content_events_counter` + `pageviews_by_device_browser_daily` + their materialized views |
| `30-conversion-data.sql` | `conversion_time`, `content_presents_in_conversion` + refreshable MV |
| `40-session-engagement-data.sql` | Full session engagement pipeline: `session_states` → `session_facts` → `session_facts_latest` → roll-up tables (`engagement_daily`, `sessions_by_device_daily`, `sessions_by_browser_daily`, `sessions_by_language_daily`) |
| `50-users.sql` | Creates `cust-001` with a row policy scoped to `customer_id='cust-001'` |

> `CLICKHOUSE_DB` is intentionally **not set** in `docker-compose.yml`. Setting it would cause
> Docker's entrypoint to pre-create the database as a plain (non-replicated) engine before the
> init scripts run, making the `CREATE DATABASE … ENGINE = Replicated(…)` in `01-init.sql` a
> no-op.

---

## Default Credentials

| User | Password | Scope |
|---|---|---|
| `admin` | `admin` | Full access, all databases |
| `cust-001` | `abc` | `analytics` database, rows where `customer_id = 'cust-001'` |

These are **local development defaults only**. All passwords must be rotated in any
non-development environment.

---

## Ports at a Glance

| Host port | Container | Protocol | Notes |
|---|---|---|---|
| `8123` | clickhouse-01 | HTTP | Primary ClickHouse HTTP API |
| `9000` | clickhouse-01 | TCP | ClickHouse native protocol |
| `8124` | clickhouse-02 | HTTP | Replica HTTP API (tests only) |
| `9001` | clickhouse-02 | TCP | Replica native protocol (tests only) |
| `9181` | clickhouse-keeper | TCP | ZooKeeper-compatible Keeper client port |
| `8080` | ca-event-manager | HTTP | Analytics REST API |

---

## Scaling Keeper to Production

The current single-node Keeper provides **no high availability**. If the Keeper container goes
down, the data nodes can still serve reads but cannot commit new inserts or run replicated DDL
until the connection is restored.

For a fault-tolerant cluster, run an **odd number of Keeper nodes** (minimum 3):

| Keeper nodes | Can lose | Quorum |
|---|---|---|
| 1 | 0 | 1 of 1 — no HA |
| 3 | 1 | 2 of 3 |
| 5 | 2 | 3 of 5 |

Steps to expand:
1. Add `clickhouse-keeper-2` and `clickhouse-keeper-3` containers, each with its own
   `keeper_config.xml` that has a unique `<server_id>` and all three servers listed in
   `<raft_configuration>`.
2. Update `conf/zookeeper.xml` on every data node to list all three Keeper endpoints.
3. Restart the cluster.

See the comments inside `conf/keeper/keeper_config.xml` and `conf/zookeeper.xml` for full
configuration examples.