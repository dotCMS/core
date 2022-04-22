import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'

const projectRoot = core.getInput('project_root')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')
const testResources = ['log4j2.xml']
const srcTestResourcesFolder = 'cicd/resources'
const targetTestResourcesFolder = 'dotCMS/src/test/resources'

interface Command {
  cmd: string
  args: string[]
  workingDir?: string
  outputDir: string
}

export interface Commands {
  gradle: Command
  maven: Command
}

export const COMMANDS: Commands = {
  gradle: {
    cmd: './gradlew',
    args: ['test'],
    workingDir: dotCmsRoot,
    outputDir: `${dotCmsRoot}/build/test-results/unit-tests/xml`
  },
  maven: {
    cmd: './mvnw',
    args: ['test'],
    workingDir: dotCmsRoot,
    outputDir: `${dotCmsRoot}/target/surefire-reports`
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

  addExtraArgs(cmd)

  core.info(`
    ==================
    Running unit tests
    ==================`)
  core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
}

/**
 * Prepares tests by copyng necessary files into workspace
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
const addExtraArgs = (cmd: Command) => {
  const extraParams = core.getInput('extra_params')?.trim()
  if (!extraParams) {
    return
  }

  core.info(`Found extra params: "${extraParams}"`)
  extraParams.split(' ').forEach(param => cmd.args.push(param.trim()))
}
