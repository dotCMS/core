workflow "testing" {
  on = "push"
  resolves = ["build-dotcms-docker"]
}

action "build-dotcms-docker" {
  uses = "dotcms/docker/images/dotcms@dotcms-src-seed"
  args="--build-arg COMMIT=$GITHUB_SHA"
}
