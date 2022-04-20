import * as core from '@actions/core'
import * as github from './github-status'

/**
 * Main entry point for this action.
 */
const run = () => {
  const testType = core.getInput('test_type')
  const dbType = core.getInput('db_type')
  const testResultsStatus = core.getInput('test_results_status')

  const dbLabel = !!dbType ? ` with ${dbType} database` : ''
  core.info(`Submitting ${testResultsStatus} status to Github for ${testType} tests${dbLabel}`)
  github.send(testType, dbType, testResultsStatus)
}

// Run main function
run()
