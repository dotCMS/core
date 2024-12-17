# Deploy Artifact Javadoc Composite Action

This GitHub composite action deploys Javadoc artifacts to the GitHub Packages registry and an AWS S3 bucket. It uses Maven to generate the Javadocs and AWS CLI to upload the generated documentation to a specified S3 URI.

## Inputs

- **ref**:  
  *Description*: Branch to build from.  
  *Required*: No  
  *Default*: `main`

- **github-token**:  
  *Description*: GitHub Token for authentication.  
  *Required*: Yes  

- **release-version**:  
  *Description*: The version of the release.  
  *Required*: Yes  

- **artifact-run-id**:  
  *Description*: The run ID of the core artifacts to restore classes from.  
  *Required*: No  

- **aws-access-key-id**:  
  *Description*: AWS Access Key ID for authentication.  
  *Required*: Yes  

- **aws-secret-access-key**:  
  *Description*: AWS Secret Access Key for authentication.  
  *Required*: Yes  

- **aws-region**:  
  *Description*: AWS region for deployment.  
  *Required*: Yes  
  *Default*: `us-east-1`

## Steps

1. **Checkout**: Uses the `actions/checkout@v4` action to check out the specified branch.
2. **Maven Clean Build**: Runs a Maven clean install to build the project (skipping tests), only if `artifact-run-id` is not provided.
3. **Deploy Javadoc**: Runs Maven to generate Javadocs and restores classes from the specified artifact run ID if provided.
4. **Configure AWS Credentials**: Configures AWS credentials using the `aws-actions/configure-aws-credentials@v1` action.
5. **Generate/Push Javadoc**: Uploads the generated Javadoc to an S3 bucket using the AWS CLI.

## Example Usage

```yaml
name: Deploy Javadoc Workflow
on:
  push:
    branches:
      - main

jobs:
  deploy-javadoc:
    runs-on: ubuntu-${{ vars.UBUNTU_RUNNER_VERSION || '24.04' }}
    steps:
      - name: Deploy Artifact Javadoc
        uses: ./.github/actions/deploy-artifact-javadoc
        with:
          ref: 'main'
          github-token: ${{ secrets.GITHUB_TOKEN }}
          release-version: '1.0.0'
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: 'us-east-1'
```

# Notes

- Ensure that the github-token, aws-access-key-id, and aws-secret-access-key inputs are provided securely using GitHub secrets.
- The artifact-run-id is optional and can be used to restore classes from previous artifact runs if necessary.
