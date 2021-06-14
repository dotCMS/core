# BUILD dotCMS Docker image from arguments 

This version of the dockerfile and requisite build files that are provided with the primary purpose of illustrating dotCMS distributed image functionality. We encourage you to use the distribution images provided at  https://hub.docker.com/r/dotcms/dotcms/ rather than building your own image.  

Please note that the dotCMS image can be extended via static and dynamic plugins, as well as numerous extension points to the init and configuration functionality.  If you need customized functionality please check documentation on using plugins with docker images before assuming you need to build a custom docker image. 

dotCMS has architected this image to handle many different use cases, providing the best dotCMS product experience while still enabling use of custom functionality with the genuine dotCMS image.

dotCMS support Mulit-Architecture builds and has support for both amd64 and arm64 architectures. See below for more information on how to build dotCMS for different architectures.

Please send any thoughts or suggestions to improve our docker image to support@dotcms.com. 


# Arguments for building dotCMS docker image: 

|  BUILD_FROM  | BUILD_ID                     |
| ------------ | ---------------              |
| TARBALL_URL  | URL to tar file              |
| RELEASE      | Release number               |
| COMMIT       | Commit hash or branch name to use for build |
| TAG          | Tag to use for build         |


## Examples 

### TARBALL_URL Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=TARBALL_URL --build-arg BUILD_ID=https://dotcms.com/contentAsset/raw-data/523ef132-4a0b-4f17-9d82-eb2cfec779c6/targz/dotcms-2018-03-09_22-10.tar.gz -t dotcms-test .

docker run -it -p 8080:8080 --rm dotcms-test
```

### RELEASE Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=RELEASE --build-arg BUILD_ID=5.2.0 -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```

### BRANCH Example 
Where your branch name is `master`.  In this case, becuase a branch is a movable pointer, you need to prune your
images before building in order to purge your image cache and get a clean build.
```
docker build --pull --no-cache --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=origin/master -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```


### COMMIT Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=c4e97b3 -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```

### TAG Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=TAG --build-arg BUILD_ID=4.2.3-beta -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```


# Multi-Architecture Images
As of v.5.3.9, dotCMS supports building images that target both amd64 and arm64 architecture.  

### Setup
To enable "multiarch" building, you need to use `docker buildx` and have defined a new builder (you might need to enable "experimental" features in order to enable `docker buildx`). If you have `buildx` installed, you can see what buildx builders you have available by doing a `ls`:

```
docker buildx ls
```

To create and use a new multiarch builder run:
```
docker buildx create --name multiarch
docker buildx use multiarch
```

### Building a MultiArch Image

At this point, you can use `buildx` to build your image and target the platform(s) you want to build for.  Mostly, the same arguements apply, though `buildx` also allows you to immediatly push your new image to docker hub after it is built.

```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=origin/master -t dotcms/dotcms:multiarch-test .

```