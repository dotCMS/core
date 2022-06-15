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

  core.setOutput('tests_results_location', cmd.outputDir)
  core.setOutput('tests_results_report_location', cmd.reportDir)
  core.setOutput('ci_index', cmd.ciIndex)

  const dbType = core.getInput('db_type')

  integration
    .runTests(cmd)
    .then(exitCode => {
      const results = {
        testsRunExitCode: exitCode,
        testResultsLocation: cmd.outputDir,
        skipResultsReport: false
      }
      core.info(`Integration test results:\n${JSON.stringify(results)}`)
      core.setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput('tests_results_skip_report', false)
      core.setOutput(`${dbType}_tests_results_status`, exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput(`${dbType}_tests_results_skip_report`, false)
    })
    .catch(reason => {
      const messg = `Running integration tests failed due to ${reason}`
      const skipResults = cmd.outputDir && !fs.existsSync(cmd.outputDir)
      core.setOutput('tests_results_status', 'FAILED')
      core.setOutput('tests_results_skip_report', skipResults)
      core.setOutput(`${dbType}_tests_results_status`, 'FAILED')
      core.setOutput(`${dbType}_tests_results_skip_report`, skipResults)
      core.setFailed(messg)
    })
}

// Run main function
run()
