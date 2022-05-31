import * as core from '@actions/core'
import * as matcher from './module-matcher'

/**
 * Main entry point for this action.
 */
const run = async () => {
  // Call module logic to discover modules
  try {
    const moduleFound = await matcher.moduleMatches()
    core.info(`Module found: [ ${moduleFound} ]`)
    core.setOutput('module_found', moduleFound)
  } catch (err) {
    core.setOutput('module_found', false)
    core.setFailed(`Failing workflow due to ${err}`)
  }
}

// Run main function
run()
