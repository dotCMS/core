# CLI Publish NPM Package Action

## Description

This GitHub Action workflow packages the dotCMS CLI as an NPM project.

## Inputs

### `ref`

Branch to build from.

- **Description**: Branch to build the project from.
- **Required**: No
- **Default**: `main`

### `github-token`

GitHub Token.

- **Description**: GitHub Token required to access GitHub resources.
- **Required**: Yes

### `npm-token`

NPM Token.

- **Description**: NPM Token required to publish the package to the NPM registry.
- **Required**: Yes

### `npm-package-name`

NPM package name.

- **Description**: NPM package name to be published.
- **Required**: No
- **Default**: `dotcli`

### `npm-package-scope`

NPM package scope.

- **Description**: NPM package scope.
- **Required**: No
- **Default**: `@dotcms`

### `node-version`

Node.js version.

- **Description**: Node.js version to be used to build the project.
- **Required**: No
- **Default**: `19`

### `workflow-to-search`

Workflow to search for artifacts.

- **Description**: Name of the workflow to search for to get the artifacts.
- **Required**: No
- **Default**: `build-test-main.yml`

### `artifact-id`

Artifact id.

- **Description**: Identifier of the artifact to be searched for and downloaded.
- **Required**: No
- **Default**: `cli-artifacts-*`

## Usage

```yaml
- name: CLI Publish NPM Package
  uses: ./.github/actions/core-cicd/deployment/deploy-cli-npm
  with:
    ref: 'main'
    github-token: ${{ secrets.GITHUB_TOKEN }}
    npm-token: ${{ secrets.NPM_TOKEN }}
    npm-package-name: 'dotcli'
    npm-package-scope: '@dotcms'
    node-version: '19'
    workflow-to-search: 'build-test-main.yml'
    artifact-id: 'cli-artifacts-*'
```