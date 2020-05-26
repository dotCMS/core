# DOTCMS INTEGRATION AND CURL TESTS SEED

This image contains the source files of dotCMS.  It consists of a clone of the dotcms git repo and includes the pre-downloaded gradle dependencies from the time this image was created.  
It is intended to act as the build seed when running dotcms integration and curl tests images, so these dependencies do not need to be downloaded with every build.

## How to update

##### Building locally and pushing to google registry
https://cloud.google.com/container-registry/docs/quickstart
```
docker build --pull --no-cache -t tests-seed .
gcloud auth configure-docker
docker tag tests-seed gcr.io/cicd-246518/tests-seed:latest
docker push gcr.io/cicd-246518/tests-seed:latest
```

##### Building with gcloud
```
gcloud builds submit --config cloudbuild.yaml .
```