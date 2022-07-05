import * as core from '@actions/core'
import * as exec from '@actions/exec'

const pullOverBuild = core.getBooleanInput('pull_over_build')
const buildId = core.getInput('build_id')
const skipPull = core.getBooleanInput('skip_pull')
const imageName = core.getInput('image_name')
const dockerPath = core.getInput('docker_path')

/**
 * Based on build_id parameter, builds a DotCMS Docker image
 *
 * @returns a number representing the command exit code
 */
export const build = async (): Promise<number> => {
  const cmd = 'docker'
  const args = []
  if (pullOverBuild) {
    args.push('pull', imageName)
  } else {
    args.push('build')
    if (!skipPull) {
      args.push('--pull')
    }
    args.push(
      '--no-cache',
      '-t',
      imageName,
      '--build-arg',
      'BUILD_FROM=COMMIT',
      '--build-arg',
      `BUILD_ID=${buildId}`,
      '.'
    )
  }

  core.setOutput('built_image_name', imageName)

  core.info(`Executing command: ${cmd} ${args.join(' ')}`)
  return await exec.exec(cmd, args, {cwd: dockerPath})
}
