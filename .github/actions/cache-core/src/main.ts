import * as core from '@actions/core'
import * as cache from './cache'

/**
 * Main entry point for this action.
 */
const run = () => {
  cache.cacheCore().then(cacheResults => {
    const cacheOutput = JSON.stringify(cacheResults, null, 2)
    core.info(`Cache results:\n${cacheOutput}`)
    core.setOutput('cache-results', cacheOutput)
  })
}

// Run main function
run()
