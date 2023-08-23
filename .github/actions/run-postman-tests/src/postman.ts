import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'
import * as shelljs from 'shelljs'
import fetch, {Response} from 'node-fetch'

const projectRoot = core.getInput('project_root')
const builtImageName = core.getInput('built_image_name')
const waitForDeps = core.getInput('wait_for_deps')
const dbType = core.getInput('db_type')
const licenseKey = core.getInput('license_key')
const customStarterUrl = core.getInput('custom_starter_url')
const parallelCollections: ParallelCollections[] = JSON.parse(core.getInput('parallel_collections'))
const parallelCollection = core.getInput('parallel_collection')
const tests = core.getInput('tests')
const exportReport = core.getBooleanInput('export_report')
const includeAnalytics = core.getBooleanInput('include_analytics')
const cicdFolder = path.join(projectRoot, 'cicd')
const resourcesFolder = path.join(cicdFolder, 'resources', 'postman')
const dockerFolder = path.join(cicdFolder, 'docker')
const licenseFolder = path.join(dockerFolder, 'license')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const volumes = [licenseFolder, path.join(dockerFolder, 'cms-shared'), path.join(dockerFolder, 'cms-local')]
const postmanTestsPath = path.join(dotCmsRoot, 'src', 'curl-test')
const postmanEnvFile = 'postman_environment.json'
const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')
const runtTestsPrefix = 'postman-tests:'
const PASSED = 'PASSED'
const FAILED = 'FAILED'

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

interface ParallelCollections {
  name: string
  collections: string[]
}

const DEPS_ENV: {[key: string]: string} = {
  DOTCMS_IMAGE: builtImageName,
  TEST_TYPE: 'postman',
  DB_TYPE: dbType,
  CUSTOM_STARTER_FOLDER: customStarterUrl,
  WAIT_FOR_DEPS: waitForDeps,
  POSTGRES_USER: 'postgres',
  POSTGRES_PASSWORD: 'postgres',
  POSTGRES_DB: 'dotcms',
  JVM_ENDPOINT_TEST_PASS: 'obfuscate_me'
}

/*
 * Run postman tests and provides a summary of such.
 *
 * @returns a object representing run status
 */
export const runTests = async (): Promise<PostmanTestsResult> => {
  await setup()
  await startDeps()

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
    await copyOutputs()
    await stopDeps()
  }
}

/**
 * Copies logs from docker volume to standard DotCMS location.
 */
const copyOutputs = async () => {
  const logFile = path.join(dotCmsRoot, `${normalize(parallelCollection)}.log`)
  await execCmd(
    toCommand('docker', ['cp', 'docker_dotcms-app_1:/srv/dotserver/tomcat-9.0.60/logs/dotcms.log', logFile])
  )
}

const copyHeaderAndFooter = async () => {
  await execCmd(toCommand('cp', [path.join(resourcesFolder, 'postman-results-header.html'), reportFolder]))
  await execCmd(toCommand('cp', [path.join(resourcesFolder, 'postman-results-footer.html'), reportFolder]))
}

/**
 * Sets up everuthing needed to run postman collections.
 */
