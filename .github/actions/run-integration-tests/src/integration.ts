import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as path from 'path'
import * as setup from './it-setup'
import fetch, {Response} from 'node-fetch'

/**
 * Based on dbType resolves the ci index
 *
 * @returns index based on provided db type
 */
const resolveCiIndex = (): number => {
  if (dbType === 'postgres') {
    return 0
  } else if (dbType === 'mssql') {
    return 1
  } else {
    return -1
  }
}

const buildEnv = core.getInput('build_env')
const projectRoot = core.getInput('project_root')
const workspaceRoot = path.dirname(projectRoot)
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const dbType = core.getInput('db_type') || 'postgres'
const runtTestsPrefix = 'integration-tests:'
const dockerFolder = `${projectRoot}/cicd/docker`
const outputDir = `${dotCmsRoot}/build/test-results/integrationTest`
const reportDir = `${dotCmsRoot}/build/reports/tests/integrationTest`
const ciIndex = resolveCiIndex()
const includeAnalytics = core.getBooleanInput('include_analytics')

interface CommandResult {
  exitCode: number
  outputDir?: string
  reportDir?: string
  ciIndex?: number
}

export interface Command {
  cmd: string
  args: string[]
  workingDir: string
  env?: {[key: string]: string}
}

interface DatabaseEnvs {
  postgres?: {[key: string]: string}
  mssql?: {[key: string]: string}
}

interface AccessToken {
  access_token: string
  token_type: string
  issueDate: string
  expires_in: number
  refresh_expires_in: number
  refresh_token: string
  scope: string
  clientId: string
  aud: string
}

interface AnalyticsKey {
  jsKey: string
  m2mKey: string
}

const DEPS_ENV: DatabaseEnvs = {
  postgres: {
    POSTGRES_USER: 'postgres',
    POSTGRES_PASSWORD: 'postgres',
    POSTGRES_DB: 'dotcms'
  }
}

export interface Commands {
  gradle: Command[]
  maven: Command[]
}

export const COMMANDS: Commands = {
  gradle: [
    {
      cmd: './gradlew',
      args: ['generateDependenciesFromMaven'],
      workingDir: dotCmsRoot
    },
    {
      cmd: './gradlew',
      args: ['integrationTest', `-PdatabaseType=${dbType}`, '--stacktrace'],
      workingDir: dotCmsRoot
    }
  ],
  maven: [
    {
      cmd: './mvnw',
      args: ['integrationTest', `-PdatabaseType=${dbType}`],
      workingDir: dotCmsRoot
    }
  ]
}

const START_ANALYTICS_INFRA_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'analytics-compose.yml', 'up'],
  workingDir: dockerFolder
}

const STOP_ANALYTICS_INFRA_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'analytics-compose.yml', 'down', '-v'],
  workingDir: dockerFolder
}

const PULL_OPEN_DISTRO_CMD: Command = {
  cmd: 'docker',
  args: ['pull', 'ghcr.io/dotcms/elasticsearch:7.9.1'],
  workingDir: dockerFolder,
  env: DEPS_ENV[dbType as keyof DatabaseEnvs]
}

const PULL_DB_CMD: Command = {
  cmd: 'docker',
  args: [
    'pull',
    `${dbType === 'mssql' ? 'ghcr.io/dotcms/mssqlserver:2017-latest' : 'ghcr.io/dotcms/postgres:13-alpine'}`
  ],
  workingDir: dockerFolder,
  env: DEPS_ENV[dbType as keyof DatabaseEnvs]
}

const START_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'up'],
  workingDir: dockerFolder,
  env: DEPS_ENV[dbType as keyof DatabaseEnvs]
}

const STOP_DEPENDENCIES_CMD: Command = {
  cmd: 'docker-compose',
  args: ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'down'],
  workingDir: dockerFolder,
  env: DEPS_ENV[dbType as keyof DatabaseEnvs]
}

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run integration tests.
 *
 * @param cmds resolved command
 * @returns a number representing the command exit code
 */
export const runTests = async (cmds: Command[]): Promise<CommandResult> => {
  // Setup ITs
  await setup.setupTests(propertyMap())

  let idx = 0

  try {
    if (cmds.length > 1) {
      const prepareCmd = cmds[idx++]
      core.info(`
      ===========================
      Preparing integration tests
      ===========================`)
      await execCmd(prepareCmd)
    }

    // Starting dependencies
    core.info(`
    =======================================
    Starting integration tests dependencies
    =======================================`)

    if (includeAnalytics) {
      // Starting analytics infrastructure
      execCmdAsync(START_ANALYTICS_INFRA_CMD)
      await waitFor(160, 'Analytics Infrastructure')
      await warmUpAnalytics()
    }

    await execCmd(PULL_OPEN_DISTRO_CMD)
    await execCmd(PULL_DB_CMD)
    execCmdAsync(START_DEPENDENCIES_CMD)

    await waitFor(resolveWait(), `ES and ${dbType}`)

    // Executes ITs
    const cmd = cmds[idx]
    resolveParams(cmd)
    core.info(`
      ===========================================
      Running integration tests against ${dbType}
      ===========================================`)

    const exitCode = await execCmd(cmd)
    core.info(`
      ===========================================
      Integration tests have finished to run
      ===========================================`)
    return {
      exitCode,
      outputDir,
      reportDir,
      ciIndex
    }
  } catch (err) {
    core.setFailed(`Running integration tests failed due to ${err}`)
    return {
      exitCode: 127,
      outputDir,
      reportDir,
      ciIndex
    }
  } finally {
    await stopDeps()
  }
}

