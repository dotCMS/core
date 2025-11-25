# Cleanup Runner Action

This GitHub Action is designed to clean up resources on GitHub Actions runners, helping to reclaim disk space and optimize performance for subsequent steps or jobs.

## Features

- Displays initial disk usage
- Cleans package manager caches (Homebrew on macOS, apt on Linux)
- Prunes Docker resources (images, volumes, containers, networks)
- Removes unnecessary large directories and files
- Shows final disk usage after cleanup

## Supported Runners

- macOS
- Linux

## Usage

To use this action in your workflow, add the following step:

```yaml
- name: Cleanup Runner
  uses: ./.github/actions/core-cicd/cleanup-runner
```


## When to Use

This action is particularly useful in the following scenarios:

1. Before steps that require a significant amount of disk space
2. After steps that generate a lot of artifacts or temporary files
3. In workflows with multiple jobs that might benefit from a clean environment

## Example

Here's an example of how to incorporate this action into your workflow:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      # Your build steps here...

      - name: Cleanup Runner
        uses: ./.github/actions/core-cicd/cleanup-runner

      # Subsequent steps that benefit from cleaned-up resources...

```

## Notes

- This action may remove some pre-installed tools. If your subsequent steps require specific tools, make sure to reinstall them after running this action.
- The cleanup process may take a few minutes to complete, depending on the amount of resources to be cleaned.
