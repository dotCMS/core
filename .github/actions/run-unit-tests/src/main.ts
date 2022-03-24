import * as core from '@actions/core'
import * as unit from './unit'

/**
 * Main entry point for this action.
 */
const run = () => {
  core.info('Running Core unit tests')
  unit
    .runTests(core.getInput('build-env'))
    .then(returnCode => {
      if (returnCode != 0) {
        core.setFailed(`Process executed returned code ${returnCode}`)
        return
      }
    })
    .catch(reason => core.setFailed(`Running unit tests failed due to ${reason}`))
}

// Run main function
run()
