import * as cache from '@actions/cache'
import * as core from '@actions/core'
import * as exec from '@actions/exec'
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

const HOME_FOLDER = path.join('/home', 'runner')
const GRADLE_FOLDER = path.join(HOME_FOLDER, '.gradle')
const M2_FOLDER = path.join(HOME_FOLDER, '.m2')
const PROJECT_ROOT = core.getInput('project_root')
const TOMCAT_WEBINF = path.join(PROJECT_ROOT, 'dist', 'dotserver', 'tomcat-9.0.60', 'webapps', 'ROOT', 'WEB-INF')
const DOTCMS_ROOT = path.join(PROJECT_ROOT, 'dotCMS')
const RESTORE_CONFIGURATION: RestoreConfiguration = {
  gradle: {
    dependencies: [path.join(GRADLE_FOLDER, 'caches'), path.join(GRADLE_FOLDER, 'wrapper')],
    buildOutput: [
      path.join(DOTCMS_ROOT, '.gradle'),
      path.join(DOTCMS_ROOT, 'gradle'),
      path.join(DOTCMS_ROOT, 'build', 'classes'),
      path.join(DOTCMS_ROOT, 'build', 'generated'),
      path.join(DOTCMS_ROOT, 'build', 'resources'),
      path.join(TOMCAT_WEBINF, 'felix'),
      path.join(TOMCAT_WEBINF, 'felix-system')
    ]
  },
  maven: {
    dependencies: [path.join(M2_FOLDER, 'repository')],
    buildOutput: [path.join(DOTCMS_ROOT, 'target')]
  }
}

const CACHE_FOLDER = path.join(path.dirname(PROJECT_ROOT), 'cache')

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
    for (const location of locationMetadata.cacheLocations) {
      du(location)
    }

    const type = locationMetadata.type as keyof CacheLocations
    const configLocations = restoreConfig[type]
    if (!configLocations || configLocations.length === 0) {
      core.setFailed(`Could not resolve config locations from ${type}`)
      return Promise.resolve(EMPTY_LOCATIONS)
    }

    cacheLocations[type] = relocate(type, locationMetadata.cacheLocations, configLocations, [])
  }

  return new Promise<CacheLocations>(resolve => resolve(cacheLocations))
}

/**
 * Once cached locations are restored, for every location type that requires to relocate folders it does so.
 *
 * @param type location type (dependencies or buildOutput)
 * @param cacheLocations cache locations arrays
 * @param configLocations config locations extracted from restore configuration
 * @param rellocatable list of location types that requires rellocating
 * @returns relocated restored locations
 */
const relocate = (
  type: string,
  cacheLocations: string[],
  configLocations: string[],
  rellocatable: string[]
): string[] => {
  const isRellocatable = !!rellocatable.find(r => r === type)
  if (!isRellocatable) {
    core.info(`Not relocating any cache for ${type}`)
    for (const location of cacheLocations) {
      const parent = path.dirname(location)
      if (!fs.existsSync(parent)) {
        core.info(`Cache location parent ${parent} does not exist, creating it`)
        fs.mkdirSync(parent, {recursive: true})
      }
    }
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

      const newLocation = path.join(PROJECT_ROOT, foundLocation)
      core.info(`New location for cache: ${newLocation}`)
      const newFolder = path.dirname(newLocation)
      if (!fs.existsSync(newFolder)) {
        core.info(`New location folder ${newFolder} does not exist, creating it`)
        fs.mkdirSync(newFolder, {recursive: true})
      }

      core.info(`Relocating cache from ${location} to ${newLocation}`)
      fs.renameSync(location, newLocation)
      du(newLocation)

      return newLocation
    })
    .filter(location => location !== '')
}

const du = async (location: string) => {
  core.info(`Listing folder ${location}`)
  try {
    await exec.exec('du', ['-hs', location])
  } catch (err) {
    core.info(`Cannot list folder ${location}`)
  }
}
