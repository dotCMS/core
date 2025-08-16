# DOTCMS SRC SEED

This image contains the source files of dotCMS.  It consists of a clone of the dotcms git repo and includes the pre-downloaded gradle dependices from the time this image was created.  It is intended to act as the build seed when building dotcms images, so these dependices do not need to be downloaded with every build

## How to build/update
```

docker build --pull  -t dotcms/dotcms-seed:5.1.6ks .



docker buildx build --no-cache --platform linux/amd64,linux/arm64 --pull --push  -t dotcms/dotcms-seed:5.1.6ks .


```
