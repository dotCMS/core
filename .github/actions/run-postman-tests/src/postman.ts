import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import {ChildProcess} from 'node:child_process'
import * as path from 'path'
import * as shelljs from 'shelljs'

const projectRoot = core.getInput('project_root')
const buildEnv = core.getInput('build_env')
const builtImageName = core.getInput('built_image_name')
const dbType = core.getInput('db_type')
const licenseKey = core.getInput('license_key')
const customStarterUrl = core.getInput('custom_starter_url')
const tests = core.getInput('tests')
const exportReport = core.getBooleanInput('export_report')
const cicdFolder = path.join(projectRoot, 'cicd')
const resourcesFolder = path.join(cicdFolder, 'resources', 'postman')
const dockerFolder = path.join(cicdFolder, 'docker')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const tomcatFolder = core.getInput('tomcat_folder')
const tomcatRoot = path.join(projectRoot, 'dist', 'dotserver', tomcatFolder)
const logsFolder = path.join(tomcatRoot, 'logs')
const tomcatLogFile = path.join(logsFolder, 'catalina.out')
const logFile = path.join(logsFolder, 'dotcms.log')
const licenseFolder = path.join(tomcatRoot, 'webapps', 'ROOT', 'dotsecure', 'license')
const postmanTestsPath = path.join(dotCmsRoot, 'src', 'curl-test')
const postmanEnvFile = 'postman_environment.json'
const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')
const runtTestsPrefix = 'postman-tests:'
const PASSED = 'PASSED'
const FAILED = 'FAILED'
let dotCmsProcess: ChildProcess
let logProcess: ChildProcess

export interface PostmanTestsResult {
  testsRunExitCode: number
  testsResultsStatus: string
  skipResultsReport: boolean
}

export interface Command {
  cmd: string
  args?: string[]
  workingDir?: string
  env?: {[key: string]: string}
}

const DEPS_ENV: {[key: string]: string} = {
  DOTCMS_IMAGE: builtImageName,
  TEST_TYPE: 'postman',
  DB_TYPE: dbType,
  CUUSTOM_STARTER_FOLDER: customStarterUrl,
  POSTGRES_USER: 'dotcms',
  POSTGRES_PASSWORD: 'dotcms',
  POSTGRES_DB: 'dotcms'
}

const DOTCMS_ENV: {[key: string]: string} = {
  databaseType: dbType,
  CATALINA_OPTS: '-XX:+PrintFlagsFinal',
  DOT_ES_ENDPOINTS: 'https://localhost:9200',
  DOT_DOTCMS_DEV_MODE: 'true',
  DB_MAX_TOTAL: '15',
  DOT_INDEX_POLICY_SINGLE_CONTENT: 'FORCE',
  DOT_ASYNC_REINDEX_COMMIT_LISTENERS: 'false',
  DOT_ASYNC_COMMIT_LISTENERS: 'false',
  DOT_CACHE_GRAPHQLQUERYCACHE_SECONDS: '600'
}

/*
 * Run postman tests.
 *
 * @returns a number representing the command exit code
 */
export const runTests = async (): Promise<PostmanTestsResult> => {
  setup()

  startDeps()

  try {
    return await runPostmanCollections()
  } catch (err) {
    core.setFailed(`Postman tests faiuled due to: ${err}`)
    return {
      testsRunExitCode: 127,
      testsResultsStatus: FAILED,
      skipResultsReport: !fs.existsSync(reportFolder)
    }
  } finally {
    copyOutputs()
    await stopDeps()
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
  await execCmd(toCommand('npm', npmArgs))

  if (!fs.existsSync(tomcatRoot) && buildEnv === 'gradle') {
    core.info(`Tomcat root ${tomcatRoot} does not exist, creating it`)
    await execCmd(toCommand('./gradlew', ['clonePullTomcatDist']))
    if (!tomcatRoot) {
      throw new Error('Cannot find any Tomcat root folder')
    }
  }
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
  execCmdAsync(
    toCommand(
      'docker-compose',
      ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'up'],
      dockerFolder,
      DEPS_ENV
    ),
    false
  )

  await waitFor(70, 'DotCMS dependencies')

  startDotCMS()
}

