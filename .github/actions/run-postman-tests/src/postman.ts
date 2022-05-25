import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'
import * as shelljs from 'shelljs'

// const resolveTomcat = (): string => {
//   const dotServerFolder = path.join(projectRoot, 'dist', 'dotserver')
//   const tomcatFolder = shelljs.ls(dotServerFolder).find(folder => folder.startsWith('tomcat-'))
//   return path.join(dotServerFolder, tomcatFolder || '')
// }

const projectRoot = core.getInput('project_root')
// const buildEnv = core.getInput('build_env')
const builtImageName = core.getInput('built_image_name')
const waitForDeps = core.getInput('wait_for_deps')
const dbType = core.getInput('db_type')
const licenseKey = core.getInput('license_key')
const customStarterUrl = core.getInput('custom_starter_url')
const tests = core.getInput('tests')
const exportReport = core.getBooleanInput('export_report')
const dockerFolder = path.join(projectRoot, 'cicd', 'docker')
const licenseFolder = path.join(dockerFolder, 'license')
const volumes = [licenseFolder, path.join(dockerFolder, 'cms-shared'), path.join(dockerFolder, 'cms-local')]
const postmanTestsPath = path.join(projectRoot, 'dotCMS', 'src', 'curl-test')
const postmanEnvFile = 'postman_environment.json'
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')
const runtTestsPrefix = 'postman-tests:'
//let tomcatRoot = resolveTomcat()

export interface PostmanTestsResult {
  testsRunExitCode: number
  testsResultsStatus: string
  skipResultsReport: boolean
}

const DEPS_ENV: {[key: string]: string} = {
  DOTCMS_IMAGE: builtImageName,
  TEST_TYPE: 'postman',
  DB_TYPE: dbType,
  CUUSTOM_STARTER_FOLDER: customStarterUrl,
  WAIT_FOR_DEPS: waitForDeps,
  POSTGRES_USER: 'postgres',
  POSTGRES_PASSWORD: 'postgres',
  POSTGRES_DB: 'dotcms'
}

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run integration tests.
 *
 * @returns a number representing the command exit code
 */
export const runTests = async (): Promise<PostmanTestsResult> => {
  setup()
  await startDeps()
  await startDotCMS()
  const results = await runPostmanTests()
  await stopDotCMS()
  await stopDeps()
  return results
}

const setup = () => {
  installDeps()
  createFolders()
  prepareLicense()
}

const installDeps = async () => {
  core.info('Installing newman')
  const npmArgs = ['install', '-g', 'newman']
  if (exportReport) {
    npmArgs.push('newman-reporter-htmlextra')
  }
  await exec.exec('npm', npmArgs)

  // if (!fs.existsSync(tomcatRoot) && buildEnv === 'gradle') {
  //   core.info(`Tomcat root does not exist, creating it`)
  //   await exec.exec('./gradlew', ['clonePullTomcatDist'])

  //   tomcatRoot = resolveTomcat()
  //   if (!tomcatRoot) {
  //     throw new Error('Cannot find any Tomcat root folder')
  //   }
  // }
}

const startDeps = async () => {
  // Starting dependencies
  core.info(`
    =======================================
    Starting postman tests dependencies
    =======================================`)
  // const depProcess = she
  exec.exec(
    'docker-compose',
    [
      '-f',
      'open-distro-compose.yml',
      '-f',
      `${dbType}-compose.yml`,
      '-f',
      'dotcms-compose.yml',
      'up',
      '--abort-on-container-exit'
    ],
    {
      cwd: dockerFolder,
      env: DEPS_ENV
    }
  )

  await waitFor(30, `ES, ${dbType} and DotCMS instance`)
}

const stopDeps = async () => {
  // Stopping dependencies
  core.info(`
    =======================================
    Stopping postman tests dependencies
    =======================================`)
  // const depProcess = she
  await exec.exec(
    'docker-compose',
    ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'down'],
    {
      cwd: dockerFolder,
      env: DEPS_ENV
    }
  )
}

