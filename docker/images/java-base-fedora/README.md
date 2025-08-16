# BUILD dotCMS Java 8 Base + ShenandoahGC 

This Dockerfile builds the base container that will be used to run dotCMS. It uses fedora linux as that is the only distro with an openjdk8 that includes the backported ShenandoahGC


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
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/fedora-java-base:8-shenandoah .
```
