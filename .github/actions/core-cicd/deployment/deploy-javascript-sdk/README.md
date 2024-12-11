# GitHub Action: SDK Publish NPM Packages

This GitHub Action is designed to automate the process of publishing dotCMS SDK libraries to the NPM registry. It performs the following tasks:

1. **Checks out the specified branch of the repository**.
2. **Sets up the required Node.js environment**.
3. **Retrieves the next version of the SDK from the package.json file**.
4. **Validates the version number against the existing version in the NPM registry**.
5. **Publishes the SDK libraries to the NPM registry if validation passes**.

## Inputs

| Name             | Description                       | Required | Default |
|------------------|-----------------------------------|----------|---------|
| `ref`            | Branch to build from              | No       | `main`|
| `npm-token`      | NPM token                         | Yes      |         |
| `npm-package-tag`| Package tag                       | No       | `alpha` |
| `node-version`   | Node.js version                   | No       | `19`    |
| `github-token`   | GitHub Token                      | Yes      |         |

## Outputs

| Name                 | Description                           |
|----------------------|---------------------------------------|
| `npm-package-version`| SDK libs - NPM package version        |

## Steps Overview

1. **Checkout**: Checks out the specified branch of the repository.
2. **Set up Node.js**: Sets up the Node.js environment based on the provided version.
3. **Get Next Version**: Retrieves the next version from the `package.json` file of the SDK.
4. **Validate Version**: Validates whether the next version is correct and whether it should be published.
5. **Publish SDK into NPM Registry**: Publishes the SDK libraries to NPM if the version is validated.

## Detailed Steps

1. **Checkout**  
   The action uses `actions/checkout@v4` to check out the specified branch, allowing the workflow to access the repository's contents.

2. **Set Up Node.js**  
   `actions/setup-node@v4` sets up the Node.js environment, crucial for running scripts and managing dependencies.

3. **Get Next Version**  
   This step retrieves the next version of the SDK by reading the `package.json` file from the specified directory.

4. **Validate Version**  
   The version retrieved in the previous step is compared to the current version in the NPM registry. The workflow checks if the version is already published or if it follows the expected versioning scheme.

5. **Publish SDK into NPM Registry**  
   If the validation passes, the SDK libraries are published to the NPM registry. The libraries are iterated over, and each is published using the provided NPM token and tag.

### Notes

- The versions of the SDK libraries will be incremented in the NPM registry, but they will not be updated in the codebase itself through this process. This simplification avoids automatic pull requests and all the considerations necessary due to protections on the main branch, as well as the lengthy execution process that would be required to ultimately publish the libraries.
- This is a temporary solution until we determine the most appropriate pattern to handle the lifecycle of each module that needs to be released individually (e.g., dotCLI and the SDKs).
- Additionally, the example projects should point to the 'latest' tag to ensure that version updates do not impact their functionality due to version inconsistency.
- Ensure that the NPM token provided has the correct permissions to publish packages.
- The action assumes that the `package.json` files are located under `core-web/libs/sdk/client`.

## Usage Example

Below is an example of how to use this GitHub Action in your workflow file:

```yaml
name: 'Publish SDK Libraries'
on:
  push:
    branches:
      - main
  workflow_dispatch:

jobs:
  publish-sdk:
    runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}
    steps:
      - name: Publish to NPM
        uses: ./.github/actions/core-cicd/deployment/deploy-javascript-sdk
        with:
          ref: 'main'
          npm-token: ${{ secrets.NPM_TOKEN }}
          npm-package-tag: 'latest'
          node-version: '18'
          github-token: ${{ secrets.GITHUB_TOKEN }}
```