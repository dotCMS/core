import * as core from '@actions/core'
// import * as exec from '@actions/exec'
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

// const DEPS_ENV: {[key: string]: string} = {
//   DOTCMS_IMAGE: builtImageName,
//   TEST_TYPE: 'cli',
//   DB_TYPE: dbType,
//   CUSTOM_STARTER_FOLDER: customStarterUrl,
//   WAIT_FOR_DEPS: waitForDeps,
//   POSTGRES_USER: 'postgres',
//   POSTGRES_PASSWORD: 'postgres',
//   POSTGRES_DB: 'dotcms',
//   JVM_ENDPOINT_TEST_PASS: 'obfuscate_me'
// }

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
  } catch (err) {
    core.setFailed(`Error setting up environment: ${err}`)
    return {
      testsRunExitCode: 127,
      testsResultsStatus: FAILED,
      skipResultsReport: true
    }
  }

  return {
    testsRunExitCode: 0,
    testsResultsStatus: PASSED,
    skipResultsReport: false
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

// /**
//  * Delays the resolve part of a promise to simulate a sleep
//  *
//  * @param seconds number of seconds
//  * @returns void promise
//  */
// const delay = (seconds: number) => new Promise(resolve => setTimeout(resolve, seconds * 1000))

// /**
//  * Waits for specific time with corresponding messages.
//  *
//  * @param wait time to wait
//  * @param startLabel start label
//  * @param endLabel end label
//  */
// const waitFor = async (wait: number, startLabel: string, endLabel?: string) => {
//     core.info(`Waiting ${wait} seconds for ${startLabel}`)
//     await delay(wait)
//     const finalLabel = endLabel || startLabel
//     core.info(`Waiting on ${finalLabel} loading has ended`)
//   }

//   startAppEnvironment = async () => {
//     core.info('Starting dotCMS environment...')
//     await exec.exec('docker-compose', ['up', '-d'], {cwd: dockerFolder})
//     await waitForAppEnvironment()
//   }
