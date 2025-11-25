# DEPRECATED
Moved to java-base/Dockerfile

# BUILD dotCMS pg_dump

This Dockerfile builds postgres so we can extract a valid pg_dump binary.  pg_dump is backwards compatible so any version greater than our cloud db version should work future forward.

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

Use the version specified
```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/pg-base:16.2 .
```

or specify a version
```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/pg-base:21.2.0.r11-grl .

```
