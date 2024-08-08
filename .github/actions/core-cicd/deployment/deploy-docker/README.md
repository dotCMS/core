# Deploy Docker Artifact Action

This GitHub Action, named "Deploy Docker Artifact", is designed to build and push a Docker image to a specified registry. The Docker context for this action is passed from a run.

## Inputs

The action accepts the following inputs:

- `docker_platforms`: Docker platforms to build the image on. Default is 'linux/amd64'.
- `docker_registry`: Docker registry to push the image to (DOCKER.IO, GHCR.IO, BOTH). Default is 'DOCKER.IO'.
- `build_run_id`: The run id of the build to pull the artifact from. This is required.
- `docker_context` : The docker context from the docker directory to use for the build. This is required if build-run-id is not used.
- `commit_id`: The commit id that triggered the build. This is required.
- `image_name`: The name of the image to build. e.g 'dotcms/dotcms'
- `ref`: The the branch or tag that triggered the build e.g. the docker environment name "trunk", "nightly" if "release" then the name will not be used in the tag.
- `docker-use-ref`: If true, the ref will be used in the tag if this is false then version must be specified
- `docker_tag`: The docker tag to use for the image. This is required.
- `latest` : If true, the image will be tagged as latest, usually reserved for the agile release e.g. dotcms/dotcms:latest
- `version`: The release version of the image to tag for snapshot builds this is unset and ref is used.
- `do_deploy`: Actually do the final deploy, set to false for testing. Default is 'true'.
- `docker_io_username`: Docker.io username.
- `docker_io_token`: Docker.io token.
- `ghcr_io_username`: GHCR.io username.
- `ghcr_io_token`: GHCR.io token.
- `github_token`: GitHub token. This is required.
- `build_args`: Build arguments to pass to the docker build command. Default is ''.

## Usage

Here is an example of how to use this action in a workflow:

```yaml
- name: Build/Push Docker Dev Image
        id: docker_build_dev
        if: inputs.deploy-dev-image
        uses: ./.github/actions/core-cicd/deployment/deploy-docker
        with:
          image_name: dotcms/dotcms-dev
          docker_platforms: linux/amd64,linux/arm64
          docker_context: dev-env
          commit_id: ${{ github.sha }}
          ref: trunk
          do_deploy: ${{ vars.DOCKER_DEPLOY || 'true' }} # default to true, set to disable in fork
          docker_io_username: ${{ secrets.DOCKER_USERNAME }}
          docker_io_token: ${{ secrets.DOCKER_TOKEN }}
          github_token: ${{ secrets.GITHUB_TOKEN }}
          # Make value below which is source dotcms/dotcms tag to base dev image on dynamic to use whatever tag created above
          build_args: |
            DOTCMS_DOCKER_TAG=trunk

This action is part of a larger workflow and is designed to work in conjunction with other actions. It is used to build and push a Docker image as part of a continuous integration/continuous deployment (CI/CD) pipeline.