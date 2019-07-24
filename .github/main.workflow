workflow "Run tests" {
  resolves = ["Run Angular Tests in dotcms-ui"]
  on = "pull_request"
}

action "Install dependencies" {
  uses = "actions/npm@59b64a598378f31e49cb76f27d6f3312b582f680"
  runs = "npm i"
}

action "Build angular libraries" {
  uses = "actions/npm@59b64a598378f31e49cb76f27d6f3312b582f680"
  needs = ["Install dependencies"]
  runs = "npm run build:libs"
}

action "Run Angular Tests in dotcms-ui" {
  uses = "actions/npm@59b64a598378f31e49cb76f27d6f3312b582f680"
  needs = ["Build angular libraries"]
  runs = "npm run test dotcms-ui"
}
