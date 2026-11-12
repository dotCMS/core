# BUILD dotCMS Java Base

This Dockerfile builds the base container woth java that will be used to build and run dotCMS. 

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

Build and publish the version pinned in `.sdkmanrc` (from this directory):
```bash
JAVA_VERSION="$(sed -n 's/^java=//p' ../../.sdkmanrc)"
docker buildx build --platform linux/amd64,linux/arm64 --pull --push \
  --build-arg SDKMAN_JAVA_VERSION="$JAVA_VERSION" \
  -t "dotcms/java-base:${JAVA_VERSION//+/-}" .
```

Publish this base image before building downstream dotCMS images that reference its tag.

### eMudhra certificate distrust

Microsoft Java `25.0.5.0+2-ms` contains the CVE-2026-91403 intermediate-certificate
blocklist. The build also applies [OpenJDK's root-removal mitigation](https://openjdk.org/emudhra)
for the follow-up CVE-2026-114820 to `/java/lib/security/cacerts`. Certificates chaining
only to these removed roots will no longer be trusted. Custom mounted truststores
and developer SDKMAN installations require separate mitigation.

Test removal with a JDK on `PATH`:
```bash
bash remove-compromised-cas.test.sh
```
