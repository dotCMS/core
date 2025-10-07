# dotCMS Cluster Mode with Redis Session Manager

This is an example of a 2-node dotCMS Cluster running on ports 8081 and 8082 respectively, using the Redis Session Manager feature. For more information, please refer to the plugin's repository: https://github.com/dotCMS/tomcat-redis-session-manager

The Redis server runs on port 6379 with password `MY_SECRET_P4SS`.

## Usage

#### Environment setup

1. A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

The license pack must contain at least two licenses (one for each node in the cluster).

2. A local path to access data in the instance can be set uncommenting this line:

```
#- {local_data_path}:/data/shared
```

3. A custom starter can be set through this line (uncomment and change the starter URL accordingly):

```
#"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20250722/starter-20250722.zip'
```

4. The most important configuration parameters have already been set in both `docker-compose.yml` files. Please refer to the plugin's official repository above for additional ones.

#### Deploying nodes:

```bash
docker-compose -f docker-compose-node-1.yml up
```

Once the node 1 is running, deploy node 2:

```bash
docker-compose -f docker-compose-node-2.yml up
```

#### Checking Session persistence in Redis:

You can easily check that the dotCMS Sessions are being persisted in Redis by following these simple steps:

1. SSH into the Redis container:

```bash
docker exec -it with-redis-session-redis-1 /bin/bash
```

2. Run the following command to access the Redis CLI:

```bash
redis-cli -a MY_SECRET_P4SS
```

3. Log into the dotCMS back-end in both nodes.
4. Run this command to print all the keys stored in Redis:

```bash
KEYS *
```

4. You should see a list of keys similar to this one:

```
1) "dotcms-redis-cluster4E5ACD893B6250FACEB24CAE6F02581E"
2) "dotcms-redis-cluster31AE4BC07CF37EDA49323954DFDC5B25"
```

The key is composed of (1) the current Cluster ID -- which is optional -- and the actual JSESSION ID.

Additionally, at the beggining of the dotCMS log, you will see the folowing information related to the Redis Session Manager, and the most important configuration values that are being used to instantiate the service:

```
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.276 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal ====================================
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.277 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal Redis-managed Tomcat Session plugin
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.277 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal ====================================
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.277 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal - Attaching 'com.dotcms.tomcat.redissessions.RedisSessionManager' to 'com.dotcms.tomcat.redissessions.RedisSessionHandlerValve'
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.279 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeSerializer Attempting to use serializer: com.dotcms.tomcat.redissessions.JavaSerializer
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.280 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal - Initializing configuration parameters:
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.282 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_HOST: redis
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.283 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_PORT: 6379
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.283 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_PASSWORD: - Set -
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.284 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_SSL_ENABLED: false
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.284 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_SENTINEL_MASTER: null
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.285 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_SENTINELS: null
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.285 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_DATABASE: 0
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.286 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_TIMEOUT: 2000
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.286 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_SESSION_PERSISTENT_POLICIES: DEFAULT
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.287 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_MAX_CONNECTIONS: 128
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.288 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_MAX_IDLE_CONNECTIONS: 100
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.288 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_MAX_IDLE_CONNECTIONS: 32
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.289 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- DOT_DOTCMS_CLUSTER_ID (Redis Key Prefix): dotcms-redis-cluster
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.289 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.initializeConfigParams -- TOMCAT_REDIS_ENABLED_FOR_ANON_TRAFFIC: false
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.289 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal - Initializing Redis connection
with-redis-session-dotcms-node-1-1  | SLF4J: No SLF4J providers were found.
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  | SLF4J: Defaulting to no-operation (NOP) logger implementation
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  | SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details.
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  |
with-redis-session-dotcms-node-1-1  | 18-Jul-2023 18:30:02.378 INFO [main] com.dotcms.tomcat.redissessions.RedisSessionManager.startInternal - Successful! Redis-managed Tomcat Sessions will expire after 1800 seconds.
```

#### Un-deploying nodes:

```bash
docker-compose -f docker-compose-node-2.yml down
docker-compose -f docker-compose-node-1.yml down
```

**Important note:** `ctrl+c` does not destroy instances
