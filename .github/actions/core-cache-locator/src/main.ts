import * as core from '@actions/core'
import * as cacheLocator from './cache-locator'

/**
 * Main entry point for this action.
 */
const run = () => {
  // Call module logic to resolve cache locations
  const cacheLocations = JSON.stringify(
    cacheLocator.getCacheLocations(),
    null,
    2
  )
  core.info(`Found these tags: ${cacheLocations}`)
  core.setOutput('cache-locations', cacheLocations)
}

// Run main function
run()
