import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as path from 'path'

interface Command {
  cmd: string
  args: string[]
  workingDir?: string
}
interface Commands {
  gradle: Command[]
  maven: Command[]
}

const gradleCmd = './gradlew'
const mavenCmd = './mvnw'
const projectRoot = core.getInput('project_root')
const dotCmsRoot = path.join(projectRoot, 'dotCMS')

const COMMANDS: Commands = {
  gradle: [
    {
      cmd: gradleCmd,
      args: ['generateDependenciesFromMaven'],
      workingDir: dotCmsRoot
    },
    {
      cmd: gradleCmd,
      args: ['createDistPrep', '--stacktrace'],
      workingDir: dotCmsRoot
    },
    {
      cmd: 'rm',
      args: ['-rf', '/home/runner/.m2/repository/com/dotcms']
    }
  ],
  maven: [
    {
      cmd: mavenCmd,
      args: ['package', '-DskipTests'],
      workingDir: dotCmsRoot
    }
  ]
}

/**
 * Based on a detected build environment, that is gradle or maven, this resolves the command to run in order to build core.
 *
 * @param buildEnv build environment
 * @returns a number representing the command exit code
 */
export const build = async (buildEnv: string): Promise<number> => {
  const cmds = COMMANDS[buildEnv as keyof Commands]
  if (!cmds || cmds.length === 0) {
    core.error('Cannot resolve build tool, aborting')
    return Promise.resolve(127)
  }

  let rc = 0
  for (const cmd of cmds) {
    core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
    rc = await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
    if (rc !== 0) {
      break
    }
  }

  return rc
}
