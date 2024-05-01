# Deploy Artifacts to Artifactory

This GitHub Action streamlines the process of deploying dotCMS artifacts to Artifactory. It's particularly useful for automating the deployment workflow in projects utilizing dotCMS.

## Inputs

### `artifactory-repo-username`

**Description**: Username required for accessing the Artifactory repository.  
**Required**: true

### `artifactory-repo-password`

**Description**: Password associated with the Artifactory repository username.  
**Required**: true

### `github-token`

**Description**: Token required for GitHub API access, used here for retrieving artifacts.  
**Required**: true

## Example Usage

```yaml
name: Deploy Artifact

on:
  push:
    branches:
      - main

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Deploy Artifact to Artifactory
        uses: ./.github/actions/deploy-artifact-artifactory
        with:
          artifactory-repo-username: ${{ secrets.ARTIFACTORY_REPO_USERNAME }}
          artifactory-repo-password: ${{ secrets.ARTIFACTORY_REPO_PASSWORD }}
          github-token: ${{ secrets.GITHUB_TOKEN }}
```
