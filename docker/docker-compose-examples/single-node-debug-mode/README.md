# Dotcms Single Node (Debug Mode)

A single instance of dotcms running on port 8080. Database: postgres. Debug mode enabled on port 8000

## Usage

#### Environment setup


1) A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

2) A local path to access data in the instance can be set uncommenting this line: 

```
#- {local_data_path}:/data/shared
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

**Important note:** `ctrl+c` does not destroy instances


4) Configure your IDE enabling remote debugging:

![Remote Debugging](https://github.com/dotCMS/core/blob/new-docker-compose-examples/docker/docker-compose-examples/single-node-debug-mode/Intellij%20Debug%20Mode.png?raw=true)





