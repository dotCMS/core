import * as cache from '@actions/cache'
import * as core from '@actions/core'
import * as fs from 'fs'
import * as path from 'path'

interface CacheLocationMetadata {
  cacheKey: string
  type: string
  cacheLocations: string[]
  cacheId?: number
}

export interface CacheMetadata {
  buildEnv: string
  locations: CacheLocationMetadata[]
}

export interface CacheLocations {
  dependencies: string[]
  buildOutput: string[]
}

const EMPTY_LOCATIONS: CacheLocations = {
  dependencies: [],
  buildOutput: []
}
const BUILD_OUTPUT = 'buildOutput'

interface RestoreConfiguration {
  gradle: CacheLocations
  maven: CacheLocations
}

const RESTORE_CONFIGURATION: RestoreConfiguration = {
  gradle: {
    dependencies: ['~/.gradle/caches', '~/.gradle/wrapper'],
    buildOutput: ['dotCMS/.gradle', 'dotCMS/build/classes', 'dotCMS/build/resources']
  },
  maven: {
    dependencies: ['~/.m2/repository'],
    buildOutput: ['dotCMS/target']
  }
}

const CORE_ROOT = core.getInput('core-root')
const CACHE_FOLDER = path.join(path.dirname(CORE_ROOT), 'cache')

/**
 * Restore previously cached locations.
 *
 * @param cacheMetadata {@link CacheMetadata} object that created with cache information.
 * @returns cache metadata object
 */
export const restoreLocations = async (cacheMetadata: CacheMetadata): Promise<CacheLocations> => {
  const cacheLocations: CacheLocations = {
    dependencies: [],
    buildOutput: []
  }

  const buildEnv = cacheMetadata.buildEnv as keyof RestoreConfiguration
  const restoreConfig = RESTORE_CONFIGURATION[buildEnv]
  if (!restoreConfig) {
    core.setFailed(`Could not resolve restore configuration from build env ${buildEnv}`)
    return Promise.resolve(EMPTY_LOCATIONS)
  }

  for (const locationMetadata of cacheMetadata.locations) {
    core.info(`Restoring locations:\n[${locationMetadata.cacheLocations}]\nusing key: ${locationMetadata.cacheKey}`)

    if (locationMetadata.type === BUILD_OUTPUT) {
      locationMetadata.cacheLocations.forEach(location => {
        if (!fs.existsSync(location)) {
          core.info(`Location ${location} does not exist, creating it`)
          fs.mkdirSync(location, {recursive: true})
        }
      })
    }

    const cacheKey = await cache.restoreCache(locationMetadata.cacheLocations, locationMetadata.cacheKey)
    core.info(`Locations restored with key ${cacheKey}`)

    const type = locationMetadata.type as keyof CacheLocations
    const configLocations = restoreConfig[type]
    if (!configLocations || configLocations.length === 0) {
      core.setFailed(`Could not resolve config locations from ${type}`)
      return Promise.resolve(EMPTY_LOCATIONS)
    }

    cacheLocations[type] = relocate(type, locationMetadata.cacheLocations, configLocations)
  }

  return new Promise<CacheLocations>(resolve => resolve(cacheLocations))
}

/**
 * Once cached locations are restored, for every location type that requires to relocate folders it does
 *
 * @param type location type (dependencies or buildOutput)
 * @param cacheLocations cache locations arrays
 * @param configLocations config locations extracted from restore configuration
 * @returns relocated restored locations
 */
function relocate(type: string, cacheLocations: string[], configLocations: string[]): string[] {
  if (type !== BUILD_OUTPUT) {
    core.info(`Not relocating any cache for ${type}`)
    return cacheLocations
  }

  core.info(`Caches to relocate: ${cacheLocations}`)
  return cacheLocations
    .map(location => {
      const prefix = CACHE_FOLDER + path.sep
      const baseName = location.slice(prefix.length)
      const foundLocation = configLocations.find(configLocation => configLocation === baseName)
      if (!foundLocation) {
        return ''
      }

      const newLocation = path.join(CORE_ROOT, foundLocation)
      core.info(`New location for cache: ${newLocation}`)
      const newFolder = path.dirname(newLocation)
      if (!fs.existsSync(newFolder)) {
        core.info(`New location folder ${newFolder} does not exist, creating it`)
        fs.mkdirSync(newFolder)
      }

      core.info(`Relocating cache from ${location} to ${newLocation}`)
      fs.renameSync(location, newLocation)

      return newLocation
    })
    .filter(location => location !== '')
}
