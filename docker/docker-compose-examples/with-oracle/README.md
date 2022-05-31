# Dotcms Single Node (Oracle Database)

A single instance of dotcms running on port 8080. Database: oracle

## Usage

#### Environment setup

1) Start the Oracle database using the example provided on this repo (view directory ***oracle-database***)

2) Set your IP address accordingly. It could be a local or a public ip (not localhost nor 127.0.0.1):

```
      "PROVIDER_DB_DNSNAME": '{ip_address}'
      "PROVIDER_DB_URL": 'jdbc:oracle:thin:@{ip_address}:1521:XE'
```

3) A local path to license pack must be set here:

```
- {license_local_path}/license.zip:/data/shared/assets/license.zip
```

4) A local path to access data in the instance can be set uncommenting this line: 

```
#- {local_data_path}:/data/shared
```

5) A custom starter can be set through this line (uncomment and change the starter url accordingly): 

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


