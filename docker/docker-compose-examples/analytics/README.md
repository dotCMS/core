# Content Analytics Infrastructure

The Docker Compose setup for the dotCMS Content Analytics infrastructure
(ClickHouse cluster + `ca-event-manager`) lives in its own repository to avoid
duplicating configuration files across repos:

**https://github.com/dotCMS/dot-ca-event-manager**

Refer to the `docker/analytics-infra-example/` directory in that repository for the full setup,
including the ClickHouse keeper, replica nodes, initialization scripts, and
the event manager service.