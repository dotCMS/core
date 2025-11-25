# Prepare Runner Action

This GitHub Action prepares the runner environment with basic setup steps. It provides a flexible way to configure the runner for various workflow requirements, including cleanup, Java setup, and ensuring the main branch is available.

## Features

- Optional runner cleanup for extra disk space
- Optional Java and GraalVM setup
- Ensures main branch is available locally if required
- Flexible configuration through inputs

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `cleanup-runner` | Perform runner cleanup for extra disk space | Yes | `false` |
| `require-graalvm` | Install GraalVM | Yes | `false` |
| `require-java` | Install Java | No | `true` |
| `require-main` | Ensure main branch is available locally | Yes | `false` |
| `java-version` | Specify the Java version to install | No | - |
| `graalvm-version` | Specify the GraalVM version to install | No | - |

## Usage

To use this action in your workflow, add the following step:

```yaml
- name: Prepare Runner
  uses: ./.github/actions/core-cicd/prepare-runner
  with:
    cleanup-runner: 'true'
    require-java: 'true'
    java-version: '17.0.2-tem'
    require-graalvm: 'true'
    graalvm-version: '21.0.2-graalce'
    require-main: 'true'
```


## Subactions

This action uses the following subactions:

1. `cleanup-runner`: Cleans up the runner for extra disk space.
2. `setup-java`: Sets up Java and optionally GraalVM.

Ensure that these subactions are also available in your repository.

## Example Workflow

Here's an example of how to incorporate this action into your workflow:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Prepare Runner
        uses: ./.github/actions/core-cicd/prepare-runner
        with:
          cleanup-runner: 'true'
          require-java: 'true'
          java-version: '17.0.2-tem'
          require-graalvm: 'true'
          require-main: 'true'

      - name: Build with Maven
        run: mvn clean install

      # Other steps...
```

## Notes

- The `cleanup-runner` step is useful when you need extra disk space for your workflow.
- If `require-main` is set to 'true', the action will ensure the main branch is available locally without switching the current branch.
- Java setup is performed by default unless `require-java` is set to 'false'.
- GraalVM is only installed if `require-graalvm` is set to 'true'.
