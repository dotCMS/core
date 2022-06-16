import * as core from '@actions/core'
import * as fs from 'fs'
import * as integration from './integration'

const buildEnv = core.getInput('build_env')
const dbType = core.getInput('db_type')

/**
 * Main entry point for this action.
 */
const run = () => {
  core.info("Running Core's integration tests")

  const cmd = integration.COMMANDS[buildEnv as keyof integration.Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return
  }

  setOutput('tests_results_location', cmd.outputDir)
  setOutput('tests_results_report_location', cmd.reportDir)
  setOutput('ci_index', cmd.ciIndex)

  integration
    .runTests(cmd)
    .then(exitCode => {
      const results = {
        testsRunExitCode: exitCode,
        testResultsLocation: cmd.outputDir,
        skipResultsReport: false
      }
      core.info(`Integration test results:\n${JSON.stringify(results)}`)
      setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED')
      setOutput('tests_results_skip_report', false)
      setOutput(`${dbType}_tests_results_status`, exitCode === 0 ? 'PASSED' : 'FAILED')
      setOutput(`${dbType}_tests_results_skip_report`, false)
    })
    .catch(reason => {
      const messg = `Running integration tests failed due to ${reason}`
      const skipResults = !!cmd.outputDir && !fs.existsSync(cmd.outputDir)
      setOutput('tests_results_status', 'FAILED')
      setOutput('tests_results_skip_report', skipResults)
      setOutput(`${dbType}_tests_results_status`, 'FAILED')
      setOutput(`${dbType}_tests_results_skip_report`, skipResults)
      core.setFailed(messg)
    })
}

const setOutput = (name: string, value: string | boolean | number | undefined) => {
  const val = value === undefined ? '' : value
  core.notice(`Setting output '${name}' with value: '${val}'`)
  core.setOutput(name, value)
}

// Run main function
run()
