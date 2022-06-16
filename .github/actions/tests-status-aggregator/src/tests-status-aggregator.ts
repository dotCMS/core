import * as core from '@actions/core'

interface TestsStatus {
  status?: string
  reportUrl?: string
  logUrl?: string
}

interface TypeLabels {
  unit: string
  integration_postgres: string
  integration_mssql: string
  postman: string
}

export interface OverallTestsStatus {
  status: string
  color: string
  message: string
}

const inputTypes = ['unit', 'integration_postgres', 'integration_mssql', 'postman']
const typeLabels: TypeLabels = {
  unit: 'Unit Tests',
  integration_postgres: 'Integration Postgres Tests',
  integration_mssql: 'Integration MSSQL Tests',
  postman: 'Postman Tests'
}
const PASSED = 'PASSED'
const FAILED = 'FAILED'

export const aggregate = (): OverallTestsStatus => {
  let status = PASSED
  let color = '#5E7D00'
  const messages = []
  for (const type of inputTypes) {
    const testsStatus = resolveInputs(type)
    if (status === PASSED && (!testsStatus.status || testsStatus.status === FAILED)) {
      status = FAILED
      color = '#ff2400'
    }

    let emoji = testsStatus.status === PASSED ? ':sunny: ' : ':thunder_cloud_and_rain: '
    if (!testsStatus.reportUrl) {
      emoji = ''
    }
    const reportUrl = testsStatus.reportUrl || 'not available'
    const logUrl = testsStatus.logUrl || 'not available'
    messages.push(`${emoji}${typeLabels[type as keyof TypeLabels]}:`)
    messages.push(`Report: ${reportUrl}`)
    messages.push(`Log: ${logUrl}`)
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
    reportUrl:
      core.getInput(`testmo_${type}_tests_results_report_url`) || core.getInput(`${type}_tests_results_report_url`),
    logUrl: core.getInput(`${type}_tests_results_log_url`)
  }
}
