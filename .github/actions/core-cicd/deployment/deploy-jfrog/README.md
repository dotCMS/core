# Deploy Artifact to Artifactory GitHub Action

This GitHub Action is used to deploy the dotCMS artifacts to Artifactory. It includes steps to extract project modules, version, and repository details from the POM file and deploy artifacts to the specified Artifactory repository.

## Inputs

- **modules**: (Optional) Comma-separated list of modules to deploy. If not provided, it will be extracted from the project POM file.
- **exclude-ext**: (Optional) Comma-separated list of extensions to be excluded.
- **dry-run**: (Optional) Enable dry-run mode. If it sets `true` the action will not deploy artifacts to Artifactory.
- **version**: (Optional) Version of the artifacts to deploy. If not provided, it will be extracted from the project POM file.
- **artifactory-repository**: (Optional) Artifactory Repository. If not provided, it will be extracted from the project POM file.
- **artifactory-url**: (Optional) Artifactory URL. If not provided, it will be extracted from the project POM file.
- **artifactory-access-token**: (Optional) Artifactory Access Token. It takes precedence over `artifactory-username` and
  `artifactory-password`.
- **artifactory-username**: (Optional) Artifactory username.
- **artifactory-password**: (Optional) Artifactory password.
- **github-token**: (Required) GitHub Token.

## Outputs

This action does not produce explicit outputs but uses extracted values within the workflow.

## Example Usage

```yaml
name: Deploy Artifact to Artifactory

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Deploy Artifact
        uses: ./.github/actions/core-cicd/deployment/deploy-jfrog
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          artifactory-access-token: ${{ secrets.ARTIFACTORY_ACCESS_TOKEN }}
