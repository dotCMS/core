import * as core from '@actions/core'

/**
 * Main entry point for this action.
 */
const run = () => {
  const dockerImage = core.getInput('docker_image')
  const time = new Date().toTimeString()
  console.log(`Running tests in ${dockerImage}...${time}`)
  core.setOutput('time', time)
}

// Run main function
run()
