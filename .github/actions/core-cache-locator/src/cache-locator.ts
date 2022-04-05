import * as core from '@actions/core'
import * as path from 'path'

interface CacheLocations {
  dependencies: string[]
  buildOutput: string[]
}
interface CacheConfiguration {
  gradle: CacheLocations
  maven: CacheLocations
}

const CACHE_CONFIGURATION: CacheConfiguration = {
  gradle: {
    dependencies: ['~/.gradle/caches', '~/.gradle/wrapper'],
    buildOutput: [
      'dotCMS/.gradle',
      'dotCMS/build/classes',
      'dotCMS/build/resources'
    ]
  },
  maven: {
    dependencies: ['~/.m2/repository'],
    buildOutput: ['dotCMS/target']
  }
}

const EMPTY_CACHE_LOCATIONS: CacheLocations = {
  dependencies: [],
  buildOutput: []
}

const BUILD_OUTPUT = 'buildOutput'
const CACHE_FOLDER = path.join(
  path.dirname(core.getInput('core-root')),
  'cache'
)

/**
 * Resolves locations to be cached after building core.
 *
 * @returns a {@link CacheLocation} instance with the information of what is going to be cached
 */
export const getCacheLocations = (): CacheLocations => {
  // Resolves build tool environment
  const buildToolEnv = core.getInput('build-tool-env')
  core.info(`Resolving cache locations with buid tool ${buildToolEnv}`)

  const cacheLocations =
    CACHE_CONFIGURATION[buildToolEnv as keyof CacheConfiguration]
  if (!cacheLocations) {
    core.warning(
      `Cannot find cache configuration for build tool ${buildToolEnv}`
    )
    return EMPTY_CACHE_LOCATIONS
  }

  // For each cache location resolves the location to be cached
  Object.keys(cacheLocations).forEach(key =>
    resolveLocations(
      buildToolEnv,
      cacheLocations,
      key,
      key === BUILD_OUTPUT ? decorateBuildOutput : undefined
    )
  )

  return cacheLocations
}

/**
 * Adds to a {@link CacheLocation[]} the locations for each lcoation type (key).
 * 
 * @param buildToolEnv build tool environtment (gradle or maven)
 * @param cacheLocations cache locations based on the build tool env
 * @param key location type (depedencies or buildOutput)
 * @param decorateFn function to do some extra decoration to locations path

 */
const resolveLocations = (
  buildToolEnv: string,
  cacheLocations: CacheLocations,
  key: string,
  decorateFn?: (location: string) => string
) => {
  const cacheEnableKey = key.replace(
    /[A-Z]/g,
    letter => `-${letter.toLowerCase()}`
  )
  const cacheEnable = core.getBooleanInput(`cache-${cacheEnableKey}`)
  if (!cacheEnable) {
    core.warning(`Core cache is disabled for ${key}`)
    return
  }

  core.info(`Looking cache configuration for ${key}`)
  const locationKey = key as keyof CacheLocations
  const locations = cacheLocations[locationKey]
  if (!locations) {
    core.warning(
      `Cannot resolve any ${key} locations for build tool ${buildToolEnv}`
    )
    return
  }

  core.info(`Found ${key} locations: ${locations}`)
  const resolvedLocations = locations.map(location =>
    decorateFn ? decorateFn(location) : location
  )
  core.info(`Resolved ${key} locations: ${resolvedLocations}`)
  cacheLocations[locationKey] = resolvedLocations
}

/**
 * Decoration function for build output
 *
 * @param location location
 * @returns decorated string
 */
const decorateBuildOutput = (location: string): string =>
  path.join(CACHE_FOLDER, path.basename(location))
