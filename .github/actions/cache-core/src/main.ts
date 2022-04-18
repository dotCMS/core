import * as core from '@actions/core'
import * as cache from './cache'

/**
 * Main entry point for this action.
 */
const run = () => {
  cache.cacheCore().then(cacheMetadata => {
    const cacheMetadataOutput = JSON.stringify(cacheMetadata, null, 2)
    core.info(`Cache results:\n${cacheMetadataOutput}`)
    core.setOutput('cache_metadata', cacheMetadataOutput)
  })
}

// Run main function
run()
