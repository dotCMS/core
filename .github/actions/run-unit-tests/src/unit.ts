import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'
import * as path from 'path'

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
    workingDir: 'dotCMS',
    outputDir: 'dotCMS/build/test-results/unit-tests/xml'
  },
  maven: {
    cmd: './mvnw',
    args: ['test'],
    workingDir: 'dotCMS',
    outputDir: 'dotCMS/target/surefire-reports'
  }
}

const TEST_RESOURCES = ['log4j2.xml']
const SOURCE_TEST_RESOURCES_FOLDER = 'cicd/resources'
const TARGET_TEST_RESOURCES_FOLDER = 'dotCMS/src/test/resources'

/**
 * Based on a revolved {@link Command}, resolve the command to execute in order to run unit tests.
 *
 * @param cmd resolved command
 * @returns a number representing the command exit code
 */
export const runTests = async (cmd: Command): Promise<number> => {
  prepareTests()

  core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
}

const prepareTests = () => {
  const projectRoot = core.getInput('project_root')
  core.info('Preparing unit tests')
  TEST_RESOURCES.forEach(res => {
    const source = path.join(projectRoot, SOURCE_TEST_RESOURCES_FOLDER, res)
    const dest = path.join(projectRoot, TARGET_TEST_RESOURCES_FOLDER, res)
    core.info(`Copying resource ${source} to ${dest}`)
    fs.copyFileSync(source, dest)
  })
}
