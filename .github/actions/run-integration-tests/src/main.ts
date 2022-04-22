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
    .runTests(cmd)
    .then(exitCode => {
      const results = {
        testsRunExitCode: exitCode,
        testResultsLocation: cmd.outputDir,
        skipResultsReport: false
      }
      core.info(`Integration test results:\n${JSON.stringify(results)}`)
      core.setOutput('tests_run_exit_code', exitCode)
      core.setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput('tests_results_location', cmd.outputDir)
      core.setOutput('tests_report_location', cmd.reportDir)
      core.setOutput('skip_results_report', false)
      core.setOutput('ci_index', cmd.ciIndex)
    })
    .catch(reason => {
      const messg = `Running integration tests failed due to ${reason}`
      const skipResults = cmd.outputDir && !fs.existsSync(cmd.outputDir)
      core.setOutput('tests_run_exit_code', 1)
      core.setOutput('tests_results_status', 'FAILED')
      core.setOutput('tests_results_location', cmd.outputDir)
      core.setOutput('tests_report_location', cmd.reportDir)
      core.setOutput('skip_results_report', skipResults)
      core.setOutput('ci_index', cmd.ciIndex)
      core.setFailed(messg)
    })
}

// Run main function
run()
