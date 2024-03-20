# Deploy Docker Artifact Action

This GitHub Action, named "Deploy Docker Artifact", is designed to build and push a Docker image to a specified registry. The Docker context for this action is passed from a run.

## Inputs

The action accepts the following inputs:

- `docker_platforms`: Docker platforms to build the image on. Default is 'linux/amd64'.
- `docker_registry`: Docker registry to push the image to (DOCKER.IO, GHCR.IO, BOTH). Default is 'DOCKER.IO'.
- `build_run_id`: The run id of the build to pull the artifact from. This is required.
- `commit_id`: The commit id that triggered the build. This is required.
- `variant`: The branch or type of build to tag the image with. This is required.
- `docker_tag`: The docker tag to use for the image. This is required.
- `do_deploy`: Actually do the final deploy, set to false for testing. Default is 'true'.
- `docker_io_username`: Docker.io username.
- `docker_io_token`: Docker.io token.
- `ghcr_io_username`: GHCR.io username.
- `ghcr_io_token`: GHCR.io token.
- `github_token`: GitHub token. This is required.

## Usage

Here is an example of how to use this action in a workflow:

```yaml
- name: Deploy Docker Artifact
  uses: ./.github/actions/deploy-artifact-docker
  with:
    docker_platforms: linux/amd64,linux/arm64
    build_run_id: ${{ github.event.workflow_run.id }}
    commit_id: ${{ github.event.workflow_run.head_sha }}
    variant: dotcms/dotcms:master
    docker_tag: dotcms/dotcms:master_latest_SNAPSHOT
    do_deploy: false
    docker_io_username: ${{ secrets.DOCKER_USERNAME }}
    docker_io_token: ${{ secrets.DOCKER_TOKEN }}
    github_token: ${{ secrets.GITHUB_TOKEN }}
```

This action is part of a larger workflow and is designed to work in conjunction with other actions. It is used to build and push a Docker image as part of a continuous integration/continuous deployment (CI/CD) pipeline.