const startDotCMS = async () => {
  core.info(`
    =======================================
    Starting DotCMS instance
    =======================================`)
  //exec.exec(path.join(tomcatRoot, 'bin', 'startup.sh'))
}

const stopDotCMS = async () => {
  core.info(`
    =======================================
    Stopping DotCMS instance
    =======================================`)
  //await exec.exec(path.join(tomcatRoot, 'bin', 'shutdown.sh'))
}

const runPostmanTests = async (): Promise<PostmanTestsResult> => {
  await waitFor(150, `DotCMS instance`)

  // Executes Postman tests
  core.info(`
    ===========================================
    Running postman tests against ${dbType}
    ===========================================`)
  const foundCollections = shelljs
    .ls(postmanTestsPath)
    .filter(file => file.endsWith('.json') && file !== postmanEnvFile)
  core.info(`Postman collections:\n${foundCollections.join(',')}`)

  const resolvedTests = resolveSpecific()
  const filtered =
    resolvedTests.length === 0
      ? foundCollections
      : resolvedTests.filter(resolved => !!foundCollections.find(collection => collection === resolved))
  core.info(`Detected Postman collections:\n${filtered.join(',')}`)

  const collectionRuns = new Map<string, number>()
  for (const collection of filtered) {
    try {
      const returnCode = await runPostmanTest(collection)
      collectionRuns.set(collection, returnCode)
    } catch (err) {
      core.error(`Postman collection run for ${collection} failed due to: ${err}`)
      collectionRuns.set(collection, 127)
    }
    core.info(`Results files so far at ${resultsFolder}:\n${shelljs.ls(resultsFolder).join(',')}`)
  }

  return handleResults(collectionRuns)
}

const runPostmanTest = async (collection: string): Promise<number> => {
  core.info(`Running Postman test: ${collection}`)
  const normalized = collection.replace(/ /g, '_').replace('.json', '')
  const resultFile = path.join(resultsFolder, `${normalized}.xml`)
  const reportFile = path.join(reportFolder, `${normalized}.html`)
  const reporters = ['cli', 'junit']
  if (exportReport) {
    reporters.push('htmlextra')
  }
  const args = [
    'run',
    collection,
    '-e',
    postmanEnvFile,
    '--reporters',
    reporters.join(','),
    '--reporter-junit-export',
    resultFile
  ]
  if (exportReport) {
    args.push('--reporter-htmlextra-export')
    args.push(reportFile)
  }
  return await exec.exec('newman', args, {
    cwd: postmanTestsPath
  })
}

const handleResults = (collectionRuns: Map<string, number>): PostmanTestsResult => {
  let collectionFailed = true
  for (const collection in collectionRuns.keys) {
    if (collectionRuns.get(collection) !== 0) {
      collectionFailed = true
      break
    }
  }

  return {
    testsRunExitCode: collectionFailed ? 1 : 0,
    testsResultsStatus: collectionFailed ? 'PASSED' : 'FAILED',
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
 * @param endLabel endlabel
 */
const waitFor = async (wait: number, startLabel: string, endLabel?: string) => {
  core.info(`Waiting ${wait} seconds for ${startLabel}`)
  await delay(wait)
  const finalLabel = endLabel || startLabel
  core.info(`Waiting on ${finalLabel} loading has ended`)
}

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
  await exec.exec('ls', ['-las', licenseFile])
}

/**
 * Resolves tests when provided
 *
 * @returns array of tring representing postman collections
 */
const resolveSpecific = (): string[] => {
  if (!tests) {
    core.info('No specific postman tests found')
    return []
  }

  const resolved: string[] = []
  for (const l of tests.split('\n')) {
    const line = l.trim()
    if (!line.toLowerCase().startsWith(runtTestsPrefix)) {
      continue
    }

    const testLine = line.slice(runtTestsPrefix.length).trim()
    for (const collection of testLine.trim().split(',')) {
      const trimmed = collection.trim()
      const normalized = trimmed.endsWith('.json') ? trimmed : `${trimmed}.json`
      resolved.push(normalized)
    }
  }

  core.info(`Resolved specific collections ${resolved.join(',')}`)
  return resolved
}
