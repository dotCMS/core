# dotCMS CI/CD Process Overview

This document provides an overview of the CI/CD process for dotCMS, explaining the structure of our workflows, the use of reusable components, and how we optimize our pipeline for efficiency and parallelism.

## Table of Contents

1.  [File Structure](#file-structure)
2.  [Important info and Best Practices](#important-info-and-best-practices)
3.  [Overall Structure](#overall-structure)
4.  [Top-Level Workflows](#top-level-workflows)
5.  [Reusable Workflow Phases](#reusable-workflow-phases)
6.  [Custom Actions](#custom-actions)
7.  [Caching and Artifacts](#caching-and-artifacts)
8.  [Parallel Execution](#parallel-execution)
9.  [PR Verification Process](#pr-verification-process)
10. [Benefits of Our Approach](#benefits-of-our-approach)

## File structure

Github only allows workflows, including reusable workflows (workflow components) to be placed into the .github/workflows directory.
Any subfolders are ignored.   When there are many files such as we have this can get large and difficult to understand and maintain.
As such we will use a folder like naming convention to help organize and sort the workflow files.  
Each element "folder" will be separated by an underscore allowing for a simple hierarchy to be encoded.
eg.  cicd/comp/build-phase.yml will be represented as cicd_comp_build-phase.yml

The main initial workflows are using a numerical prefix to order these in the order a PR goes through these.
Also we are using a prefix here in the workflow name e.g. "-1 PR Check".  Although Github has now introduced the 
ability to bookmark a few workflows in the UI listing, these are not manually sorted and all other workflows are alphanumerically sorted
using the "-" followed by an index ensures these are at the top of the list and easy to find.

The actions are not restricted and we use subfolders for these.

## Important info and Best Practices

- **Secrets**: Secrets should be stored in GitHub Secrets and accessed using the `${{ secrets.SECRET_NAME }}` syntax.
- The PR workflow is run before any code is reviewed and should not use secrets. Secrets will also not be available if run on a fork
- The exact name of the first Job "Initialize / Initialize" and the last job "Finalize / Final Status" is important for the PR and merge-queue workflows as the completion state of these indicate the start and success or failure of the workflow to the Checks.  Changing these may result in the Checks to wait until time out.
- Try not to create new workflows where there already is one for the same trigger, handle all functionality for that trigger in the same place, make use of expanding on the new cicd process to take advantage of its features before creating a whole new flow. 

## Overall Structure

**NOTE: The current release process has not been migrated yet to use the reusable components and flow**

Our CI/CD process is built using GitHub Actions and is structured into three main components:

1. Top-level workflows
2. Reusable workflow phases
3. Custom actions

This structure allows for a modular, efficient, and easily maintainable CI/CD pipeline.

## Top-Level Workflows

We have several top-level workflows that handle different scenarios these can be found in .github/workflows/cicd_*.yml

1. **PR**: Triggered on pull requests to verify changes
2. **Merge Queue**: Runs when changes are ready to be merged into the main branch.
3. **Trunk**: Executes after changes are merged into the main branch.
4. **Nightly**: Runs daily to perform comprehensive tests and deployments.

These workflows orchestrate the overall process by calling reusable workflow phases and custom actions as needed.

## Reusable Workflow Phases

We use reusable workflow phases within our top level workflows to modularize our CI/CD process and emphasize a set of phases
any commit can go through:

1. **Initialize**: Sets up the environment and determines what needs to be run.
2. **Build**: Compiles the code and generates necessary artifacts.
3. **Test**: Runs various test suites (unit tests, integration tests, etc.).
4. **Semgrep**: Performs code quality analysis.
5. **Deployment**: Handles deployment to various environments.
6. **Release**: (TODO) Publishes releases to the appropriate channels.
6. **Finalize**: Aggregates results and performs cleanup tasks.
7. **Reporting**: Generates comprehensive reports of the CI/CD process run and sends notifications

These phases can be easily included and configured in different top-level workflows, reducing code duplication and ensuring consistency.

## Custom Actions

We have several custom actions that perform specific common tasks:

1. **Prepare Runner**: Sets up the runner environment.
2. **Setup Java**: Installs and configures Java and optionally GraalVM.
3. **Cleanup Runner**: Frees up disk space on the runner.
4. **Maven Job**: Runs Maven builds with extensive configuration options handles the common setup can caching needed

These actions encapsulate complex logic and can be reused across different workflows and phases.

## Caching and Artifacts

We extensively use caching and artifacts to optimize our CI/CD process:

- **Caching**: We cache dependencies (Maven, Node.js, Yarn) and build outputs to speed up subsequent runs.
- **Artifacts**: We generate and share artifacts between jobs, allowing for parallel execution and result aggregation.

Key points:
- Maven repository is cached to speed up builds.
- Build outputs are saved as artifacts and can be used by subsequent jobs.
- Test results are saved as artifacts for later analysis and reporting.

## Parallel Execution

Our structure allows for efficient parallel execution:

1. The Initialize phase determines what needs to be run.
2. Long-running tasks like Integration and Postman tests can be executed in parallel.
3. Results and outputs from parallel jobs are aggregated in the Finalize phase.

This approach significantly reduces the overall execution time of our CI/CD pipeline.

## PR Verification Process

A typical PR goes through the following steps:

1. **Initialize**: Determine what has changed and what needs to be verified.
2. **Build**: Compile the code and generate necessary artifacts.
3. **Parallel Testing**: Run various test suites concurrently (unit tests, integration tests, Postman tests).
4. **Semgrep Analysis**: Perform code quality checks.
5. **Finalize**: Aggregate results from all previous steps.
6. **Reporting**: Generate a comprehensive report of the PR check process.

## Specific configurations for each top level workflow getting code to trunk (main) branch

| Workflow            | Trigger                                                                            | Notes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `1-pr.yml`          | Push of a PR to github                                                             | * Should not use secrets as it is run on code that has not been reviewed <br/> * post-workflow-report.yml is run as a separate triggered workflow so it can have access to the secrets it needs <br/> * For speed it does not run tests that should not be impacted by changes in the PR. Filters defined in .github/filters.yaml                                                                                                                                                                                                  |
| `2.merge-queue.yml` | PR passed its checks and was added to the merge queue                              | * We force run all tests to catch flakey issues or incorrect filters. <br/> * Merge group checks include all the code of PRs ahead of it in the queue.  If successful after merge the main branch will have the same commit id that will end up as the HEAD of main. <br/> failures in the merge queue should be monitored closely for flakey tests or misconfiguration failures here can slow the process for other developers trying to merge                                                                                |
| `3-trunk.yml`       | Runs on code that was pushed to trunk (main)                                     | * As we already built and tested the same commit in the merge queue we can take advantage of that and use the build artifacts from that workflow to skip these steps <br/> We currently build native cli artifacts in this phase due to the work required we do not want to run on every PR.  <br/> We run snapshot deployments here to github (trunk) deployments, snapshot artifactory etc.                                                                                                                                      |
| `4-nightly.yml'     | Runs on a nightly schedule and will run on the latest commit on main at the time | * Another chance to capture flakey build issues <br/> We can add longer running tests here that would be impractical to run on every PR merged <br/> Provides a more stable image to compare behavior from previous days <br/> This currently runs using the default 1.0.0-SNAPSHOT image but with release changes this end up with a dated version on a nightly branch.  The workflow triggered from the nightly cron will version and promote the code and a separate nightly workflow will build, test, deploy from that branch |

## Further verification and promotion phases up to Release
**In Progress**

The aim is to have the main branch be in a releasable state.  Our preceding steps and validations to get a PR into the main branch should be the primary gates to prevent an unreleasable bad commit.

It is also key to the smooth development process also that issues are not introduced into the main branch that could cause failures when developers merge it into their own branches.

We still need go go through some further validations though before we can approve a specific commit on the main branch as acceptable for release. Some of these tests both automatic and manual can take some time
so we do not want to block the development process while these are being run.  We will have a separate branch (release) or branches (test?,rc?,release)that will be used to promote the code from the main branch up to a release branch. Each step will provide a higher level of confidence.

We will not make manual changes to these branches, the only changes from the core commit on main that will be made are to set the version for the build.  This should be as minimal as possible and currently for maven can be done by adding just one file .mvn/maven.properties.
The more changes to the code are made the more opportunity that there is a change that impacts behavior that was not already tested in the previous steps.

We can make the promotion process a manual action and can also make use of Github deploymnents and environments to specify required reviewers before promotion is done

If an issue is found, any fixes should be propagated through the development process in a new PR.  The new code can replace the original intended version.  This process allows for a stable commit that is being verified in each phase.
We should pull in changes from as quick as possible


```text

Before Nightly run                       After Nightly Promote Step
 Test and deploy                           Test and deploy new versioned 
 versioned HEAD of nightly PR1A            HEAD of nightly PR4B

             
nightly:    PR1A                             PR1A--PR2B--PR3B--PR4B
             |                                |     |     |     |
main:   --PR1---PR2---PR3---PR4     run: --PR1---PR2---PR3---PR4
```
The commits into nightly are not the exact same commit sha as the parent on main
The change between the two is determanistic and repreducable.  We only add a ./mvn/maven.config containing the release version to embed and build with by default
We also provide the original SHA to link back to the source commit on main.  

The exact same process can be used with a manual step to select when to sync up main to a test or release candidate branch
We do not pick and choose individual PRS to sync up,  by default we would pull all the commits up to and including the HEAD commit on main There may 
be a reason to select a previous commit but must always be a commit between what is already merged and the HEAD and will contain all the commits and changes inbetween. 
The only difference will be the change in release number assigned to the commits which will help us with change logs.

**Example flow of PR through to Release**

```text


x indicates a promotion with version change


release         PR1a--PR2b--PR3b--PR4b--PR5b--PR6b
                 |x    |     |    |     |     |x
rc              PR1A--PR2B--PR3B--PR4B--PR5C--PR6C--PR7D
                 |x    |     |     |x    |     |x    |x
main    run: --PR1---PR2---PR3---PR4---PR5---PR6---PR7---PR8

1. PR1 promoted to Release Candidate and RC testing occurs on PR1A  rc-A
2. PR1A tested and approved for release with new release version.   Release A
    In the meantime PR2 and PR3 have been added to main and have no impact on RC branch 
3. PR4 promoted to RC as version B and PR4B tested while PR5 is added to main. RC-B PR2B,PR3B,PR4B included
4. PR4B is not approved for release, PR6 adds a fix is promoted to RC as version C
5. PR6C is approved for release and promoted to release.
```
Notes:

* RC can set build to a version that indicates it is a release candidate e.g. x.x.x-rc requring the release 
version to be set on promotion to release, or it could be set with the final release number, in this case it must be
deployed to a staging deployment area and then the release promotion just moves the artifacts to the final destination.
This prevents the need for a new build of artifacts on release.
* A promotion could always require a new version, or it could retain the same version e.g. to maintain the intended next version number we want to release. In this case we should still maintain an internal build number to distinguish when the PR related to that version has been updated


## Benefits of Our Approach

1. **Modularity**: Reusable workflows and custom actions make our pipeline easy to maintain and extend.
2. **Consistency**: Using reusable components ensures consistent execution across different scenarios.
3. **Efficiency**: Caching and parallel execution optimize the pipeline's performance.
4. **Flexibility**: Top-level workflows can easily be configured to include or exclude specific phases as needed.
5. **Scalability**: New test suites or deployment targets can be easily added to the existing structure.

## Conclusion

Our CI/CD process is designed to be efficient, flexible, and easy to maintain. By leveraging GitHub Actions' features like reusable workflows, custom actions, caching, and artifacts, we've created a robust pipeline that can handle the complex needs of the dotCMS project while remaining adaptable to future requirements.