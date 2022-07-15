import * as core from '@actions/core'
import * as path from 'path'
import * as postman from './postman'

/**
 * Main entry point for this action.
 */
const run = async () => {
  const projectRoot = core.getInput('project_root')
  const dotCmsRoot = path.join(projectRoot, 'dotCMS')
  const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
  const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')

  core.info("Running Core's postman tests")
  const results = await postman.runTests()

  setOutput('tests_results_location', resultsFolder)
  setOutput('tests_results_report_location', reportFolder)
  setOutput('tests_results_status', results.testsResultsStatus)
  setOutput('tests_results_skip_report', results.skipResultsReport)
}

const setOutput = (name: string, value: string | boolean | number | undefined) => {
  const val = value === undefined ? '' : value
  core.notice(`Setting output '${name}' with value: '${val}'`)
  core.setOutput(name, value)
}

// Run main function
run()
