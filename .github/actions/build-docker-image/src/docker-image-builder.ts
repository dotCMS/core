import * as core from '@actions/core'
import * as exec from '@actions/exec'

export interface Command {
  cmd: string
  args?: string[]
  workingDir?: string
  env?: {[key: string]: string}
}

const buildId = core.getInput('build_id')
const imageName = core.getInput('image_name')
const dockerPath = core.getInput('docker_path')
const push = core.getBooleanInput('push')

/**
 * Based on build_id parameter, builds a DotCMS Docker image
 *
 * @returns a number representing the command exit code
 */
export const execute = async (): Promise<number> => {
  let cmd = dockerBuildCmd()
  let rc = await execCmd(cmd)
  if (rc !== 0) {
    core.error(`Error executing ${JSON.stringify(cmd, null, 2)}`)
    return rc
  }

  core.setOutput('built_image_name', imageName)

  if (push) {
    cmd = dockerPushCmd()
    rc = await execCmd(cmd)
    if (rc !== 0) {
      core.error(`Error executing ${JSON.stringify(cmd, null, 2)}`)
    }
  }

  return rc
}

const dockerBuildCmd = (): Command => {
  const args = ['build', '-t', imageName, '--build-arg', 'BUILD_FROM=COMMIT', '--build-arg', `BUILD_ID=${buildId}`, '.']
  return {
    cmd: 'docker',
    args,
    workingDir: dockerPath
  }
}

const dockerPushCmd = (): Command => {
  return {
    cmd: 'docker',
    args: ['push', imageName]
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
