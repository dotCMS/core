import * as core from '@actions/core'
import * as path from 'path'
import * as postman from './postman'

/**
 * Main entry point for this action.
 */
const run = () => {
  const projectRoot = core.getInput('project_root')
  const dotCmsRoot = path.join(projectRoot, 'dotCMS')
  const resultsFolder = path.join(dotCmsRoot, 'build', 'test-results', 'postmanTest')
  const reportFolder = path.join(dotCmsRoot, 'build', 'reports', 'tests', 'postmanTest')

  core.info("Running Core's postman tests")
  core.setOutput('tests_results_location', resultsFolder)
  core.setOutput('tests_results_report_location', reportFolder)

  postman
    .runTests()
    .then(results => {
      core.info(`Postman test results:\n${JSON.stringify(results)}`)
      core.setOutput('tests_results_status', results.testsResultsStatus)
      core.setOutput('tests_results_skip_report', results.testsResultsStatus)
      if (results.testsResultsStatus === 'FAILED') {
        core.setFailed('Postman tests failed')
      }
    })
    .catch(reason => {
      core.error(`Postman test results failed due to ${reason}`)
      core.setOutput('tests_results_status', 'FAILED')
      core.setOutput('tests_results_skip_report', true)
      core.setFailed(reason)
    })
}

// Run main function
run()
