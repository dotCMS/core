import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'

const buildEnv = core.getInput('build_env')
const projectRoot = core.getInput('project_root')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const testResources = ['log4j2.xml']
const srcTestResourcesFolder = 'cicd/resources'
const targetTestResourcesFolder = 'dotCMS/src/test/resources'
const runtTestsPrefix = 'unit-tests:'
const outputDir = `${dotCmsRoot}/build/test-results/test`
const reportDir = `${dotCmsRoot}/build/reports/tests/test`

interface Command {
  cmd: string
  args: string[]
  workingDir?: string
  outputDir: string
  reportDir: string
}

export interface Commands {
  gradle: Command
  maven: Command
}

export const COMMANDS: Commands = {
  gradle: {
    cmd: './gradlew',
    args: ['generateDependenciesFromMaven', '&&', 'test', '--stacktrace'],
    workingDir: dotCmsRoot,
    outputDir: outputDir,
    reportDir: reportDir
  },
  maven: {
    cmd: './mvnw',
    args: ['test'],
    workingDir: dotCmsRoot,
    outputDir: outputDir,
    reportDir: reportDir
  }
}

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run unit tests.
 *
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
export const runTests = async (cmd: Command): Promise<number> => {
  prepareTests()

  resolveParams(cmd)

  core.info(`
    ==================
    Running unit tests
    ==================`)
  core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
  try {
    return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
  } catch (err) {
    core.setFailed(`Unit tests failed due to: ${err}`)
    return 127
  }
}

/**
 * Prepares tests by copying necessary files into workspace
 */
const prepareTests = () => {
  core.info('Preparing unit tests')

  testResources.forEach(res => {
    const source = path.join(projectRoot, srcTestResourcesFolder, res)
    const dest = path.join(projectRoot, targetTestResourcesFolder, res)
    core.info(`Copying resource ${source} to ${dest}`)
    fs.copyFileSync(source, dest)
  })
}

/**
 * Add extra parameters to command arguments
 *
 * @param cmd {@link Command} object holding command and arguments
 */
const resolveParams = (cmd: Command) => {
  const tests = core.getInput('tests')?.trim()
  if (!tests) {
    core.info('No specific unit tests found')
    return
  }

  core.info(`Commit message found: "${tests}"`)

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
