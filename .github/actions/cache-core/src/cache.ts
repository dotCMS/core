import * as cache from '@actions/cache'
import * as core from '@actions/core'
import {ReserveCacheError} from '@actions/cache'

export interface CacheLocations {
  dependencies: string[]
  buildOutput: string[]
}

interface BuildEnvCacheKeys {
  dependencies: string
  buildOutput: string
}

interface CacheKeys {
  gradle: BuildEnvCacheKeys
  maven: BuildEnvCacheKeys
}

interface CacheLocationMetadata {
  cacheKey: string
  type: string
  cacheLocations: string[]
  cacheId?: number
}

interface CacheMetadata {
  buildEnv: string
  locations: CacheLocationMetadata[]
}

const EMPTY_CACHE_RESULT: CacheLocationMetadata = {
  cacheKey: '',
  type: '',
  cacheLocations: []
}

/**
 * Uses cache library to cache a provided collection of locations.
 *
 * @returns a {@link Promise<CacheMetadata>} with data about the cache operation: key, locations and cache id
 */
export const cacheCore = async (): Promise<CacheMetadata> => {
  const buildEnv: string = core.getInput('build_env')
  core.info(`Resolving cache locations with buid env ${buildEnv}`)

  const cacheLocations: CacheLocations = JSON.parse(core.getInput('cache_locations'))
  core.info(`Attempting to cache core using these locations:\n ${JSON.stringify(cacheLocations, null, 2)}`)

  const availableCacheKeysStr = core.getInput('available_cache_keys')
  core.info(`Available cache keys: ${availableCacheKeysStr}`)
  const availableCacheKeys: CacheKeys = JSON.parse(core.getInput('available_cache_keys'))

  const cacheKeys = availableCacheKeys[buildEnv as keyof CacheKeys]
  core.info(`Cache keys: ${JSON.stringify(cacheKeys, null, 2)}`)

  const cacheMetadata: CacheMetadata = {
    buildEnv: buildEnv,
    locations: []
  }

  const locationTypes = Object.keys(cacheLocations)
  core.info(`Caching these locations: ${locationTypes}`)
  for (const locationType of locationTypes) {
    const cacheLocationMetadata: CacheLocationMetadata = await cacheLocation(cacheLocations, cacheKeys, locationType)
    if (cacheLocationMetadata !== EMPTY_CACHE_RESULT) {
      cacheMetadata.locations.push(cacheLocationMetadata)
    }
  }

  return new Promise<CacheMetadata>(resolve => resolve(cacheMetadata))
}

/**
 * Do the actual caching of the provided locations
 *
 * @param cacheLocations provided locations
 * @param resolvedKeys keys to used for caching
 * @param locationType location type
 * @returns a {@link Promise<CacheResult>} with the caching operation data
 */
const cacheLocation = async (
  cacheLocations: CacheLocations,
  resolvedKeys: BuildEnvCacheKeys,
  locationType: string
): Promise<CacheLocationMetadata> => {
  const cacheKey = resolvedKeys[locationType as keyof BuildEnvCacheKeys]
  const resolvedLocations = cacheLocations[locationType as keyof CacheLocations]
  core.info(`Caching locations:\n  [${resolvedLocations}]\n  with key: ${cacheKey}`)

  let cacheResult = EMPTY_CACHE_RESULT
  try {
    const cacheId = await cache.saveCache(resolvedLocations, cacheKey)
    core.info(`Cache id found: ${cacheId}`)
    cacheResult = {
      cacheKey,
      type: locationType,
      cacheLocations: resolvedLocations,
      cacheId
    }
    core.info(`Resolved cache result ${JSON.stringify(cacheResult)}`)
  } catch (err) {
    if (err instanceof ReserveCacheError) {
      core.info(`${err}, so still considering for cache`)
      cacheResult = {
        cacheKey,
        type: locationType,
        cacheLocations: resolvedLocations
      }
    } else {
      core.warning(`Could not cache using ${cacheKey} due to ${err}`)
    }
  }

  return Promise.resolve(cacheResult)
}
