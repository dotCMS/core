import * as core from '@actions/core'
import * as resolver from './postman-collection-resolver'

const run = () => {
  const projectRoot = core.getInput('project_root')
  const collectionGroups = core.getInput('collection_groups')
  const current = core.getInput('current')
  const collections = resolver.resolve(projectRoot, collectionGroups, current)
  core.setOutput('collections_to_run', collections.join(','))
}

run()
