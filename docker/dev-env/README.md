# dotCMS Development Docker Image
### All in one docker image including Postgres and Opensearch
This image, intended for development, runs Ubuntu 22.04 and contains dotCMS, Postgres 16 and Opensearch 1.x. All dotCMS, db and es index data is stored in the `/data` directory, which should be mapped in if you want your environment to persist.  The beauty of this image that it can be used to CLONE an existing dotCMS instance.  


## Running the image
This image takes all the normal dotCMS docker config switches - keep in mind that the DB and ES come pre-wired, so no need to change those.  This image also takes the following env variables:

- `DOTCMS_SOURCE_ENVIRONMENT` : the url for the environment you wish to clone, e.g. https://demo.dotcms.com .
- `DOTCMS_CLONE_TYPE` : either `dump` or `starter`, defaults to `dump`.  Set this to dump to take a database dump and asset backup (recommended for large sites).  Set this to `starter` to force the target environment to generate a starter to download.
- `DOTCMS_API_TOKEN` : A valid dotCMS API Token from an admin user in the source environment.
- `DOTCMS_USERNAME_PASSWORD` :  The username:password for an admin user in the source environment.
- `DOTCMS_DEBUG` :  Run dotCMS in debug mode and listen for a remote debugger on port 8000, defaults to `false`.
- `ALL_ASSETS` : Controls whether old versions of assets are included in the download, defaults to false, which means only the current live and working versions of assets will be downloaded.



## Cloning a dotCMS Environment
When running this image, if you specify a source environment and a valid means to authenticate, the image will attempt to pull the **assets** and **db** from the source environment.  To do this, you start the image up and pass it a `DOTCMS_SOURCE_ENVIRONMENT` and either an `DOTCMS_API_TOKEN` or `DOTCMS_USERNAME_PASSWORD` (e.g. `admin@dotcms.com:admin`).  On startup, the image will try to reach out and download the database and assets from the specified dotCMS instance, load the db and assets and start dotCMS in debug mode. Once the server starts, you need to run a full reindex.



All of these examples expect you to login via https on port 8443.  There is a valid certificate if you are running locally using:

https://local.dotcms.site:8443/dotAdmin

#### Clone demo with a dotCMS API Token
This pulls down the assets and a SQL dump that is then imported into the new dotCMS instance.  You will need to login with the same credentials that are used in the target environment.
```
export TOK=XXXXXXX_YOUR_DOTCMS_TOKEN.eXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

docker run --rm \
--pull always \
-p 8000:8000 \
-p 8443:8443 \
-v $PWD/data:/data \
-e DOTCMS_SOURCE_ENVIRONMENT=https://demo.dotcms.com \
-e DOTCMS_API_TOKEN=$TOK \
 dotcms/dotcms-dev:nightly
```

#### Clone demo with UserID/Password
This pulls down the assets and a SQL dump that is then imported into the new dotCMS instance.  You will need to login with the same credentials that are used in the target environment.
```
docker run --rm \
--pull always \
-p 8443:8443 \
-v $PWD/data:/data \
-e DOTCMS_SOURCE_ENVIRONMENT=https://demo.dotcms.com \
-e DOTCMS_USERNAME_PASSWORD="admin@dotcms.com:admin" \
 dotcms/dotcms-dev:nightly
```


#### Clone demo using a starter.zip 
This asks the source server to generate a starter.zip, which can be time-consuming to generate AND to import initially.  
```
docker run --rm \
--pull always \
-p 8443:8443 \
-v $PWD/data:/data \
-e DOTCMS_SOURCE_ENVIRONMENT=https://demo.dotcms.com \
-e DOTCMS_USERNAME_PASSWORD="admin@dotcms.com:admin" \
-e DOTCMS_CLONE_TYPE=starter \
dotcms/dotcms-dev:nightly
```


#### DEV DEBUG - with Postgres port exposed.  
In this case dotCMS java waits to start up until a debugger is connected to it on port 8000.
```
docker run --rm  \
--pull always \
-p 8443:8443 \
-p 5432:5432 \
-p 8000:8000 \
-v $PWD/data:/data \
-e DOTCMS_DEBUG=true \
 dotcms/dotcms-dev:nightly
```


#### Troubleshooting the Download
Due to a bug in docker, downloading large environments can time out. To get around this, you can download the assets and db yourself (outside of docker) to seed your installation and add them to the data volume you map into the image.  dotCMS will look for `/data/assets.zip` and/or `/data/dotcms_db.sql.gz` to import before running the normal starter import routine.

#### Downloading your assets and db outside of Docker
dotCMS offers two admin only endpoints to download your data and assets
- `/api/v1/maintenance/_downloadAssets`
- `/api/v1/maintenance/_downloadDb`

When downloading assets, you can specify `?oldAssets=false`, and dotCMS will only include the assets for live and working versions of your content, thus hopefully generating a MUCH smaller download

#### Example wget to download assets
```
wget --header="$AUTH_HEADER" -t 1 -O assets.zip  $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadAssets?oldAssets=false
```

#### Example wget to download your DB
```
wget --header="$AUTH_HEADER" -t 1 -O dotcms_db.sql.gz $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadDb 

```

#### Example wget to download a new starter.zip
```
wget --header="$AUTH_HEADER" -t 1 -O starter.zip $DOTCMS_SOURCE_ENVIRONMENT/api/v1/maintenance/_downloadStarterWithAssets?oldAssets=false

```



#### Starting from a clean slate
Your development instance can be deleted and reset by deleting the ./data directory that is mapped in. 




## Building this Image
By default, this image is built from the `dotcms/dotcms:latest` tagged version of dotCMS.  You can specify another dotCMS version you want use for your dev instance by passing the build-arg `DOTCMS_DOCKER_TAG` to indicate which dotCMS image tag to use to build,  e.g.
`--build-arg DOTCMS_DOCKER_TAG=latest` or `--build-arg DOTCMS_DOCKER_TAG=23.07`

```
docker build --pull --build-arg DOTCMS_DOCKER_TAG=latest --progress=plain --load . -t dotcms/dotcms-dev:testing
```
or
```
docker buildx build --build-arg DOTCMS_DOCKER_TAG=trunk --platform linux/amd64,linux/arm64 --pull --push -t dotcms/dotcms-dev:testing .
```

then
```

docker run --rm \
-p 8000:8000 \
-p 8443:8443 \
-v $PWD/data:/data 
dotcms/dotcms-dev:testing
```


### Included Database and Elasticsearch

This image runs the following servers internally. 

#### Opensearch 1.3.11
Running https on 
- https on port 9200 
- basic auth (admin/admin)
- data stored in /data/opensearch 


#### Postgres 16
Running on port 5432 and using the dotCMS defaults:
- db: dotcms
- user: dotcmsdbuser
- pass: password
- data stored in /data/postgres

If you wish to connect to these instances remotely, you will need to expose their ports in docker when you run the image, e.g.
