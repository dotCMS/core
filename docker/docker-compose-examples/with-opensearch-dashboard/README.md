# Dotcms with Kibana

A single instance of dotcms running on port 8080. Kibana was included for torubleshooting and runs on port 5601. Database: postgres.

## Usage

#### Environment setup

1. A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

2. A local path to access data in the instance can be set uncommenting this line:

```
#- {local_data_path}:/data/shared
```

3. A custom starter can be set through this line (uncomment and change the starter url accordingly):

```
#CUSTOM_STARTER_URL: 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20250722/starter-20250722.zip'
```

#### Run an example:

```bash
docker-compose up
```

#### Shut down instances:

```bash
docker-compose down
```

**Important note:** `ctrl+c` does not destroy instances
