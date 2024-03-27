## Deploy CLI Artifact

This GitHub Actions workflow automates the deployment of dotCMS CLI artifacts to Artifactory. The workflow is triggered by a specific branch and requires several inputs such as branch name, Artifactory repository credentials, GitHub token, build run ID, and commit ID.

### Inputs

- **branch**: The branch to build from.
- **artifactory-repo-username**: Artifactory Repository Username.
- **artifactory-repo-password**: Artifactory Repository Password.
- **github-token**: GitHub Token.
- **build-run-id**: The run ID of the build to pull the artifact from.
- **commit-id**: The commit ID that triggered the build.

### Workflow Steps

1. **Set up JDK**: Sets up the JDK with version 11 using the `actions/setup-java` action.

2. **Get Date**: Retrieves the current date and saves it for later use.

3. **Download all build artifacts**: Downloads the dotCMS CLI artifacts from the specified path in the GitHub workspace using the `actions/download-artifact` action.

4. **Check artifact download**: Checks if the artifact download was successful.

5. **List CLI Artifacts**: Lists all the downloaded CLI artifacts.

6. **Maven settings.xml setup**: Sets up the Maven settings.xml file with Artifactory repository credentials using the `whelk-io/maven-settings-xml-action` action.

7. **Deploy CLI Artifacts**: Deploys the CLI artifacts to Artifactory using Maven.

### Conditions

- All steps after the artifact download step (`Download all build artifacts`) are conditional upon the successful download of artifacts.

### Execution

The workflow first sets up the JDK, then proceeds to download the CLI artifacts from the specified build run. After verifying the successful download, it sets up Maven settings and deploys the artifacts to Artifactory using Maven.

Note: Ensure that the necessary credentials and paths are correctly configured in the workflow inputs for successful execution.


### Usage

To use this GitHub Actions workflow, you can include the following step in your workflow YAML file:

```yaml
- name: CLI Deploy
  id: cli_deploy
  uses: ./.github/actions/deploy-artifact-cli
  with:
    branch: master
    artifactory-repo-username: ${{ secrets.EE_REPO_USERNAME }}
    artifactory-repo-password: ${{ secrets.EE_REPO_PASSWORD }}
    github-token: ${{ secrets.GITHUB_TOKEN }}
    build-run-id: ${{ needs.deploy_check.outputs.run_id }}
    commit-id: ${{ needs.deploy_check.outputs.build_sha }}
```
Replace the values of branch, artifactory-repo-username, artifactory-repo-password, github-token, build-run-id, and commit-id with appropriate variables or secrets according to your project setup. This step will trigger the deployment of dotCMS CLI artifacts to Artifactory based on the provided inputs.
