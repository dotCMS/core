# BUILD dotCMS Docker image from arguments 

This version of the dockerfile and requisite build files that are provided with the primary purpose of illustrating dotCMS-distributed image functionality. We encourage you to use the distribution images provided at  https://hub.docker.com/r/dotcms/dotcms/ rather than building your own image.  

Building custom images may create more complexity and challenges as it relates to the ongoing support of your installation.  Please contact support@dotcms.com to inquire about add-on product services available for extended support around Docker.  

Please note that the dotCMS image can be extended via static and dynamic plugins, as well as numerous extension points to the init and configuration functionality.  If you need customized functionality please check documentation on using plugins with docker images before assuming you need to build a custom docker image. 

dotCMS has architected this image to handle many different use cases, providing the best dotCMS product experience while still enabling use of custom functionality with the genuine dotCMS image.

Please send any thoughts or suggestions to improve our docker image to support@dotcms.com. 

If you feel you must create your own custom docker image(s), please contact dotCMS Support to discuss product compatibility and scope-of-support concerns prior to implementation.


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

docker buildx build --pull --platform linux/amd64,linux/arm64 --no-cache --progress=plain   --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=origin/5.1.6.k -t dotcms-test .


```


### COMMIT Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=c4e97b3 -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```

### TAG Example 
```
docker build --pull --no-cache --build-arg BUILD_FROM=TAG --build-arg BUILD_ID=origin/ -t dotcms-test .

docker run -it -p 8080:8080  --rm dotcms-test
```
