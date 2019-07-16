# Compose files

## How to run

### postgres
```
docker-compose -f postgres-docker-compose.yml build
docker-compose -f postgres-docker-compose.yml up
```

#### Running with options to shutdown other services (db) as soon as the tests finished to run
```
docker-compose -f postgres-docker-compose.yml up \
    --abort-on-container-exit \
    --exit-code-from integration-tests
```

### mysql
```
docker-compose -f mysql-docker-compose.yml build
docker-compose -f mysql-docker-compose.yml up
```

#### Running with options to shutdown other services (db) as soon as the tests finished to run
```
docker-compose -f mysql-docker-compose.yml up \
    --abort-on-container-exit \
    --exit-code-from integration-tests
```

## Useful commands
### Building a given compose file

`docker-compose -f postgres-docker-compose.yml build`

### Running a given compose file

`docker-compose -f postgres-docker-compose.yml up`

*Detach mode:*

`docker-compose -f postgres-docker-compose.yml up -d` 

### Checking the status of the containers for a given compose file

`docker-compose -f postgres-docker-compose.yml ps`

### Shutting down the containers of a given compose file

`docker-compose -f postgres-docker-compose.yml down`

### Explore a container internal content executing bash on the container

* Check the running containers for a given compose file

    `docker-compose -f postgres-docker-compose.yml ps`

* Select one to explore with bash [using the container name] 

    `docker exec -t -i integration-tests_integration-tests_1 /bin/bash`

## Tip [-d]
The standard output of _docker-compose_ up may hang occasionally, leaving you to think that the 
application is not responding. Hence, you can run containers detached with the `-d` flag and tail 
the container logs manually with `docker-compose logs --follow [SERVICE…]`

`docker-compose -f postgres-docker-compose.yml logs --follow integration-tests_integration-tests_1`

---
---
---

# Using the docker Image

### Arguments for building dotCMS docker image: 

|  BUILD_FROM  | BUILD_ID                     | DB_TYPE |
| ------------ | ---------------              | --------------- |
| COMMIT       | Commit hash or branch name to use for build | One of 4 options ["postgres", "mysql", "oracle", "mssql"] |
| TAG          | Tag to use for build         | |


## Examples 

### BRANCH Example 
Where your branch name is `pre-release-5.0.3`.  In this case, becuase a branch is a movable pointer, you need to prune your
images before building in order to purge your image cache and get a clean build.
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=origin/test-issue-16372-create-schema-in-integration-tests -t integration-tests .

docker run -it integration-tests
```

### COMMIT Example 
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=c4e97b3 -t integration-tests .

docker run -it integration-tests
```

### TAG Example 
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=TAG --build-arg BUILD_ID=4.2.3-beta -t integration-tests .

docker run -it integration-tests
```