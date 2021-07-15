# BUILD dotCMS Java Base

This Dockerfile builds the base container that will be used to run dotCMS. It uses the tool sdkman (https://sdkman.io/) to supply the java version that will be included in the base container. You can specify a JAVA_VERSION to use as a build arg. The intent is to provide a simple that allow us to update the java versions that we use to run dotCMS. 

### Custom JAVA_VERSION :
Setting the build arg `JAVA_VERSION` allows you to specify any java version >= java 11 that is supplied by sdkman. To see what versions are available, install sdkman and run `sdk update` and then `sdk ls java` - the value you want to supply is found in the `Identifier` column.

### Building Multiarch
To enable "multiarch" building, you need to use `docker buildx` and have defined a new builder (you might need to enable "experimental" features in order to enable `docker buildx`). If you have `buildx` installed, you can see what buildx builders you have available by doing a `ls`:

```
docker buildx ls
```

To create and use a new multiarch builder run:
```
docker buildx create --name multiarch
docker buildx use multiarch
```

At this point, you can use `buildx` to build your image and target the platform(s) you want to build for.  Mostly, the same arguements apply, though `buildx` also allows you to immediatly push your new image to docker hub after it is built.

```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push --build-arg JAVA_VERSION=21.1.0.r11-grl  -t dotcms/java-base:multiarch-test .

```