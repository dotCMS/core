import * as core from '@actions/core'
import * as fs from 'fs'
import * as integration from './integration'

const buildEnv = core.getInput('build_env')
const dbType = core.getInput('db_type') || 'postgres'

/**
 * Main entry point for this action.
 */
const run = async () => {
  core.info("Running Core's integration tests")

  const cmds = integration.COMMANDS[buildEnv as keyof integration.Commands]
  if (!cmds) {
    core.error('Cannot resolve build tool, aborting')
    return
  }

  const result = await integration.runTests(cmds)
  const skipReport = !(result.outputDir && fs.existsSync(result.outputDir))
  setOutput('tests_results_location', result.outputDir)
  setOutput('tests_results_report_location', result.reportDir)
  setOutput('ci_index', result.ciIndex)
  setOutput('tests_results_status', result.exitCode === 0 ? 'PASSED' : 'FAILED')
  setOutput('tests_results_skip_report', skipReport)
  setOutput(`${dbType}_tests_results_status`, result.exitCode === 0 ? 'PASSED' : 'FAILED', true)
  setOutput(`${dbType}_tests_results_skip_report`, skipReport)
}

const setOutput = (name: string, value?: string | boolean | number, notify = false) => {
  const val = value || ''
  if (notify && !!val) {
    core.notice(`Setting output '${name}' with value: '${val}'`)
  }
  core.setOutput(name, value)
}

// Run main function
run()
