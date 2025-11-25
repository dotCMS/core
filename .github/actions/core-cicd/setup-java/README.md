# Setup Java Action

This GitHub Action sets up Java using SDKMan, with optional GraalVM installation. It's designed to provide a flexible and efficient way to set up Java environments in GitHub Actions workflows.

## Features

- Installs SDKMan if not already present
- Installs specified Java version or uses version from .sdkmanrc file
- Optionally installs GraalVM
- Caches SDKMan and Java installations for faster subsequent runs
- Supports different architectures
- Sets up JAVA_HOME and PATH environment variables

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `java-version` | Override the Java version to install | No | - |
| `require-graalvm` | Require GraalVM to be installed | Yes | `false` |
| `graalvm-version` | Override the SDKMan version of GraalVM to install | No | - |

## Usage

To use this action in your workflow, add the following step:

```yaml
- name: Setup Java
  uses: ./.github/actions/core-cicd/setup-java
  with:
    java-version: '17.0.2-tem'  # Optional: Specify a Java version
    require-graalvm: 'true'     # Optional: Install GraalVM
    graalvm-version: '21.0.2-graalce'  # Optional: Specify GraalVM version
```

## .sdkmanrc Support

If you have an `.sdkmanrc` file in your repository root, the action will use the Java version specified there unless overridden by the `java-version` input.

Example `.sdkmanrc`:
```
java=17.0.2-tem
```

## Caching

This action implements caching for SDKMan and Java installations to speed up subsequent runs. The cache is keyed based on the runner OS, architecture, and installed Java version.

## Example Workflow

Here's an example of how to incorporate this action into your workflow:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Setup Java
        uses: ./.github/actions/core-cicd/setup-java
        with:
          java-version: '17.0.2-tem'
          require-graalvm: 'true'

      - name: Build with Maven
        run: mvn clean install

      # Other steps...
```

## Notes

- If both `java-version` input and `.sdkmanrc` file are present, the input takes precedence.
- When `require-graalvm` is set to 'true', the action will install GraalVM if it's not already present.
- The action sets the `JAVA_HOME` and `GRAALVM_HOME` (if applicable) environment variables for use in subsequent steps.
