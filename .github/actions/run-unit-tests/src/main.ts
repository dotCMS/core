import * as core from '@actions/core'
import * as fs from 'fs'
import * as unit from './unit'

/**
 * Main entry point for this action.
 */
const run = () => {
  core.info("Running Core's unit tests")

  const buildEnv = core.getInput('build_env')
  const cmd = unit.COMMANDS[buildEnv as keyof unit.Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return
  }

  unit
    .runTests(cmd)
    .then(exitCode => {
      const results = {
        testsRunExitCode: exitCode,
        testResultsLocation: cmd.outputDir,
        skipResultsReport: false
      }
      core.info(`Unit test results:\n${JSON.stringify(results)}`)
      core.setOutput('tests_run_exit_code', exitCode)
      core.setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput('tests_results_location', cmd.outputDir)
      core.setOutput('tests_report_location', cmd.reportDir)
      core.setOutput('skip_results_report', false)
    })
    .catch(reason => {
      const messg = `Running unit tests failed due to ${reason}`
      const skipResults = !fs.existsSync(cmd.outputDir)
      core.setOutput('tests_run_exit_code', 1)
      core.setOutput('tests_run_exit_status', 'FAILED')
      core.setOutput('tests_results_location', cmd.outputDir)
      core.setOutput('tests_report_location', cmd.reportDir)
      core.setOutput('skip_results_report', skipResults)
      core.setFailed(messg)
    })
}

// Run main function
run()