const resolveWait = (): number => {
  return dbType === 'mssql' ? 45 : 30
}

/**
 * Stops dependencies.
 */
const stopDeps = async () => {
  // Stopping dependencies
  core.info(`
    =======================================
    Stopping integration tests dependencies
    =======================================`)

  if (includeAnalytics) {
    try {
      await execCmd(STOP_ANALYTICS_INFRA_CMD)
    } catch (err) {
      console.error(`Error stopping dependencies: ${err}`)
    }
  }

  try {
    await execCmd(STOP_DEPENDENCIES_CMD)
  } catch (err) {
    console.error(`Error stopping dependencies: ${err}`)
  }
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
  const tomcatRoot = path.join(projectRoot, 'dist', 'dotserver', 'tomcat-9.0.60')
  properties.set('systemFelixFolder', path.join(tomcatRoot, 'webapps', 'ROOT', 'WEB-INF', 'felix-system'))
  properties.set('assetsFolder', appendToWorkspace('custom/assets'))
  properties.set('esDataFolder', appendToWorkspace('custom/esdata'))
  properties.set('logsFolder', dotCmsRoot)
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
 * Resolve parameters to produce command arguments
 *
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (cmd: Command) => {
  const tests = core.getInput('tests')?.trim()
  if (!tests) {
    addFallbackTest(cmd.args)
    return
  }

  core.info(`Commit message found: "${tests}"`)
  const resolved: string[] = []

  for (const l of tests.split('\n')) {
    const line = l.trim()
    if (!line.toLowerCase().startsWith(runtTestsPrefix)) {
      continue
    }

    const testLine = line.slice(runtTestsPrefix.length).trim()
    if (buildEnv === 'gradle') {
      for (const test of testLine.split(',')) {
        resolved.push('--tests')
        resolved.push(test.trim())
      }
    } else if (buildEnv === 'maven') {
      const normalized = testLine
        .split(',')
        .map(t => t.trim())
        .join(',')
      resolved.push(`-Dit.test=${normalized}`)
    }
  }

  if (resolved.length === 0) {
    addFallbackTest(resolved)
  }

  cmd.args.push(...resolved)
  core.info(`Resolved params ${cmd.args.join(' ')}`)
}

const addFallbackTest = (tests: string[]) => {
  core.info('No specific integration tests found using MainSuite')
  if (buildEnv === 'gradle') {
    tests.push('-Dtest.single=com.dotcms.MainSuite')
  } else if (buildEnv === 'maven') {
    tests.push('-Dit.test=com.dotcms.MainSuite')
  }
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

const printCmd = (cmd: Command) => {
  let message = `Executing cmd: ${cmd.cmd} ${cmd.args.join(' ')}`
  if (cmd.workingDir) {
    message += `\ncwd: ${cmd.workingDir}`
  }
  if (cmd.env) {
    message += `\nenv: ${JSON.stringify(cmd.env, null, 2)}`
  }
  core.info(message)
}

const execCmd = async (cmd: Command): Promise<number> => {
  printCmd(cmd)
  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir, env: cmd.env})
}

const execCmdAsync = (cmd: Command) => {
  printCmd(cmd)
  //shelljs.exec([cmd.cmd, ...cmd.args].join(' '), {async: true})
  exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir, env: cmd.env})
}

const fetchAccessToken = async (): Promise<Response> => {
  const idpUrl = 'http://localhost:61111/realms/dotcms/protocol/openid-connect/token'
  const authPayload = 'client_id=analytics-customer-customer1&client_secret=testsecret&grant_type=client_credentials'
  core.info(`Sending POST to ${idpUrl} the payload:\n${authPayload}`)

  return fetch(idpUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: authPayload
  })
}

const warmUpIdp = async (): Promise<AccessToken> => {
  await fetchAccessToken()
  waitFor(1000, 'IDP warmup')
  const response = await fetchAccessToken()
  const accessToken = (await response.json()) as AccessToken
  return accessToken
}

const fetchAnalyticsKey = async (accessToken: string): Promise<Response> => {
  const configUrl = 'http://localhost:8088/c/customer1/cluster1/keys'
  core.info(`Sending GET to ${configUrl}`)
  return fetch(configUrl, {
    method: 'GET',
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`
    }
  })
}

const warmUpConfig = async (accessToken: string): Promise<AnalyticsKey> => {
  await fetchAnalyticsKey(accessToken)
  waitFor(1000, 'Config warmup')
  const response = await fetchAnalyticsKey(accessToken)
  const analyticsKey = (await response.json()) as AnalyticsKey
  return analyticsKey
}

const warmUpAnalytics = async () => {
  const accessToken = await warmUpIdp()
  const token = accessToken.access_token
  accessToken.access_token = ''
  core.info(`Got response:\n${JSON.stringify(accessToken, null, 2)}`)

  const analyticsKey = await warmUpConfig(token)
  core.info(`Got response:\n${JSON.stringify(analyticsKey, null, 2)}`)
}
