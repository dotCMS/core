# Dotcms Cluster Mode with Redis

Cluster with 2 dotCMS instances running on ports 8080 and 8081 respectively. Database: postgres.

This environment uses Redis Pub/Sub and Cache. Redis runs on port 6379 with password `MY_SECRET_P4SS`

## Usage

#### Environment setup

1. A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

The license pack must contain at least two licenses (one for each node in the cluster)

2. A local path to access data in the instance can be set uncommenting this line:

```
#- {local_data_path}:/data/shared
```

3. A custom starter can be set through this line (uncomment and change the starter url accordingly):

```
#"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20250722/starter-20250722.zip'
```

#### Deploying nodes:

```bash
docker-compose -f docker-compose-node-1.yml up

```

Once the node 1 is running, deploy node 2:

```bash
docker-compose -f docker-compose-node-2.yml up

```

#### Undeploying nodes:

```bash
docker-compose -f docker-compose-node-2.yml down
docker-compose -f docker-compose-node-1.yml down
```

**Important note:** `ctrl+c` does not destroy instances
