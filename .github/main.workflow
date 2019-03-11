workflow "testing" {
  on = "push"
  resolves = ["build-dotcms-docker"]
}

action "build-dotcms-docker" {
  uses = "dotcms/docker/images/dotcms@dotcms-src-seed"
  env = {
    MY_NAME = "Monas"
  }
  args="--build-arg COMMIT=c4e97b3"
}
