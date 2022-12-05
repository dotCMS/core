# DOTCMS SRC SEED

This image contains the source files of dotCMS.  It consists of a clone of the dotcms git repo and includes the pre-downloaded gradle dependices from the time this image was created.  It is intended to act as the build seed when building dotcms images, so these dependices do not need to be downloaded with every build

## How to update
```
docker build --pull --no-cache -t dotcms-seed .
docker login
docker tag dotcms-seed dotcms/dotcms-seed:latest
docker push dotcms/dotcms-seed

```
