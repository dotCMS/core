# Dotcms Single Node With Demo Content

A single instance of dotcms running on port 8080 that will download and install the demo site on initialization. Database: postgres

## Usage

#### Environment setup


1) A local path to license pack can be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

You can specifiy a custom starter that will be included:
```
"CUSTOM_STARTER_URL": "https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/xxxxxxxxxx.zip"
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


