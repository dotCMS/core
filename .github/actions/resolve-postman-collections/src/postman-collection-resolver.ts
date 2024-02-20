import * as core from '@actions/core'
import * as path from 'path'
import * as shelljs from 'shelljs'

interface CollectionGroup {
  name: string
  collections: string[]
}

const DEFAULT_COLLECTION_GROUP = 'default'

export const resolve = (projectRoot: string, collectinGroupsStr: string, current: string): string[] => {
  const collectionGroups = JSON.parse(collectinGroupsStr) as CollectionGroup[]
  if (!collectionGroups) {
    return []
  }

  const postmanTestsPath = path.join(projectRoot, 'dotCMS', 'src', 'curl-test')
  const postmanEnvFile = 'postman_environment.json'
  const collectionFiles = shelljs.ls(postmanTestsPath).filter(file => file.endsWith('.json') && file !== postmanEnvFile)
  core.info(`Current collection group: [${current}]`)

  const resolved = resolveCollections(collectionGroups, current, collectionFiles)
  core.info(`Resolved collections [${resolved.join(', ')}]`)

  return resolved
}

const resolveCollections = (
  collectionGroups: CollectionGroup[],
  current: string,
  collectionFiles: string[]
): string[] => {
  const group = current?.trim() || DEFAULT_COLLECTION_GROUP
  const all = group === DEFAULT_COLLECTION_GROUP
  const resolved = (all ? [] : collectionGroups.find(pc => pc.name === group)?.collections || []).map(collection =>
    trimWithExt(collection)
  )

  if (resolved.length > 0) {
    return resolved.filter(collection => collectionFiles.includes(collection))
  }

  if (all) {
    const allParallel = collectionGroups.flatMap(pc => pc.collections).map(collection => trimWithExt(collection))
    return collectionFiles.filter(collection => !allParallel.includes(collection))
  }

  return []
}

const trimWithExt = (collection: string) => {
  const trimmed = collection.trim()
  return trimmed.endsWith('.json') ? trimmed : `${trimmed}.json`
}
