# SDK Publish NPM Packages

This GitHub Action automates the process of publishing dotCMS SDK libraries to the NPM registry. It checks out the specified branch, sets up Node.js, calculates the next version, updates the versions and dependencies, and then publishes the packages to NPM.

## Inputs

| Input Name       | Description                   | Required | Default |
|------------------|-------------------------------|----------|---------|
| `ref`            | Branch to build from          | No       | `master`|
| `npm-token`      | NPM token                     | Yes      |         |
| `npm-package-tag`| Package tag                   | No       | `alpha` |
| `node-version`   | Node.js version               | No       | `19`    |
| `github-token`   | GitHub Token                  | Yes      |         |

## Outputs

| Output Name            | Description                           |
|------------------------|---------------------------------------|
| `npm-package-version`  | SDK libs - NPM package version        |

## Example Usage

Below is an example of how to use this GitHub Action in your workflow file:

```yaml
name: Publish SDK Packages

on:
  push:
    branches:
      - main

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - name: SDK Publish NPM Packages
        uses: your-repo/sdk-publish-npm-packages@v1
        with:
          ref: 'main'
          npm-token: ${{ secrets.NPM_TOKEN }}
          npm-package-tag: 'beta'
          node-version: '18'
          github-token: ${{ secrets.GITHUB_TOKEN }}
```

## Workflow Steps

1. **Checkout**: Checks out the specified branch using the provided GitHub token.
2. **Set up Node.js**: Sets up the specified Node.js version.
3. **Get Current Version from NPM**: Retrieves the current version of the SDK from NPM.
4. **Calculate Next Version**: Calculates the next version based on the current version.
5. **Print Versions**: Prints the current and next versions.
6. **Update Versions and Dependencies**: Updates the versions and dependencies in the package.json files.
7. **Print SDK Packages**: Prints the updated package.json files for the SDK packages.
8. **Publish SDK to NPM**: Publishes the SDK packages to the NPM registry with the specified tag.