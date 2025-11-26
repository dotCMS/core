# Dotcms Single Node With Demo Content

A single instance of dotcms running on port 8080 that will download and install the demo site on initialization. Database: postgres

## Usage
`scripts/dotcms-get-demo-site-starter-urls.sh` prints the correct demo site starter URL for each dotCMS version

#### Environment setup
Specifiy a custom starter that will be included:
```
CUSTOM_STARTER_URL: 'https://repo.dotcms.com/artifactory/libs-release-local/com/dotcms/xxxxxxxxxx.zip'
```
A local path to license pack can be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
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


