import * as core from '@actions/core'
import * as builder from './docker-image-builder'

/**
 * Main entry point for this action.
 */
const run = () => {
  builder
    .build()
    .then(returnCode => {
      if (returnCode !== 0) {
        core.setFailed(`Process executed returned code ${returnCode}`)
        return
      }
    })
    .catch(reason => core.setFailed(`Docker build of DotCMS failed due to ${reason}`))
}

// Run main function
run()
