# Dotcms Single Node

A single instance of dotcms running on port 8080. Database: postgres

## Usage

#### Environment setup


1) A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

2) A local path to access data (eg: assets) in the instance can be set uncommenting this line and adjusting './assets' to point to where your downloaded assets are:

```
#- ./assets:/data/shared/assets
```

3) A custom starter can be set through this line (uncomment and change the starter url accordingly): 

```
#"CUSTOM_STARTER_URL": 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/starter/20210920/starter-20210920.zip'
```

#### Run an example:

```bash
docker-compose up
```

#### Shut down instances:

```bash
docker-compose down
```

#### Importing database from an export:

Start the db container first, and then import your data before starting the entire cluster of services:

```bash
docker-compose start db
```

Get the container id of the database:

```bash
docker ps
```

Import your data:

```bash
cat your-exported-data.sql | docker exec -i <container id> psql -U dotcmsdbuser dotcms
```

Start the remaining containers:

```bash
docker-compose up
```

You will need to re-index once your site is up and running and then your data should be available from within the backend.

**Important note:** `ctrl+c` does not destroy instances
