# BUILD dotCMS Java Base

## Native libvips colour-safety mitigation

Highway SIMD is disabled in the libvips build as a conservative mitigation for
resize colour corruption at horizontal shrink factors of 4× or more
([#38079](https://github.com/dotCMS/core/issues/38079)). Image formats remain supported,
but SIMD-accelerated operations may be slower. The failing CPU target is not yet confirmed.
The native verifier rejects Highway-enabled builds. The Java integration test
`VipsResizeImageFilterIntegrationTest` checks JPEG and RGB/RGBA PNG/WebP colours against
known Java-generated pixels across the shrink boundary, using a navy region and a 2D gradient.

Rebuild and publish `dotcms/java-base:<java-version>` for **both architectures**, then
rebuild the dependent runtime and dev-env images. JVM CI extracts the published base
image, so its new build-policy check will fail until that image is republished.
Existing corrupt renditions in `dotGenerated` and CDN caches must also be invalidated
through the normal cache-maintenance procedure; rebuilding does not repair cached images.
Do not delete original assets. The separate EXIF-orientation sizing defect is unchanged.

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

Use the version specified
```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/java-base:ms-jdk-21.09 .
```

or specify a version
```
docker buildx build --platform linux/amd64,linux/arm64 --pull --push -t dotcms/java-base:21.2.0.r11-grl .

```
