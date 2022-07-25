import * as core from '@actions/core'
import * as fs from 'fs'
import * as unit from './unit'

/**
 * Main entry point for this action.
 */
const run = async () => {
  core.info("Running Core's unit tests")

  const buildEnv = core.getInput('build_env')
  const cmd = unit.COMMANDS[buildEnv as keyof unit.Commands]
  if (!cmd) {
    core.error('Cannot resolve build tool, aborting')
    return
  }

  const exitCode = await unit.runTests(cmd)
  setOutput('tests_results_location', cmd.outputDir)
  setOutput('tests_results_report_location', cmd.reportDir)
  setOutput('tests_results_status', exitCode === 0 ? 'PASSED' : 'FAILED')
  setOutput('tests_results_skip_report', !fs.existsSync(cmd.outputDir))
}

const setOutput = (name: string, value: string | boolean | number | undefined) => {
  const val = value === undefined ? '' : value
  core.notice(`Setting output '${name}' with value: '${val}'`)
  core.setOutput(name, value)
}

// Run main function
run()
