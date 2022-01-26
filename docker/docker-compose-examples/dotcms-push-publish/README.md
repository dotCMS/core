# Dotcms Push Publish

Push publish environment where the sender runs on port 8080 and the receiver on 8081. Database: postgres.

**Important note:**  For the endpoint configuration, the local IP address must be used instead of `localhost` or `127.0.0.1`

## Usage

####Environment setup


1) A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

The license pack must contain at least two licenses (one for each node in the cluster)


2) A local path to access data in the instance can be set uncommenting this line: 

```
#- {local_data_path}:/data/shared
```

3) A custom starter can be set through this line (uncomment and change the starter url accordingly): 

```
#"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20210920/starter-20210920.zip'
```

####Deploying nodes:

```bash
docker-compose -f docker-compose-sender.yml up
docker-compose -f docker-compose-receiver.yml up

```

####Undeploying nodes:

```bash
docker-compose -f docker-compose-sender.yml down
docker-compose -f docker-compose-receiver.yml down
```

**Important note:** `ctrl+c` does not destroy instances
