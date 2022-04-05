import * as core from '@actions/core'
import * as exec from '@actions/exec'

interface Command {
  cmd: string
  args: string[]
  workingDir?: string
}

interface Commands {
  gradle: Command
  maven: Command
}

const COMMANDS: Commands = {
  gradle: {
    cmd: './gradlew',
    args: ['test'],
    workingDir: 'dotCMS'
  },
  maven: {
    cmd: './mvnw',
    args: ['test'],
    workingDir: 'dotCMS'
  }
}

/**
 * Based on a detected build environment, that is gradle or maven, this resolves the command to run in order to run unit tests.
 *
 * @param buildEnv build environment
 * @returns a number represeting the command exit code
 */
export const runTests = async (buildEnv: string): Promise<number> => {
  const cmd = COMMANDS[buildEnv as keyof Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return Promise.resolve(127)
  }

  core.info(`Executing command: ${cmd.cmd} ${cmd.args.join(' ')}`)
  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
}
