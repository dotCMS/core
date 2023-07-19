# dotCMS Cluster Mode with Redis Session Manager

This is an example of a 2-node dotCMS Cluster running on ports 8081 and 8082 respectively, using the Redis Session Manager feature. For more information, please refer to the plugin's repository: https://github.com/dotCMS/tomcat-redis-session-manager

The Redis server runs on port 6379 with password `MY_SECRET_P4SS`.

## Usage

#### Environment setup

1) A local path to license pack must be set here:
```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```
The license pack must contain at least two licenses (one for each node in the cluster).

2) A local path to access data in the instance can be set uncommenting this line:
```
#- {local_data_path}:/data/shared
```

3) A custom starter can be set through this line (uncomment and change the starter URL accordingly):
```
#"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20230712/starter-20230712.zip'
```

4) The most important configuration parameters have already been set in both `docker-compose.yml` files. Please refer to the plugin's official repository above for additional ones.


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


#### Un-deploying nodes:

```bash
docker-compose -f docker-compose-node-2.yml down
docker-compose -f docker-compose-node-1.yml down
```

**Important note:** `ctrl+c` does not destroy instances


