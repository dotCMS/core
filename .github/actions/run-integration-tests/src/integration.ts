import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as path from 'path'
import * as setup from './it-setup'

const projectRoot = core.getInput('project_root')
const workspaceRoot = path.dirname(projectRoot)
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const dbType = core.getInput('db_type')

interface Command {
  cmd: string
  args: string[]
  workingDir?: string
  outputDir?: string
}

export interface Commands {
  gradle: Command
  maven: Command
}

export const COMMANDS: Commands = {
  gradle: {
    cmd: './gradlew',
    args: ['integrationTest', `-PdatabaseType=${dbType}`],
    workingDir: dotCmsRoot,
    outputDir: `${dotCmsRoot}/build/reports/tests/integrationTest/xml`
  },
  maven: {
    cmd: './mvnw',
    args: ['integrationTest', `-PdatabaseType=${dbType}`],
    workingDir: dotCmsRoot,
    outputDir: `${dotCmsRoot}/target/surefire-reports`
  }
}

const START_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'up', '-d'],
  workingDir: `${projectRoot}/cicd/docker`
}

const STOP_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'down'],
  workingDir: `${projectRoot}/cicd/docker`
}

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run integration tests.
 *
 * @param buildEnv build environment
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
export const runTests = async (buildEnv: string, cmd: Command): Promise<number> => {
  // Setup ITs
  setup.setupTests(propertyMap())

  // Starting dependencies
  core.info(`
    =======================================
    Starting integration tests dependencies
    =======================================`)
  core.info(`Executing command: ${START_DEPENDENCIES_CMD.cmd} ${START_DEPENDENCIES_CMD.args.join(' ')}`)
  await exec.exec(START_DEPENDENCIES_CMD.cmd, START_DEPENDENCIES_CMD.args, {cwd: START_DEPENDENCIES_CMD.workingDir})

  // Wait until DB is ready
  const wait = 30
  core.info(`
    Waiting ${wait} seconds for dependencies: ES and ${dbType}`)
  await delay(wait)
  core.info(`
    Waiting on dependencies (ES and ${dbType}) loading has ended`)

  // Executes ITs
  resolveParams(buildEnv, cmd)
  core.info(`
    ===========================================
    Running integration tests against ${dbType}
    ===========================================`)
  core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
  const itCode = await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})

  // Starting dependencies
  core.info(`
    =======================================
    Stopping integration tests dependencies
    =======================================`)
  core.info(`Executing command: ${STOP_DEPENDENCIES_CMD.cmd} ${STOP_DEPENDENCIES_CMD.args.join(' ')}`)
  await exec.exec(STOP_DEPENDENCIES_CMD.cmd, STOP_DEPENDENCIES_CMD.args, {cwd: STOP_DEPENDENCIES_CMD.workingDir})

  return itCode
}

/**
 * Creates property map with DotCMS property information to override/append
 *
 * @returns map with property data
 */
const propertyMap = (): Map<string, string> => {
  const properties = new Map<string, string>()
  properties.set('dotSecureFolder', appendToWorkspace('custom/dotsecure'))
  properties.set('dotCmsFolder', dotCmsRoot)
  properties.set('felixFolder', appendToWorkspace('custom/felix'))
  properties.set('assetsFolder', appendToWorkspace('custom/assets'))
  properties.set('esDataFolder', appendToWorkspace('custom/esdata'))
  properties.set('logsFolder', appendToWorkspace('custom/output/logs'))
  properties.set('dbType', dbType)
  return properties
}

/**
 * Append folder to workspace
 *
 * @param folder folder to append to workspace
 * @returns workspace + folder
 */
const appendToWorkspace = (folder: string): string => path.join(workspaceRoot, folder)

/**
 * Resolve paramateres to produce command arguments
 *
 * @param buildEnv build environment
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (buildEnv: string, cmd: Command) => {
  const extraParams = core.getInput('extra_params')?.trim()
  core.info(`Found extra params: "${extraParams}"`)
  const testSuiteParam = '-Dtest.single=com.dotcms.MainSuite'

  let itParams = testSuiteParam
  switch (buildEnv) {
    case 'gradle': {
      if (extraParams) {
        itParams = extraParams
        if (extraParams.includes('--tests')) {
          itParams = `${extraParams} ${testSuiteParam}`
        }
      }
      break
    }
    case 'maven': {
      break
    }
  }

  itParams.split(' ').forEach(param => cmd.args.push(param))
  core.info(`Resolved parameters: "${cmd.args.join(' ')}"`)
}

/**
 * Delays the resolve part of a promise to simulate a sleep
 *
 * @param seconds number of seconds
 * @returns void promise
 */
const delay = (seconds: number) => new Promise(resolve => setTimeout(resolve, seconds * 1000))
