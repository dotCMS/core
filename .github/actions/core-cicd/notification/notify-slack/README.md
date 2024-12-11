# Slack Notification GitHub Action

This GitHub Action sends a notification to a specified Slack channel using a provided payload in Markdown format. It's particularly useful for sending custom messages from your CI/CD pipelines directly to your Slack channels.

## Inputs

| Name             | Description                                     | Required | Default |
| ---------------- | ----------------------------------------------- | -------- | ------- |
| `channel-id`     | The ID of the Slack channel to send the message to. | `true`   |         |
| `payload`        | The message payload in Markdown format.         | `true`   |         |
| `slack-bot-token`| The Slack Bot Token used for authentication.    | `true`   |         |

## Example Usage

Here is an example of how to use this action in your GitHub workflow file:

```yaml
name: Notify Slack on Success

on:
  push:
    branches:
      - main

jobs:
  notify:
    runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}
    steps:
      - name: Send Slack Notification
        uses: ./
        with:
          channel-id: 'C12345678'
          payload: |
            "Build succeeded! :tada:
            *Branch:* ${{ github.ref_name }}
            *Commit:* ${{ github.sha }}
            *Author:* ${{ github.actor }}"
          slack-bot-token: ${{ secrets.SLACK_BOT_TOKEN }}
```

## Inputs Description
**channel-id**: The Slack channel ID where the notification will be posted. Make sure to use the correct ID (e.g., C12345678 for public channels or G12345678 for private channels).
**payload**: The content of the message, written in Markdown format. You can use standard Slack markdown for formatting.
**slack-bot-token**: Your Slack bot token, which should be stored as a secret in your GitHub repository for security purposes.

### Note
> Ensure that your Slack Bot Token has the necessary permissions to post messages to the specified channel. If you encounter any issues with permissions, review your Slack app's OAuth scopes.