# dotCMS All in Wonder
This image runs in Ubuntu 22.04 and contains Postgres 14 and Opensearch 1.x, as well as dotCMS. All data is contained in the /data directory, which should be mapped in if you want your enviornment to persist.

### Opensearch 
Found in `/usr/shared/opensearch`   
Running https with username/password = admin/admin

### Postgres 
Configured to use the dotCMS defaults:
db: dotcms
user: dotcmsdbuser
pass: password


## Pulling from an Environment
By specifying a `DOTCMS_SOURCE_ENVIRONMENT` and a username/password or token, this image will attempt to pull the assets and db from the source environment. Due to a bug in docker, large environments will time out.  To get around this, you can pull the assets and db yourself (outside of docker) to seed your installation and add them to the volume you map into the image.  dotCMS will look for `/data/shared/assets.zip` and `/data/shared/dotcms_db.sql.gz` to import before running the normal starter import routine.

### Pulling assets:
```
wget --header="$AUTH_HEADER" -t 1 -O assets.zip  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets 
```

### Pulling DB
```
wget --header="$AUTH_HEADER" -t 1 -O dotcms_db.sql.gz $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb 

```


## Running
```
export TOK=XXXXXXXXXXXXXXXXXXXXXXXX.eXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
## Build and Run with a Token
```
docker build . -t dotcms/dev &&\
docker run -it \
-p 8443:8443 \
-v $PWD/data:/data \
-e DOTCMS_SOURCE_ENVIRONMENT=https://demo.dotcms.com \
-e DOTCMS_API_TOKEN=$TOK \
 dotcms/dev:latest
```

## Build and Run with UserID/Password
```
docker build . -t dotcms/dev &&\
docker run -it \
-p 8443:8443 \
-v $PWD/data:/data \
-e DOTCMS_SOURCE_ENVIRONMENT=https://demo.dotcms.com \
-e DOTCMS_USERNAME_PASSWORD="admin@dotcms.com:admin" \
 dotcms/dev:latest
```