import * as core from '@actions/core'
import * as github from './github-status'

/**
 * Main entry point for this action.
 */
const run = () => {
  const testType = core.getInput('test_type')
  const testResultsStatus = core.getInput('test_results_status')

  core.info(`Submitting ${testResultsStatus} status to Github for ${testType} tests`)
  github.send(testType, testResultsStatus)
}

// Run main function
run()
