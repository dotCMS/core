import * as core from '@actions/core'
import * as fs from 'fs'
import * as unit from './unit'

/**
 * Main entry point for this action.
 */
const run = () => {
  core.info('Running Core unit tests')

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
      core.setOutput('tests-run-exit-code', exitCode === 0 ? 'PASSED' : 'FAILED')
      core.setOutput('tests-results-status', cmd.outputDir)
      core.setOutput('tests-results-location', cmd.outputDir)
      core.setOutput('skip-results-report', false)
    })
    .catch(reason => {
      const messg = `Running unit tests failed due to ${reason}`
      const skipResults = !fs.existsSync(cmd.outputDir)
      core.setOutput('tests-run-exit-code', 1)
      core.setOutput('tests-run-exit-code', 'FAILED')
      core.setOutput('tests-results-location', cmd.outputDir)
      core.setOutput('skip-results-report', skipResults)
      core.setFailed(messg)
    })
}

// Run main function
run()
