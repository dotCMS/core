import * as core from '@actions/core'
import fetch, {Response} from 'node-fetch'

interface GithubStatus {
  state: string
  description: string
  target_url: string
  context: string
}

interface LinksSupport {
  _links: {
    statuses: {
      href: string
    }
  }
}

/**
 * Sends the tests results statsus to Github using its API.
 *
 * @param testType test type
 * @param dbType database type
 * @param testResultsStatus test results status (PASSED or FAILED)
 */
export const send = async (testType: string, dbType: string, testResultsStatus: string) => {
  const pullRequest = core.getInput('pull_request')
  if (!pullRequest) {
    core.warning(`This was not triggered from a pull request, so skipping sending status `)
    return
  }

  const githubUser = core.getInput('github_user')
  const cicdGithubToken = core.getInput('cicd_github_token')
  const creds = `${githubUser}:${cicdGithubToken}`
  const pullRequestUrl = `https://api.github.com/repos/dotCMS/core/pulls/${pullRequest}`

  const prResponse = await getPullRequest(pullRequestUrl, creds)
  if (!prResponse.ok) {
    core.warning(`Could not get Github pull request ${pullRequest}`)
    return
  }

  const pr = (await prResponse.json()) as LinksSupport
  const testsReportUrl = core.getInput('tests_report_url')
  const status = createStatus(testType, dbType, testResultsStatus, testsReportUrl)
  const statusResponse = await postStatus(pr._links.statuses.href, creds, status)
  if (!statusResponse.ok) {
    core.warning(`Could not send Github status for ${testType} tests`)
    return
  }
}

/**
 * Resolves what label to use based on the test type.
 *
 * @param testType test type
 * @param dbType database type
 * @returns status label
 */
const resolveStastusLabel = (testType: string, dbType: string): string => {
  switch (testType) {
    case 'unit':
      return '[Unit tests results]'
    case 'integration':
      return `[Integration tests results] - [${dbType}]`
    case 'postman':
      return `[Curl tests results] - [${dbType}]`
    default:
      return ''
  }
}

/**
 * Creates a status object based on the provided params.
 *
 * @param testType test type
 * @param dbType database type
 * @param testResultsStatus test results status
 * @param testsReportUrl report url where tests results are located
 * @returns {@link GithubStatus} object to be used when reporting
 */
const createStatus = (
  testType: string,
  dbType: string,
  testResultsStatus: string,
  testsReportUrl: string
): GithubStatus => {
  let statusLabel
  let description
  if (testResultsStatus === 'PASSED') {
    statusLabel = 'success'
    description = 'Tests executed SUCCESSFULLY'
  } else {
    statusLabel = 'failure'
    description = 'Tests FAILED'
  }

  return {
    state: statusLabel,
    description,
    target_url: testsReportUrl,
    context: `Github Actions - ${resolveStastusLabel(testType, dbType)}`
  }
}

/**
 * Creates headers object to be used with Github API.
 *
 * @param creds base 64 encoded username:password credentials
 * @returns headers object
 */
const headers = (creds: string): HeadersInit => {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Basic ${Buffer.from(creds).toString('base64')}`
  }
}

/**
 * Uses fetch function to send GET http request to Github API to get pull request data.
 *
 * @param pullRequestUrl pull request API url
 * @param creds base 64 encoded username:password credentials
 * @returns {@link Response} object
 */
const getPullRequest = async (pullRequestUrl: string, creds: string): Promise<Response> => {
  core.info(`Sending GET to ${pullRequestUrl}`)
  const response: Response = await fetch(pullRequestUrl, {
    method: 'GET',
    headers: headers(creds)
  })
  core.info(`Got response:\n${JSON.stringify(response.body, null, 2)}`)
  return response
}

/**
 * Uses fetch function to send POST http request to Github API to report status
 *
 * @param statusUrl status report API url
 * @param creds base 64 encoded username:password credentials
 * @param status status object
 * @returns {@link Response} object
 */
const postStatus = async (statusUrl: string, creds: string, status: GithubStatus): Promise<Response> => {
  core.info(`Sending POST to ${statusUrl} the payload:\n${JSON.stringify(status, null, 2)}`)
  const response: Response = await fetch(statusUrl, {
    method: 'POST',
    headers: headers(creds),
    body: JSON.stringify(status)
  })
  core.info(`Got response:\n${JSON.stringify(response, null, 2)}`)
  return response
}
