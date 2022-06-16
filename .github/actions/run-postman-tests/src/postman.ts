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
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const logsFolder = path.join(dockerFolder, 'logs')
const logFile = 'dotcms.log'
const volumes = [licenseFolder, path.join(dockerFolder, 'cms-shared'), path.join(dockerFolder, 'cms-local'), logsFolder]
const postmanTestsPath = path.join(dotCmsRoot, 'src', 'curl-test')
const postmanEnvFile = 'postman_environment.json'
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

/*
 *
 * @returns a number representing the command exit code
 */
export const runTests = async (): Promise<PostmanTestsResult> => {
  setup()
  startDeps()

  try {
    return await runPostmanTests()
    copyOutputs()
  } catch (err) {
    throw err
  } finally {
    await stopDeps()
  }
}

/**
 * Copies logs from docker volume to standard DotCMS location.
 */
const copyOutputs = async () => {
  await exec.exec('pwd', [], {cwd: logsFolder})
  await exec.exec('ls', ['-las', '.'], {cwd: logsFolder})

  try {
    fs.copyFileSync(path.join(logsFolder, logFile), path.join(dotCmsRoot, logFile))
  } catch (err) {
    core.warning(`Error copying log file: ${err}`)
  }
}

/**
 * Sets up everuthing needed to run postman collections.
 */
const setup = () => {
  installDeps()
  createFolders()
  prepareLicense()
}

/**
 * Install necessary dependencies to run the postman collections.
 */
const installDeps = async () => {
  core.info('Installing newman')
  const npmArgs = ['install', '--location=global', 'newman']
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

/**
 * Start postman depencies: db, ES and DotCMS isntance.
 */
const startDeps = async () => {
  // Starting dependencies
  core.info(`
    =======================================
    Starting postman tests dependencies
    =======================================`)
  // const depProcess = she
  exec.exec(
    'docker-compose',
    ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'up'],
    {
      cwd: dockerFolder,
      env: DEPS_ENV
    }
  )

  //await startDotCMS()
}

/**
 * Stop postman depencies: db, ES and DotCMS isntance.
 */
const stopDeps = async () => {
  //await stopDotCMS()
  // Stopping dependencies
  core.info(`
    ===================================
    Stopping postman tests dependencies
    ===================================`)
  try {
    await exec.exec(
      'docker-compose',
      ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'down'],
      {
        cwd: dockerFolder,
        env: DEPS_ENV
      }
    )
  } catch (err) {
    console.error(`Error stopping dependencies: ${err}`)
  }
}

// const startDotCMS = async () => {
//   core.info(`
//     =======================================
//     Starting DotCMS instance
//     =======================================`)
//   exec.exec(path.join(tomcatRoot, 'bin', 'startup.sh'))
// }

// const stopDotCMS = async () => {
//   core.info(`
//     =======================================
//     Stopping DotCMS instance
//     =======================================`)
//   await exec.exec(path.join(tomcatRoot, 'bin', 'shutdown.sh'))
// }

/**
 * Run postman tests.
 *
 * @returns an overall ivew of the tests results
 */
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
      const returnCode = await runPostmanCollection(collection)
      collectionRuns.set(collection, returnCode)
    } catch (err) {
      core.error(`Postman collection run for ${collection} failed due to: ${err}`)
      collectionRuns.set(collection, 127)
    }
    core.info(`Results files so far at ${resultsFolder}:\n${shelljs.ls(resultsFolder).join(',')}`)
  }

  return handleResults(collectionRuns)
}

/**
 * Run a postman collection.
 *
 * @param collection postman collection
 * @returns promise with process return code
 */
const runPostmanCollection = async (collection: string): Promise<number> => {
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

/*
 *
 * @param collectionRuns collection tests results map
 * @returns an overall ivew of the tests results
 */
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

/**
 * Create necessary folders
 */
const createFolders = () => {
  const folders = [resultsFolder, reportFolder, ...volumes]
  for (const folder of folders) {
    fs.mkdirSync(folder, {recursive: true})
  }

  shelljs.touch(path.join(dockerFolder, logFile))
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
