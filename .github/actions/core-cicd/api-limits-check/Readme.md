# Check GitHub API Rate Limit Action

This GitHub Action allows you to check the current API rate limits for your GitHub account by querying the `/rate_limit` endpoint of the GitHub API. The action outputs the full JSON response, including rate limits for core, search, GraphQL, and other GitHub API resources.

## Inputs

| Input  | Description | Required | Default |
|--------|-------------|----------|---------|
| `token` | The GitHub token to authenticate the API request. | true | `${{ github.token }}` |

## Outputs

The action outputs the full JSON response from the `/rate_limit` endpoint directly to the workflow log.

## Usage

Here’s an example of how to use this action in a GitHub workflow:

```yaml
name: Check GitHub API Rate Limits

on:
  workflow_dispatch:

jobs:
  check-rate-limits:
    runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}
    steps:
      - name: Check API Rate Limit
        uses: your-repo/check-rate-limit-action@v1
        with:
          token: ${{ secrets.GITHUB_TOKEN }}