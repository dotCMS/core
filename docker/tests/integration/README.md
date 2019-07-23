# Compose files

## License set up
The integration tests require a valid license to run.
There are two ways to set up a license

##### 1. Using an environment variable you can export before to build the image

Example:
```
export LICENSE_KEY=...4isycnXe8nsiO...
docker-compose -f postgres-docker-compose.yml build
```

##### 2. Binding a license file (license.dat) to the docker image
1. Inside this folder create a **license** folder (docker/tests/integration/license)
2. Add a valid **license.dat** file inside the created folder
3. Uncomment the **bind** folder instructions in the **\*-docker-compose.yml** files:
    ```
    - type: bind
      source: ./license/license.dat
      target: /custom/dotsecure/license/license.dat
    ```
4. Run the build command. Example: 
    ```
    docker-compose -f postgres-docker-compose.yml build
    ```

## How to run

### postgres
```
docker-compose -f postgres-docker-compose.yml build
docker-compose -f postgres-docker-compose.yml up
```

##### Running with options to shutdown other services (db) as soon as the tests finished to run
```
docker-compose -f postgres-docker-compose.yml up \
    --abort-on-container-exit \
    --exit-code-from integration-tests
```

##### Shutdown (it is recommended to always execute the down command when the test finished to run)
```
docker-compose -f postgres-docker-compose.yml down
```

### mysql
```
docker-compose -f mysql-docker-compose.yml build
docker-compose -f mysql-docker-compose.yml up
```

##### Running with options to shutdown other services (db) as soon as the tests finished to run
```
docker-compose -f mysql-docker-compose.yml up \
    --abort-on-container-exit \
    --exit-code-from integration-tests
```

##### Shutdown (it is recommended to always execute the down command when the test finished to run)
```
docker-compose -f mysql-docker-compose.yml down
```

## Useful commands

#### --no-cache
`docker-compose -f postgres-docker-compose.yml build --no-cache`

#### Building a given compose file

`docker-compose -f postgres-docker-compose.yml build`

#### Running a given compose file

`docker-compose -f postgres-docker-compose.yml up`

*Detach mode:*

`docker-compose -f postgres-docker-compose.yml up -d`

#### Logging
You can run containers detached with the `-d` flag and tail 
the container logs manually with `docker-compose logs --follow [SERVICE…]`

`docker-compose -f postgres-docker-compose.yml logs --follow integration_integration-tests_1` 

#### Checking the status of the containers for a given compose file

`docker-compose -f postgres-docker-compose.yml ps`

#### Shutting down the containers of a given compose file

`docker-compose -f postgres-docker-compose.yml down`

#### Explore a container internal content executing bash on the container

* Check the running containers for a given compose file

    `docker-compose -f postgres-docker-compose.yml ps`

* Select one to explore with bash [using the container name] 

    `docker exec -t -i integration_integration-tests_1 /bin/bash`

---
---
---

## Using the docker Image

#### Arguments for building dotCMS docker image: 

|  BUILD_FROM  | BUILD_ID                     | DB_TYPE |
| ------------ | ---------------              | --------------- |
| COMMIT       | Commit hash or branch name to use for build | One of 4 options ["postgres", "mysql", "oracle", "mssql"] |
| TAG          | Tag to use for build         | |


### Examples 

#### BRANCH Example 
Where your branch name is `my-branch-name`.  In this case, becuase a branch is a movable pointer, you need to prune your
images before building in order to purge your image cache and get a clean build.
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=origin/my-branch-name -t integration-tests .

docker run -it integration-tests
```

#### COMMIT Example 
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=c4e97b3 -t integration-tests .

docker run -it integration-tests
```

#### TAG Example 
```
docker build --pull --no-cache --build-arg DB_TYPE=postgres --build-arg BUILD_FROM=TAG --build-arg BUILD_ID=5.1.6 -t integration-tests .

docker run -it integration-tests
```