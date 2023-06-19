import * as core from '@actions/core'
import * as fs from 'fs'
import * as path from 'path'

interface CacheLocations {
  dependencies?: string[]
  buildOutput?: string[]
}

interface CacheConfiguration {
  gradle: CacheLocations
  maven: CacheLocations
}

const HOME_FOLDER = path.join('/home', 'runner')
const GRADLE_FOLDER = path.join(HOME_FOLDER, '.gradle')
const M2_FOLDER = path.join(HOME_FOLDER, '.m2')
const PROJECT_ROOT = core.getInput('project_root')
const DOTCMS_ROOT = path.join(PROJECT_ROOT, 'dotCMS')
const CACHE_CONFIGURATION: CacheConfiguration = {
  gradle: {
    dependencies: [
      path.join(GRADLE_FOLDER, 'caches'),
      path.join(GRADLE_FOLDER, 'wrapper'),
      path.join(M2_FOLDER, 'repository')
    ],
    buildOutput: [
      path.join(DOTCMS_ROOT, '.gradle'),
      path.join(DOTCMS_ROOT, 'gradle'),
      path.join(DOTCMS_ROOT, 'build', 'classes'),
      path.join(DOTCMS_ROOT, 'build', 'generated'),
      path.join(DOTCMS_ROOT, 'build', 'resources') /*,
      path.join(PROJECT_ROOT, 'dist', 'dotserver')*/
    ]
  },
  maven: {
    dependencies: [path.join(M2_FOLDER, 'repository')],
    buildOutput: [path.join(DOTCMS_ROOT, 'target')]
  }
}

const EMPTY_CACHE_LOCATIONS: CacheLocations = {
  dependencies: [],
  buildOutput: []
}

/**
 * Resolves locations to be cached after building core.
 *
 * @returns a {@link CacheLocation} instance with the information of what is going to be cached
 */
export const getCacheLocations = (): CacheLocations => {
  // Resolves build environment
  const buildEnv = core.getInput('build_env')
  core.info(`Resolving cache locations with buid env ${buildEnv}`)

  const cacheLocations = CACHE_CONFIGURATION[buildEnv as keyof CacheConfiguration]
  if (!cacheLocations) {
    core.warning(`Cannot find cache configuration for build env ${buildEnv}`)
    return EMPTY_CACHE_LOCATIONS
  }

  // For each cache location resolves the location to be cached
  for (const key of Object.keys(cacheLocations)) {
    resolveLocations(buildEnv, cacheLocations, key, undefined)
  }

  return cacheLocations
}

/**
 * Adds to a {@link CacheLocation[]} the locations for each lcoation type (key).
 * 
 * @param buildEnv build environtment (gradle or maven)
 * @param cacheLocations cache locations based on the build env
 * @param key location type (depedencies or buildOutput)
 * @param decorateFn function to do some extra decoration to locations path

 */
const resolveLocations = (
  buildEnv: string,
  cacheLocations: CacheLocations,
  key: string,
  decorateFn?: (location: string) => string
) => {
  const cacheEnableKey = key.replace(/[A-Z]/g, letter => `_${letter.toLowerCase()}`)
  const cacheEnable = core.getBooleanInput(`cache_${cacheEnableKey}`)
  const locationKey = key as keyof CacheLocations
  if (!cacheEnable) {
    core.notice(`Core cache is disabled for ${key}`)
    cacheLocations[locationKey] = undefined
    return
  }

  core.info(`Looking cache configuration for ${key}`)
  const locations = cacheLocations[locationKey]
  if (!locations) {
    core.warning(`Cannot resolve any ${key} locations for build env ${buildEnv}`)
    return
  }

  core.info(`Found ${key} locations: ${locations}`)
  const resolvedLocations = locations.map(location => {
    const newLocation = decorateFn ? decorateFn(location) : location

    if (location !== newLocation) {
      core.info(`Relocating cache from ${location} to ${newLocation}`)
      fs.mkdirSync(path.dirname(newLocation), {recursive: true})
      fs.renameSync(location, newLocation)
    }

    return newLocation
  })

  core.info(`Resolved ${key} locations: ${resolvedLocations}`)
  cacheLocations[locationKey] = resolvedLocations
}
