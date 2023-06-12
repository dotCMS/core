import * as core from '@actions/core'

interface TestsStatus {
  status?: string
  reportUrl?: string
  logUrl?: string
  testmoReportUrl?: string
}

interface TypeLabels {
  unit: string
  integration_postgres: string
  postman: string
}

export interface OverallTestsStatus {
  status: string
  color: string
  message: string
}

const inputTypes = ['unit', 'integration_postgres', 'postman']
const typeLabels: TypeLabels = {
  unit: 'Unit Tests',
  integration_postgres: 'Integration Tests [Postgres]',
  postman: 'Postman Tests'
}
const PASSED = 'PASSED'
const FAILED = 'FAILED'

export const aggregate = (): OverallTestsStatus => {
  let status = PASSED
  let color = '#5E7D00'

  const messages = []

  const slackUser = resolveSlackUser()
  if (slackUser) {
    messages.push(`Hey <@${slackUser}>, your tests have run:`)
  }

  for (const type of inputTypes) {
    const testsStatus = resolveInputs(type)
    if (status === PASSED && (!testsStatus.status || testsStatus.status === FAILED)) {
      status = FAILED
      color = '#ff2400'
    }

    const emoji = testsStatus.status === PASSED ? ':sunny: ' : ':thunder_cloud_and_rain: '
    const reportUrls = [
      testsStatus.reportUrl ? `<${testsStatus.reportUrl}|Github>` : '',
      testsStatus.testmoReportUrl ? `<${testsStatus.testmoReportUrl}|Testmo>` : ''
    ].filter(url => !!url)
    const logUrl = testsStatus.logUrl || ''
    const message = `${emoji}*${typeLabels[type as keyof TypeLabels]}*`
    const reportChunk = reportUrls.length > 0 ? `${reportUrls.join(' | ')}` : 'Report unavailable'
    const logChunk = logUrl ? `<${logUrl}|Log>` : 'Log unavailable'

    messages.push(`${message}: ${reportChunk} | ${logChunk}`)
  }

  return {
    status: `Tests status: ${status}`,
    color,
    message: messages.join('\n')
  }
}

const resolveInputs = (type: string): TestsStatus => {
  return {
    status: core.getInput(`${type}_tests_results_status`),
    reportUrl: core.getInput(`${type}_tests_results_report_url`),
    logUrl: core.getInput(`${type}_tests_results_log_url`),
    testmoReportUrl: core.getInput(`testmo_${type}_tests_results_report_url`)
  }
}

const resolveSlackUser = (): string => {
  const githubUser = core.getInput('github_user') || ''
  core.info(`Detected user ${githubUser}`)
  const confStr = core.getInput('github_slack_conf') || '{}'
  const githubSlackConf = JSON.parse(confStr)
  core.info(`Provided conf:\n${confStr}`)
  const resolved = githubSlackConf[githubUser] || ''
  core.info(`Resolved slack user id ${resolved}`)
  return resolved
}