const setup = async () => {
  await installDeps()
  createFolders()
  await prepareLicense()
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

  if (includeAnalytics) {
    execCmdAsync(toCommand('docker-compose', ['-f', 'analytics-compose.yml', 'up'], dockerFolder))
    await waitFor(160, 'Analytics Infrastructure')
    await warmUpAnalytics()
  }

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
const stopDeps = async () => {
  // Stopping dependencies
  core.info(`
    ===================================
    Stopping postman tests dependencies
    ===================================`)
  if (includeAnalytics) {
    try {
      await execCmd(toCommand('docker-compose', ['-f', 'analytics-compose.yml', 'down', '-v'], dockerFolder))
    } catch (err) {
      console.error(`Error stopping dependencies: ${err}`)
    }
  }

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

const normalize = (provided: string): string => {
  return !provided || provided === '*' ? 'postman' : provided.replace(/ /g, '_').replace('.json', '')
}

/**
 * Run postman tests.
 *
 * @returns an overall ivew of the tests results
 */
const runPostmanCollections = async (): Promise<PostmanTestsResult> => {
  await waitFor(120, `DotCMS instance`)

  // Executes Postman tests
  core.info(`
    ===========================================
    Running postman tests against ${dbType}
    ===========================================`)
  const collectionRuns = new Map<string, number>()
  const foundCollections = shelljs
    .ls(postmanTestsPath)
    .filter(file => file.endsWith('.json') && file !== postmanEnvFile)
  core.info(`Current parallel collection: ${parallelCollection}`)
  const resolved = resolveCollections(foundCollections)
  core.info(`Resolved collections ${resolved.join(', ')}`)
  const htmlResults: string[] = []
  const normalizedParallel = normalize(parallelCollection)

  for (const collection of resolved) {
    const normalized = normalize(collection)
    let rc: number
    const start = new Date().getTime()

    try {
      rc = await runPostmanCollection(collection, normalized)
    } catch (err) {
      core.info(`Postman collection run for ${collection} failed due to: ${err}`)
      rc = 127
    }

    fs.writeFileSync(path.join(reportFolder, `${normalized}.rc`), `test_results_rc=${rc}`, {
      encoding: 'utf8',
      flag: 'a+',
      mode: 0o666
    })

    const end = new Date().getTime()
    const duration = (end - start) / 1000
    core.info(`Collection ${collection} took ${duration} seconds to run`)

    collectionRuns.set(collection, rc)

    if (exportReport) {
      const passed = rc === 0

      const content = `<tr><td><a href="./${normalized}.html">${collection}</a></td><td style="color: #ffffff; background-color: ${
        passed ? '#28a745' : '#dc3545'
      }; font-weight: bold;">${passed ? PASSED : FAILED}</td>
      <td>${duration}</td></tr>`
      htmlResults.push(content)

      fs.writeFileSync(path.join(reportFolder, `${normalized}.inc`), content, {
        encoding: 'utf8',
        flag: 'a+',
        mode: 0o666
      })

      core.setOutput('normalized_parallel_collection', normalizedParallel)
    }
  }

  if (exportReport) {
    const header = fs.readFileSync(path.join(resourcesFolder, 'postman-results-header.html'), {
      encoding: 'utf8',
      flag: 'r'
    })
    const footer = fs.readFileSync(path.join(resourcesFolder, 'postman-results-footer.html'), {
      encoding: 'utf8',
      flag: 'r'
    })
    const contents = [header, ...htmlResults, footer]
    fs.writeFileSync(path.join(reportFolder, `${normalizedParallel}.html`), `${contents.join('\n')}`, {
      encoding: 'utf8',
      flag: 'a+',
      mode: 0o666
    })
  }

  await copyHeaderAndFooter()

  return handleResults(collectionRuns)
}

const resolveCollections = (foundCollections: string[]): string[] => {
  const parallel = parallelCollection?.trim() || '*'
  const all = parallel === '*'
  const resolved = (
    all ? resolveSpecific() : parallelCollections.find(pc => pc.name === parallel)?.collections || []
  ).map(collection => trimAndExt(collection))

  if (resolved.length > 0) {
    return resolved.filter(collection => foundCollections.includes(collection))
  }

  if (all) {
    const allParallel = parallelCollections.flatMap(pc => pc.collections).map(collection => trimAndExt(collection))
    return foundCollections.filter(collection => !allParallel.includes(collection))
  }

  return all ? foundCollections : []
}

/**
 * Run a postman collection.
 *
 * @param collection postman collection
 * @param normalized normalized collection
 * @returns promise with process return code
 */
const runPostmanCollection = async (collection: string, normalized: string): Promise<number> => {
  core.info(`Running Postman collection: "${collection}"`)
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

  return await execCmd(toCommand('newman', args, postmanTestsPath))
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
 * @param endLabel end label
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
}

/**
 * Creates license folder and file with appropiate key.
 */
const prepareLicense = async () => {
  const licenseFile = path.join(licenseFolder, 'license.dat')
  core.info(`Adding license to ${licenseFile}`)
  fs.writeFileSync(licenseFile, licenseKey, {encoding: 'utf8', flag: 'a+', mode: 0o777})
  //await execCmd(toCommand('ls', ['-las', licenseFile]))
}

/**
 * Resolves tests when provided
 *
 * @returns array of tring representing postman collections
 */
const resolveSpecific = (): string[] => {
  const extracted = extractFromMessg(tests)
  core.info(`Extracted from message: [${extracted.join(', ')}]`)

  if (extracted.length === 0) {
    core.info('No specific postman tests found, returning empty')
    return []
  }

  const resolved: string[] = []
  for (const line of extracted) {
    const testLine = line.slice(runtTestsPrefix.length).trim()
    for (const collection of testLine.trim().split(',')) {
      resolved.push(trimAndExt(collection))
    }
  }

  return resolved
}

const trimAndExt = (collection: string) => {
  const trimmed = collection.trim()
  return trimmed.endsWith('.json') ? trimmed : `${trimmed}.json`
}

/**
 * Extracts postman tests from commit message.
 *
 * @param message commit message
 * @returns filtered tests
 */
const extractFromMessg = (message: string): string[] => {
  if (!message) {
    return []
  }

  const extracted: string[] = []
  for (const l of tests.split('\n')) {
    const trimmed = l.trim()
    const line = trimmed.toLowerCase()
    if (line.startsWith(runtTestsPrefix)) {
      extracted.push(trimmed)
    }
  }

  return extracted
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
