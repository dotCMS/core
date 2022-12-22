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
  parent?: string
}

/**
 * Resolves which field to use
 *
 * @param module {@link ModuleConf} module to get info from
 * @returns value in resolved field
 */
const location = (module: ModuleConf): string => module.folder || module.module

const buildId = core.getInput('build_id')
const current = core.getInput('current')
const modulesConf: ModuleConf[] = JSON.parse(core.getInput('modules'))
const childModules = modulesConf
  .filter(m => m.module !== current && m.parent === current)
  .map(m => location(m))
const pullRequest = core.getInput('pull_request')
const commit = core.getInput('commit')

/**
 * Discover modules that were "touched" by changed contained either in pull request or commit
 *
 * @returns true if module was found
 */
export const moduleMatches = async (): Promise<boolean> => {
  validateConf()

  core.info(`Provided current module: ${current}`)
  core.info(
    `Provided modules configuration: ${JSON.stringify(modulesConf, null, 2)}`
  )

  const currentModule = modulesConf.find(conf => conf.module === current)
  if (!currentModule) {
    core.error(`Module ${current} was not found in configuration`)
    return false
  }

  if (/^release-\d{2}\.\d{2}(\.\d{1,2})?$|master/.test(buildId)) {
    core.info('Master o release branch detected, allowing workflow to run')
    return true
  }

  const commits = await resolveCommits()
  core.info(`Commits found: ${commits.length}`)
  if (commits.length >= 100) {
    // Probably not found bu the amount of commits definitively calls for returning true
    core.info('Commits reached max capacity, allowing workflow to run')
    return true
  }

  const found = searchInCommits(currentModule, commits)
  found
    ? core.info(`Current module ${current} matched with changes`)
    : core.warning(
        `Could not match module ${current} with changes, discarding it...`
      )

  return found
}

/**
 * Validates provided module configuration
 *
 * @returns {@link ModuleConf} object if found otherwise undefined
 */
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

/**
 * Searches in given commit list if module appears inside them
 *
 * @param module {@link ModuleConf} instance to search
 * @param commits list of commits
 * @returns true if found, otherwise false
 */
const searchInCommits = (module: ModuleConf, commits: string[]): boolean => {
  for (const sha of commits) {
    const cmd = `git diff-tree --no-commit-id --name-only -r ${sha}`
    core.info(`Searching in commit ${sha} by running:\n${cmd}`)

    const output = shelljs.exec(cmd)?.stdout || ''
    const changed = output.split('\n')
    if (searchInChanges(module, changed)) {
      return true
    }
  }

  return false
}

/**
 * Searches in given commit changes if module appears in changes
 *
 * @param module {@link ModuleConf} instance to search
 * @param changed list of changes
 * @returns true if found, otherwise false
 */
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

/**
 * If a pull request is detected then return the list of commits included in PR.
 * It a push is detected use actual commit that triggered run.
 *
 * @returns commits as a list of string
 */
const resolveCommits = async (): Promise<string[]> => {
  if (pullRequest) {
    core.info(`Provided pull request: ${pullRequest}`)
    const response = await getPullRequestCommits()
    if (!response.ok) {
      core.warning(`Could not get Github pull request ${pullRequest}`)
      return []
    }

    const commits = (JSON.parse(await response.text()) as Commit[]).map(
      c => c.sha
    )
    core.info(
      `Found pull request ${pullRequest} commits:\n${commits.join(', ')}`
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
 * @returns {@link Response} object
 */
const getPullRequestCommits = async (): Promise<Response> => {
  const url = `https://api.github.com/repos/dotCMS/core/pulls/${pullRequest}/commits?per_page=250`
  core.info(`Sending GET to ${url}`)
  const response: Response = await fetch(url, {method: 'GET'})
  core.info(`Got response: ${response.status}`)
  return response
}

/**
 * Evaluates if module is included in change for the cases when the module is configured as main and when is a "child" module.
 *
 * @param module {@link ModuleConf} module instance
 * @param change line of change from commit
 * @returns true it matches, otherwise false
 */
const doesCurrentMatch = (module: ModuleConf, change: string): boolean => {
  if (!!module.main) {
    const folder = module.folder || '.'
    const list = shelljs
      .ls('-A', folder)
      .filter(file => !childModules.find(cm => file.startsWith(cm)))
    return !!list.find(file => change.startsWith(file))
  }

  return change.startsWith(location(module))
}
