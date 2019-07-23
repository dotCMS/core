workflow "Run tests" {
  on = "pull_request"
  resolves = ["Run Angular Tests in dotcms-ui"]
}

action "Run Angular Tests in dotcms-ui" {
  uses = "actions/npm@59b64a598378f31e49cb76f27d6f3312b582f680"
  runs = "ng test dotcms-ui"
}
