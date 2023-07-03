import * as core from '@actions/core'
// import * as path from 'path'
import * as cli from './cli'

/**
 * Main entry point for this action.
 */
const run = async () => {
  const projectRoot = core.getInput('project_root')
  // const dotCmsRoot = path.join(projectRoot, 'dotCMS')

  core.info("Running Core's CLI tests on PROJECT ROOT:" + projectRoot)
  const results = await cli.runTests()
  core.setOutput('test_status', results.testsResultsStatus)
}

// Run main function
run()

// /**
//  * Main entry point for this action.
//  */
// const run = () => {
//   const dockerImage = core.getInput('docker_image')
//   const time = new Date().toTimeString()
//   console.log(`Running tests in ${dockerImage}...${time}`)
//   core.setOutput('time', time)
// }

// // Run main function
// run()
