import * as core from '@actions/core'
import * as wrapper from './testmo-wrapper'

/**
 * Main entry point for this action.
 */
const run = () => {
  const operation = core.getInput('operation')
  core.info(`Attempting to run Testmo CLI with operation: ${operation}`)
  wrapper
    .runTestmo(operation)
    .then(returnCode => {
      if (returnCode != 0) {
        core.setFailed(`Process executed returned code ${returnCode}`)
        return
      }
    })
    .catch(reason => core.setFailed(`Calling Testmo CLI failed due to ${reason}`))
}

// Run main function
run()