/**
 * Stop postman depencies: db, ES and DotCMS isntance.
 */
const stopDeps = async () => {
  await stopDotCMS()

  // Stopping dependencies
  core.info(`
    ===================================
    Stopping postman tests dependencies
    ===================================`)
  try {
    await execCmd(
      toCommand(
        'docker-compose',
        ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, 'down'],
        dockerFolder,
        DEPS_ENV
      )
    )
  } catch (err) {
    console.error(`Could not stop dependencies gracefully due to: ${err}`)
  }
}

const startDotCMS = () => {
  core.info(`
    =======================================
    Starting DotCMS instance
    =======================================`)
  dotCmsProcess = execCmdAsync(
    toCommand(path.join(tomcatRoot, 'bin', 'startup.sh'), [], tomcatRoot, DOTCMS_ENV),
    true
  ) as ChildProcess
  logProcess = execCmdAsync(toCommand('tail', ['-f', tomcatLogFile]), true) as ChildProcess
  core.info(`Log process: ${logProcess}`)
}

const stopDotCMS = async () => {
  core.info(`
    =======================================
    Stopping DotCMS instance
    =======================================`)
  try {
    await execCmd(toCommand(path.join(tomcatRoot, 'bin', 'shutdown.sh'), [], tomcatRoot, DOTCMS_ENV))
  } catch (err) {
    core.warning(`Could not stop gracefully DotCMS due to: ${err}`)
  } finally {
    tryToKill(dotCmsProcess, 'DotCMS to stop')
    tryToKill(logProcess, 'DotCMS log to stop')
  }
}

/**
 * Run postman tests.
 *
 * @returns an overall ivew of the tests results
 */
const runPostmanCollections = async (): Promise<PostmanTestsResult> => {
  await waitFor(160, `DotCMS instance`)

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

  const htmlResults: string[] = []
  const header = fs.readFileSync(path.join(resourcesFolder, 'postman-results-header.html'), {
    encoding: 'utf8',
    flag: 'r'
  })
  const footer = fs.readFileSync(path.join(resourcesFolder, 'postman-results-footer.html'), {
    encoding: 'utf8',
    flag: 'r'
  })
  const collectionRuns = new Map<string, number>()

  for (const collection of filtered) {
    const normalized = collection.replace(/ /g, '_').replace('.json', '')
    let rc: number
    const start = new Date().getTime()

    try {
      rc = await runPostmanCollection(collection, normalized)
    } catch (err) {
      core.info(`Postman collection run for ${collection} failed due to: ${err}`)
      rc = 127
    }

    const end = new Date().getTime()
    const duration = (end - start) / 1000
    core.info(`Collection ${collection} took ${duration} seconds to run`)

    collectionRuns.set(collection, rc)

    if (exportReport) {
      const passed = rc === 0
      htmlResults.push(
        `<tr><td><a href="./${normalized}.html">${collection}</a></td><td style="color: #ffffff; background-color: ${
          passed ? '#28a745' : '#dc3545'
        }; font-weight: bold;">${passed ? PASSED : FAILED}</td>
        <td>${duration} seconds</td>
        </tr>`
      )
    }
  }

  if (exportReport) {
    const contents = [header, ...htmlResults, footer]
    fs.writeFileSync(path.join(reportFolder, 'index.html'), `${contents.join('\n')}`, {
      encoding: 'utf8',
      flag: 'a+',
      mode: 0o666
    })
  }

  return handleResults(collectionRuns)
}

/**
 * Run a postman collection.
 *
 * @param collection postman collection
 * @param normalized normalized collection
 * @returns promise with process return code
 */
