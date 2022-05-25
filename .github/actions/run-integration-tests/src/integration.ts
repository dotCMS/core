import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as path from 'path'
import * as setup from './it-setup'

const buildEnv = core.getInput('build_env')
const projectRoot = core.getInput('project_root')
const workspaceRoot = path.dirname(projectRoot)
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const dbType = core.getInput('db_type')
const runtTestsPrefix = 'integration-tests:'

/**
 * Based on dbType resolves the ci index
 * @returns
 */
const resolveCiIndex = (): number => {
  if (dbType === 'postgres') {
    return 0
  } else if (dbType === 'mssql') {
    return 1
  }

  return -1
}

export interface Command {
  cmd: string
  args: string[]
  workingDir?: string
  outputDir?: string
  reportDir?: string
  ciIndex?: number
  env?: {[key: string]: string}
}

interface DatabaseEnvs {
  postgres: {[key: string]: string}
  mssql: {[key: string]: string}
}

const DATABASE_ENVS: DatabaseEnvs = {
  postgres: {
    POSTGRES_USER: 'postgres',
    POSTGRES_PASSWORD: 'postgres',
    POSTGRES_DB: 'dotcms'
  },
  mssql: {}
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
    outputDir: `${dotCmsRoot}/build/test-results/integrationTest`,
    reportDir: `${dotCmsRoot}/build/reports/tests/integrationTest`,
    ciIndex: resolveCiIndex()
  },
  maven: {
    cmd: './mvnw',
    args: ['integrationTest', `-PdatabaseType=${dbType}`],
    workingDir: dotCmsRoot,
    outputDir: `${dotCmsRoot}/build/test-results/integrationTest`,
    reportDir: `${dotCmsRoot}/build/reports/tests/integrationTest`,
    ciIndex: resolveCiIndex()
  }
}

const START_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'up'],
  workingDir: `${projectRoot}/cicd/docker`,
  env: DATABASE_ENVS[dbType as keyof DatabaseEnvs]
}

const STOP_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'down'],
  workingDir: `${projectRoot}/cicd/docker`,
  env: DATABASE_ENVS[dbType as keyof DatabaseEnvs]
}

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run integration tests.
 *
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
export const runTests = async (cmd: Command): Promise<number> => {
  // Setup ITs
  setup.setupTests(propertyMap())

  // Starting dependencies
  core.info(`
    =======================================
    Starting integration tests dependencies
    =======================================`)
  core.info(`Executing command: ${START_DEPENDENCIES_CMD.cmd} ${START_DEPENDENCIES_CMD.args.join(' ')}`)
  exec.exec(START_DEPENDENCIES_CMD.cmd, START_DEPENDENCIES_CMD.args, {cwd: START_DEPENDENCIES_CMD.workingDir})

  await waitFor(30, `ES and ${dbType}`)

  // Executes ITs
  resolveParams(cmd)
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
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (cmd: Command) => {
  const tests = core.getInput('tests')?.trim()
  if (!tests) {
    core.info('No specific integration tests found')
    return
  }

  core.info(`Found tests to run: "${tests}"`)

  tests.split('\n').forEach(l => {
    const line = l.trim()
    if (!line.toLowerCase().startsWith(runtTestsPrefix)) {
      return
    }

    const testLine = line.slice(runtTestsPrefix.length).trim()
    if (buildEnv === 'gradle') {
      testLine.split(',').forEach(test => {
        cmd.args.push('--tests')
        cmd.args.push(test.trim())
      })
    } else if (buildEnv === 'maven') {
      const normalized = testLine
        .split(',')
        .map(t => t.trim())
        .join(',')
      cmd.args.push(`-Dit.test=${normalized}`)
    }
  })

  core.info(`Resolved params ${cmd.args.join(' ')}`)
}

/**
 * Delays the resolve part of a promise to simulate a sleep
 *
 * @param seconds number of seconds
 * @returns void promise
 */
const delay = (seconds: number) => new Promise(resolve => setTimeout(resolve, seconds * 1000))

/**
 * Waits for specific time with corresponding messages.
 *
 * @param wait time to wait
 * @param startLabel start label
 * @param endLabel endlabel
 */
const waitFor = async (wait: number, startLabel: string, endLabel?: string) => {
  core.info(`Waiting ${wait} seconds for ${startLabel}`)
  await delay(wait)
  const finalLabel = endLabel || startLabel
  core.info(`Waiting on ${finalLabel} loading has ended`)
}
