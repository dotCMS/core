# Maven Job Action

This GitHub Action sets up and runs a Maven job with extensive configuration options. It's designed to handle various aspects of Maven-based workflows, including runner preparation, caching, artifact management, and Maven execution, with special support for dotCMS, Docker, and native image builds.

## Features

- Flexible runner preparation including Java and GraalVM setup
- Caching for Maven repository, Node.js, Yarn, and SonarQube
- Artifact restoration and persistence
- Configurable Maven execution with support for native builds
- Docker image handling and artifact generation
- Test result and build report generation
- Support for dotCMS license setup

## Inputs

| Input | Description | Required | Default |
|-------|-------------|----------|---------|
| `needs-docker-image` | Runner needs the built dotCMS docker image | Yes | `false` |
| `cleanup-runner` | Runner requires extra disk space | Yes | `false` |
| `generate-artifacts` | Generate artifacts for the job | Yes | `false` |
| `generate-docker` | Generate docker artifact | Yes | `false` |
| `needs-history` | Runner needs the full git history | Yes | `false` |
| `requires-node` | Job requires Node.js | Yes | `true` |
| `cache-sonar` | Cache the SonarQube files | No | `false` |
| `require-main` | Require the main tag to run this action | Yes | `false` |
| `dotcms-license` | The license key for dotCMS | No | `''` |
| `artifacts-from` | Download artifacts from a previous job | No | `''` |
| `github-token` | GitHub token for authentication | Yes | - |
| `restore-classes` | Restore build classes | No | `false` |
| `stage-name` | Stage name for the build | Yes | - |
| `maven-args` | Arguments for Maven build | Yes | - |
| `generates-test-results` | Generate test results artifacts | No | `false` |
| `native` | Build native image | Yes | `false` |
| `version` | The version of the build | No | `1.0.0-SNAPSHOT` |
| `java-version` | The version of Java to install | No | - |
| `graalvm-version` | Override the sdkman version of GraalVM to install | No | - |
| `require-graalvm` | Require GraalVM to be installed | Yes | `false` |

## Usage

To use this action in your workflow, add the following step:

```yaml
- name: Run Maven Job
  uses: ./.github/actions/core-cicd/maven-job
  with:
    stage-name: 'Build and Test'
    maven-args: 'clean install'
    github-token: ${{ secrets.GITHUB_TOKEN }}
    generate-artifacts: 'true'
    generates-test-results: 'true'
    # Add other inputs as needed
```

## Workflow Steps

1. Prepare the runner environment (including Java/GraalVM setup)
2. Set up dotCMS license (if provided)
3. Restore caches (Maven, Node.js, Yarn, SonarQube)
4. Restore artifacts from previous jobs (if specified)
5. Run Maven build (with support for native builds)
6. Handle Docker image generation (if required)
7. Persist artifacts and update caches
8. Generate build reports and test results

## Example Workflow

Here's an example of how to incorporate this action into your workflow:

```yaml
jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Run Maven Job
        uses: ./.github/actions/core-cicd/maven-job
        with:
          stage-name: 'Build and Test'
          maven-args: 'clean install'
          github-token: ${{ secrets.GITHUB_TOKEN }}
          generate-artifacts: 'true'
          generates-test-results: 'true'
          requires-node: 'true'
          cache-sonar: 'true'
          generate-docker: 'true'
          dotcms-license: ${{ secrets.DOTCMS_LICENSE }}

      # Other steps...
```

## Notes

- This action is highly customizable and can be adapted for various Maven-based workflow needs, especially for projects involving dotCMS and Docker.
- It handles caching and artifact management to optimize workflow performance.
- The action supports both Java and GraalVM setups, making it versatile for different project requirements including native image builds.
- Docker image handling is included, which is useful for projects that involve containerization.
- The action can set up a dotCMS license, which is useful for dotCMS-specific builds and tests.