const runPostmanCollection = async (collection: string, normalized: string): Promise<number> => {
  core.info(`Running Postman collection: ${collection}`)
  const resultFile = path.join(resultsFolder, `${normalized}.xml`)
  const page = `${normalized}.html`
  const reportFile = path.join(reportFolder, page)
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
  const rc = await execCmd(toCommand('newman', args, postmanTestsPath))

  return rc
}

/*
 * Process results.
 *
 * @param collectionRuns collection tests results map
 * @returns an overall ivew of the tests results
 */
const handleResults = (collectionRuns: Map<string, number>): PostmanTestsResult => {
  core.info(`Postman collection results:`)
  for (const collection of collectionRuns.keys()) {
    core.info(`"${collection}" -> ${collectionRuns.get(collection)}`)
  }

  let collectionFailed = false
  for (const collection of collectionRuns.keys()) {
    if (collectionRuns.get(collection) !== 0) {
      collectionFailed = true
      break
    }
  }

  return {
    testsRunExitCode: collectionFailed ? 1 : 0,
    testsResultsStatus: collectionFailed ? FAILED : PASSED,
    skipResultsReport: !fs.existsSync(reportFolder)
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
  shelljs.touch(tomcatLogFile)

  const folders = [resultsFolder, reportFolder, licenseFolder]
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
  await execCmd(toCommand('ls', ['-las', licenseFile]))
}

/**
 * Resolves tests when provided
 *
 * @returns array of tring representing postman collections
 */
const resolveSpecific = (): string[] => {
  const extracted = extractFromMessg(tests)

  if (extracted.length === 0) {
    core.info('No specific postman tests found')
    return []
  }

  const resolved: string[] = []
  for (const line of extracted) {
    const testLine = line.slice(runtTestsPrefix.length).trim()
    for (const collection of testLine.trim().split(',')) {
      const trimmed = collection.trim()
      const normalized = trimmed.endsWith('.json') ? trimmed : `${trimmed}.json`
      resolved.push(normalized)
    }
  }

  core.info(`Resolved specific collections:\n${resolved.join(',')}`)
  return resolved
}

const extractFromMessg = (message: string): string[] => {
  if (!message) {
    return []
  }

  const extracted: string[] = []
  for (const l of tests.split('\n')) {
    const trimmed = l.trim()
    const line = trimmed.toLocaleLowerCase()
    if (line.startsWith(runtTestsPrefix)) {
      extracted.push(trimmed)
    }
  }

  return extracted
}

const toCommand = (cmd: string, args?: string[], workingDir?: string, env?: {[key: string]: string}): Command => {
  return {
    cmd,
    args,
    workingDir,
    env
  }
}

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

const execCmd = async (cmd: Command): Promise<number> => {
  printCmd(cmd)
  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir, env: cmd.env})
}

const execCmdAsync = (cmd: Command, useChild: boolean): ChildProcess | void => {
  printCmd(cmd)

  if (cmd.env) {
    for (const env of Object.keys(cmd.env)) {
      shelljs.env[env] = cmd.env[env]
    }
  }

  const args = cmd.args || []
  if (useChild) {
    const cmdStr = [cmd.cmd, ...args].join(' ')
    const process = shelljs.exec(cmdStr, {async: true})
    core.info(`Creating process from '${cmdStr}': ${process.pid}`)
    return process
  }

  exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir, env: cmd.env})
}

const killProcess = (process?: ChildProcess, sig?: number) => {
  if (process) {
    core.info(`Killing process: ${process.pid}`)
    sig ? process.kill(sig) : process.kill()
  } else {
    core.info('No process found to kill')
  }
}

const tryToKill = async (process: ChildProcess, label: string) => {
  if (!process.killed) {
    killProcess(process)

    await waitFor(20, label)

    if (!process.killed) {
      killProcess(process, 9)
    }
  }
}

/**
 * Copies logs from docker volume to standard DotCMS location.
 */
const copyOutputs = async () => {
  try {
    fs.copyFileSync(tomcatLogFile, logFile)
  } catch (err) {
    core.error(`Error copying log file: ${err}`)
  }
}
