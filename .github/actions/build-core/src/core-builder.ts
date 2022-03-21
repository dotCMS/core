import * as core from '@actions/core'
import * as exec from '@actions/exec'

interface Command {
  cmd: string
  args?: string[]
  workingDir?: string
}
interface Commands {
  gradle: Command
  maven: Command
}

const COMMANDS: Commands = {
  gradle: {
    cmd: './gradlew',
    args: ['createDistPrep'],
    workingDir: 'dotCMS'
  },
  maven: {
    cmd: './mvnw',
    args: ['package', '-DskipTests'],
    workingDir: 'dotCMS'
  }
}

/**
 * Based on a detected build tool environment, that is gradle or maven, this resolves the command to run in order to build core.
 *
 * @param buildToolEnv build tool environment
 * @returns a number represeting the command exit code
 */
export const build = async (buildToolEnv: string): Promise<number> => {
  const cmd = COMMANDS[buildToolEnv as keyof Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return Promise.resolve(127)
  }

  return await exec.exec(cmd.cmd, cmd.args, {cwd: cmd.workingDir})
}
