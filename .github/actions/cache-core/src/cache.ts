import * as cache from '@actions/cache'
import * as core from '@actions/core'

export interface CacheLocations {
  dependencies: string[]
  buildOutput: string[]
}

interface BuildToolCacheKeys {
  dependencies: string
  buildOutput: string
}

interface CacheKeys {
  gradle: BuildToolCacheKeys
  maven: BuildToolCacheKeys
}

interface CacheMetadata {
  cacheKey: string
  cacheLocations: string[]
  cacheId?: number
}
interface CacheResults {
  buildToolEnv: string
  metadata: CacheMetadata[]
}

const EMPTY_CACHE_RESULT: CacheMetadata = {
  cacheKey: '',
  cacheLocations: []
}

/**
 * Uses cache library to cache a provided collection of locations.
 *
 * @returns a {@link Promise<CacheResults>} with data about the cache operation: key, locations and cache id
 */
export const cacheCore = async (): Promise<CacheResults> => {
  const buildToolEnv: string = core.getInput('build-tool-env')
  core.info(`Resolving cache locations with buid tool ${buildToolEnv}`)

  const cacheLocations: CacheLocations = JSON.parse(
    core.getInput('cache-locations')
  )
  core.info(
    `Attempting to cache core using these locations:\n ${JSON.stringify(
      cacheLocations,
      null,
      2
    )}`
  )

  const availableCacheKeysStr = core.getInput('available-cache-keys')
  core.info(`Available cache keys: ${availableCacheKeysStr}`)
  const availableCacheKeys: CacheKeys = JSON.parse(
    core.getInput('available-cache-keys')
  )

  const cacheKeys = availableCacheKeys[buildToolEnv as keyof CacheKeys]
  core.info(`Cache keys: ${JSON.stringify(cacheKeys, null, 2)}`)

  const locations = Object.keys(cacheLocations)
  core.info(`Caching these locations: ${locations}`)

  const cacheResults: CacheResults = {
    buildToolEnv,
    metadata: []
  }
  for (const locationType of locations) {
    const cacheResult: CacheMetadata = await cacheLocation(
      cacheLocations,
      cacheKeys,
      locationType
    )
    if (cacheResult !== EMPTY_CACHE_RESULT) {
      cacheResults.metadata.push(cacheResult)
    }
  }

  return new Promise<CacheResults>(resolve => resolve(cacheResults))
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
  resolvedKeys: BuildToolCacheKeys,
  locationType: string
): Promise<CacheMetadata> => {
  const cacheKey = resolvedKeys[locationType as keyof BuildToolCacheKeys]
  const resolvedLocations = cacheLocations[locationType as keyof CacheLocations]
  core.info(
    `Caching locations:\n  [${resolvedLocations}]\n  with key: ${cacheKey}`
  )

  let cacheResult = EMPTY_CACHE_RESULT
  try {
    const cacheId = await cache.saveCache(resolvedLocations, cacheKey)
    core.info(`Cache id found: ${cacheId}`)
    cacheResult = {
      cacheKey,
      cacheLocations: resolvedLocations,
      cacheId
    }
    core.info(`Resolved cache result ${JSON.stringify(cacheResult)}`)
  } catch (err) {
    core.warning(`Could not cache using ${cacheKey} due to ${err}`)
  }

  return new Promise<CacheMetadata>(resolve => resolve(cacheResult))
}
