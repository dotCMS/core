import * as core from '@actions/core'
import fetch, {Response} from 'node-fetch'
import * as shelljs from 'shelljs'

interface Commit {
  sha: string
}

interface ModuleConf {
  module: string
  folder?: string
  main?: boolean
}

const current = core.getInput('current')
const modulesConf: ModuleConf[] = JSON.parse(core.getInput('modules'))
const pullRequest = core.getInput('pull_request')
const commit = core.getInput('commit')

/**
 * Discover modules that were "touched" by changed contained either in pull request or commit
 * @returns list of modified modules
 */
export const moduleMatches = async (): Promise<boolean> => {
  validateConf()

  core.info(`Provided current momdule: ${current}`)
  core.info(
    `Provided modules configuration: ${JSON.stringify(modulesConf, null, 2)}`
  )

  const currentModule = modulesConf.find(conf => conf.module === current)
  if (!currentModule) {
    core.error(`Module ${current} was not found in configuration`)
    return false
  }

  const commits = await resolveCommits()

  const found = searchInCommits(currentModule, commits)
  found
    ? core.info(`Current module ${module} matched with changes`)
    : core.warning(
        `Could not match module ${module} with changes, disrding it...`
      )

  return found
}

const validateConf = (): ModuleConf => {
  const main = modulesConf.find(conf => !!conf.main)
  if (!main) {
    throw new Error(
      `No main module was found at modules configuration: ${JSON.stringify(
        modulesConf,
        null,
        2
      )}`
    )
  }
  return main
}

const searchInCommits = (module: ModuleConf, commits: string[]): boolean => {
  for (const sha of commits) {
    const output =
      shelljs.exec(`git diff-tree --no-commit-id --name-only -r ${sha}`)
        ?.stdout || ''
    const changed = output.split('\n')
    if (searchInChanges(module, changed)) {
      return true
    }
  }

  return false
}

const searchInChanges = (module: ModuleConf, changed: string[]): boolean => {
  for (const change of changed) {
    const normalized = change.trim()
    if (doesCurrentMatch(module, normalized)) {
      core.info(
        `Found modified module ${JSON.stringify(
          module,
          null,
          2
        )} from change at ${normalized}`
      )
      return true
    }
  }

  return false
}

const resolveCommits = async (): Promise<string[]> => {
  if (pullRequest) {
    core.info(`Provided pull request: ${pullRequest}`)
    const response = await getPullRequestCommits()
    if (!response.ok) {
      core.warning(`Could not get Github pull request ${pullRequest}`)
      return []
    }

    const commits = ((await response.json()) as Commit[]).map(c => c.sha)
    core.info(
      `Found pull request ${pullRequest} commits: ${commits.join(', ')}`
    )
    return commits
  } else if (commit) {
    core.info(`Found (push) commit: ${commit}`)
    return await Promise.resolve([commit])
  } else {
    core.warning('No commits found')
    return await Promise.resolve([])
  }
}

/**
 * Uses fetch function to send GET http request to Github API to get pull request commits data.
 *
 * @param pullRequest pull request
 * @returns {@link Response} object
 */
const getPullRequestCommits = async (): Promise<Response> => {
  const url = `https://api.github.com/repos/dotCMS/core/pulls/${pullRequest}/commits`
  core.info(`Sending GET to ${url}`)
  const response: Response = await fetch(url, {method: 'GET'})
  core.info(`Got response:\n${JSON.stringify(response.body, null, 2)}`)
  return response
}

const location = (module: ModuleConf): string => module.folder || module.module

const doesCurrentMatch = (module: ModuleConf, change: string): boolean => {
  if (!!module.main) {
    return !!shelljs
      .ls('-A', module.folder || '.')
      .find(file => change.startsWith(file))
  }

  return change.startsWith(location(module))
}
