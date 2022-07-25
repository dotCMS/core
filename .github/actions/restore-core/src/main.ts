import * as core from '@actions/core'
import * as restore from './restore'
import {CacheMetadata} from './restore'

/**
 * Main entry point for this action.
 */
const run = () => {
  // Call module logic to restore cache locations
  const cacheMetadataInput = core.getInput('cache_metadata')
  core.info(`Using cache metadata ${cacheMetadataInput}`)
  const cacheMetadata: CacheMetadata = JSON.parse(cacheMetadataInput)
  restore.restoreLocations(cacheMetadata).then(cacheLocations => {
    const cacheLocationsOutput = JSON.stringify(cacheLocations, null, 2)
    core.info(`Found these cache locations: ${cacheLocationsOutput}`)
    core.setOutput('cache_locations', cacheLocationsOutput)
  })
}

// Run main function
run()
