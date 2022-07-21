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
const cicdFolder = path.join(projectRoot, 'cicd')
const resourcesFolder = path.join(cicdFolder, 'resources', 'postman')
const dockerFolder = path.join(cicdFolder, 'docker')
const licenseFolder = path.join(dockerFolder, 'license')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const logFile = path.join(dotCmsRoot, 'dotcms.log')
const volumes = [licenseFolder, path.join(dockerFolder, 'cms-shared'), path.join(dockerFolder, 'cms-local')]
const postmanTestsPath = path.join(dotCmsRoot, 'src', 'curl-test')
const postmanEnvFile = 'postman_environment.json'
const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')
const runtTestsPrefix = 'postman-tests:'
const PASSED = 'PASSED'
const FAILED = 'FAILED'
//let tomcatRoot = resolveTomcat()

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
  printInfo()

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
 * Copies logs from docker volume to standard DotCMS location.
 */
const copyOutputs = async () => {
  printInfo()
  await execCmd(
    toCommand('docker', ['cp', 'docker_dotcms-app_1:/srv/dotserver/tomcat-9.0.60/logs/dotcms.log', logFile])
  )
  await execCmd(toCommand('ls', ['-las', dotCmsRoot]))
}

/**
 * Sets up everuthing needed to run postman collections.
 */
const setup = () => {
  installDeps()
  createFolders()
  prepareLicense()
  printInfo()
}

const printInfo = async () => {
  await execCmd(toCommand('docker', ['images']))
  await execCmd(toCommand('docker', ['ps']))
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
const startDeps = () => {
  // Starting dependencies
  core.info(`
    =======================================
    Starting postman tests dependencies
    =======================================`)
  execCmdAsync(
    toCommand(
      'docker-compose',
      ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'up'],
      dockerFolder,
      DEPS_ENV
    )
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
    await execCmd(
      toCommand(
        'docker-compose',
        ['-f', 'open-distro-compose.yml', '-f', `${dbType}-compose.yml`, '-f', 'dotcms-compose.yml', 'down'],
        dockerFolder,
        DEPS_ENV
      )
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
const runPostmanCollections = async (): Promise<PostmanTestsResult> => {
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
  return await exec.exec(cmd.cmd, cmd.args || [], {cwd: cmd.workingDir, env: cmd.env})
}

const execCmdAsync = (cmd: Command) => {
  printCmd(cmd)
  exec.exec(cmd.cmd, cmd.args || [], {cwd: cmd.workingDir, env: cmd.env})
}
