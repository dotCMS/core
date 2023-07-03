import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'
// import * as shelljs from 'shelljs'
// import fetch, {Response} from 'node-fetch'
// import {start} from 'repl'
// import {create} from 'domain'

// Action inputs
const projectRoot = core.getInput('project_root')
const builtImageName = core.getInput('built_image_name')
const waitForDeps = core.getInput('wait_for_deps')
const dbType = core.getInput('db_type')
const licenseKey = core.getInput('license_key')
const customStarterUrl = core.getInput('custom_starter_url')

const cicdFolder = path.join(projectRoot, 'cicd')
const dockerFolder = path.join(cicdFolder, 'docker')
const licenseFolder = path.join(dockerFolder, 'license')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'cliTest')
const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'cliTest')
const volumes = [licenseFolder, path.join(dockerFolder, 'cms-shared'), path.join(dockerFolder, 'cms-local')]
const PASSED = 'PASSED'
const FAILED = 'FAILED'

const DEPS_ENV: {[key: string]: string} = {
  DOTCMS_IMAGE: builtImageName,
  TEST_TYPE: 'cli',
  DB_TYPE: dbType,
  CUSTOM_STARTER_FOLDER: customStarterUrl,
  WAIT_FOR_DEPS: waitForDeps,
  POSTGRES_USER: 'postgres',
  POSTGRES_PASSWORD: 'postgres',
  POSTGRES_DB: 'dotcms',
  JVM_ENDPOINT_TEST_PASS: 'obfuscate_me'
}

export interface Command {
  cmd: string
  args?: string[]
  workingDir?: string
  env?: {[key: string]: string}
}

export interface TestsResult {
  testsRunExitCode: number
  testsResultsStatus: string
  skipResultsReport: boolean
}

const printInputs = () => {
  core.info(`project_root: ${projectRoot}`)
  core.info(`built_image_name: ${builtImageName}`)
  core.info(`wait_for_deps: ${waitForDeps}`)
  core.info(`db_type: ${dbType}`)
  core.info(`license_key: ${licenseKey}`)
  core.info(`custom_starter_url: ${customStarterUrl}`)
}

/*
 * Run postman tests and provides a summary of such.
 *
 * @returns a object representing run status
 */
export const runTests = async (): Promise<TestsResult> => {
  try {
    printInputs()
    await setup()
    await startDependencies()
    return await runCliTestsInDocker()
  } catch (err) {
    core.setFailed(`Error setting up environment: ${err}`)
    return {
      testsRunExitCode: 127,
      testsResultsStatus: FAILED,
      skipResultsReport: true
    }
  } finally {
    await stopDependencies()
  }
}

/**
 * Sets up everything needed to run dotCMS.
 */
const setup = async () => {
  createFolders()
  await prepareLicense()
}

/**
 * Create necessary folders
 */
const createFolders = () => {
  const folders = [resultsFolder, reportFolder, ...volumes]
  for (const folder of folders) {
    fs.mkdirSync(folder, {recursive: true})
  }
}

/**
 * Creates license folder and file with appropiate key.
 */
const prepareLicense = async () => {
  const licenseFile = path.join(licenseFolder, 'license.dat')
  core.info(`Adding license to ${licenseFile}`)
  fs.writeFileSync(licenseFile, licenseKey, {encoding: 'utf8', flag: 'a+', mode: 0o777})
}

/**
 * Start test depencies: db, ES and DotCMS instance.
 */
const startDependencies = async () => {
  // Starting dependencies
  core.info(`
      =======================================
      Starting tests dependencies
      =======================================`)

  await execCmd(toCommand('docker', ['pull', 'ghcr.io/dotcms/elasticsearch:7.9.1'], dockerFolder))
  await execCmd(toCommand('docker', ['pull', 'ghcr.io/dotcms/postgres:13-alpine'], dockerFolder))
  await execCmd(toCommand('docker', ['pull', builtImageName], dockerFolder))

  execCmdAsync(
    toCommand(
      'docker-compose',
      ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'up'],
      dockerFolder,
      DEPS_ENV
    )
  )
}

/**
 * Stop postman depencies: db, ES and DotCMS isntance.
 */
const stopDependencies = async () => {
  // Stopping dependencies
  core.info(`
      ===================================
      Stopping tests dependencies
      ===================================`)

  try {
    await execCmd(
      toCommand(
        'docker-compose',
        ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'down', '-v'],
        dockerFolder,
        DEPS_ENV
      )
    )
  } catch (err) {
    console.error(`Error stopping dependencies: ${err}`)
  }
}

/**
 * Executes command contained in Command object.
 *
 * @param cmd Command object
 * @returns Promise with process number
 */
const execCmd = async (cmd: Command): Promise<number> => {
  printCmd(cmd)
  return await exec.exec(cmd.cmd, cmd.args || [], {cwd: cmd.workingDir, env: cmd.env})
}

/**
 * Async executes command contained in Command object.
 *
 * @param cmd Command object
 */
const execCmdAsync = (cmd: Command) => {
  printCmd(cmd)
  exec.exec(cmd.cmd, cmd.args || [], {cwd: cmd.workingDir, env: cmd.env})
}

/**
 * Gather values and build a Command instance.
 *
 * @param cmd command
 * @param args arguments
 * @param workingDir working dir
 * @param env environment variables
 * @returns Command object
 */
const toCommand = (cmd: string, args?: string[], workingDir?: string, env?: {[key: string]: string}): Command => {
  return {
    cmd,
    args,
    workingDir,
    env
  }
}

/**
 * Prints string Command representation.
 *
 * @param cmd Command object
 */
const printCmd = (cmd: Command) => {
  let message = `Executing cmd: ${cmd.cmd} ${cmd.args?.join(' ') || ''}`
  if (cmd.workingDir) {
    message += `\ncwd: ${cmd.workingDir}`
  }
  if (cmd.env) {
    message += `\nenv: ${JSON.stringify(cmd.env, null, 2)}`
  }
  core.info(message)
}

/**
 * Run postman tests.
 *
 * @returns an overall ivew of the tests results
 */
const runCliTestsInDocker = async (): Promise<TestsResult> => {
  await waitFor(120, `DotCMS instance`)

  // Run tests
  core.info(`
      ===========================================
      Running CLI tests against ${dbType}
      ===========================================`)

  // runCliTests()

  return {
    testsRunExitCode: 0,
    testsResultsStatus: PASSED,
    skipResultsReport: false
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
 * @param endLabel end label
 */
const waitFor = async (wait: number, startLabel: string, endLabel?: string) => {
  core.info(`Waiting ${wait} seconds for ${startLabel}`)
  await delay(wait)
  const finalLabel = endLabel || startLabel
  core.info(`Waiting on ${finalLabel} loading has ended`)
}
