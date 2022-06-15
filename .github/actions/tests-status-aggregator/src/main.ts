import * as core from '@actions/core'
import * as aggregator from './tests-status-aggregator'

/**
 * Main entry point for this action.
 */
const run = () => {
  const testStatus = aggregator.aggregate()
  core.setOutput('status', testStatus.status)
  core.setOutput('color', testStatus.color)
  core.setOutput('message', testStatus.message)
}

// Run main function
run()
