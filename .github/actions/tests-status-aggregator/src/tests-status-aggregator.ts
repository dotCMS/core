import * as core from '@actions/core'

interface TestsStatus {
  status?: string
  skipReport?: boolean
  reportUrl?: string
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
const SUCCESS = 'SUCCESS'
const FAILED = 'FAILED'

export const aggregate = (): OverallTestsStatus => {
  let status = 'SUCCESS'
  let color = '#5E7D00'
  const messages = []
  for (const type of inputTypes) {
    const testsStatus = resolveInputs(type)
    if (status === SUCCESS && (!testsStatus.status || testsStatus.status === 'FAILED')) {
      status = FAILED
      color = '#ff2400'
    }

    const reportUrl = testsStatus.reportUrl || 'Could not resolve report URL'
    messages.push(`${typeLabels[type as keyof TypeLabels]}: ${reportUrl}`)
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
    skipReport: core.getBooleanInput(`${type}_tests_results_skip_report`),
    reportUrl: core.getInput(`${type}_tests_results_report_url`)
  }
}
