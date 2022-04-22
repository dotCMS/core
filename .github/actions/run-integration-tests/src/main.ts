import * as core from '@actions/core'
import * as fs from 'fs'
import * as integration from './integration'

/**
 * Main entry point for this action.
 */
const run = () => {
  core.info("Running Core's integration tests")

  const buildEnv = core.getInput('build_env')
  const cmd = integration.COMMANDS[buildEnv as keyof integration.Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return
  }

  integration
    .runTests(buildEnv, cmd)
    .then(exitCode => {
      const results = {
        testsRunExitCode: exitCode,
        testResultsLocation: cmd.outputDir,
        skipResultsReport: false
      }
      core.info(`Integration test results:\n${JSON.stringify(results)}`)
      core.setOutput('tests-run-exit-code', exitCode)
      core.setOutput('tests-results-status', exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput('tests-results-location', cmd.outputDir)
      core.setOutput('skip-results-report', false)
    })
    .catch(reason => {
      const messg = `Running integration tests failed due to ${reason}`
      const skipResults = cmd.outputDir && !fs.existsSync(cmd.outputDir)
      core.setOutput('tests-run-exit-code', 1)
      core.setOutput('tests-results-status', 'FAILED')
      core.setOutput('tests-results-location', cmd.outputDir)
      core.setOutput('skip-results-report', skipResults)
      core.setFailed(messg)
    })
}

// Run main function
run()
