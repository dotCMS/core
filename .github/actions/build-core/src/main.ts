import * as core from '@actions/core'
import * as builder from './core-builder'

/**
 * Main entry point for this action.
 */
const run = () => {
  const buildEnv = core.getInput('build_env')
  core.info(`Attempting to build core with ${buildEnv}`)
  builder
    .build(buildEnv)
    .then(returnCode => {
      if (returnCode != 0) {
        core.setFailed(`Process executed returned code ${returnCode}`)
        return
      }
    })
    .catch(reason => core.setFailed(`Build core failed due to ${reason}`))
}

// Run main function
run()
