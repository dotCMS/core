workflow "testing" {
  on = "push"
  resolves = ["build-dotcms-docker"]
}

action "build-dotcms-docker" {
  uses = "dotcms/docker/images/dotcms@dotcms-src-seed"
  env = {
    MY_NAME = "Monas"
  }
  args="--build-arg BUILD_FROM=COMMIT --build-arg BUILD_ID=c4e97b3"
}
