import * as core from '@actions/core'
import * as exec from '@actions/exec'
import * as fs from 'fs'

interface Command {
  cmd: string
  args: string[]
  env?: {[key: string]: string}
}

interface TestmoOperation {
  operation: string
  args: string[]
  env?: {[key: string]: string}
}

interface TestmoOperations {
  resources: TestmoOperation[]
  resources_thread: TestmoOperation[]
  create: TestmoOperation[]
  submit: TestmoOperation[]
  submit_thread: TestmoOperation[]
  complete: TestmoOperation[]
}

const capitalize = (str: string): string => str.charAt(0).toUpperCase() + str.slice(1)

const githubServerUrl = core.getInput('github_server_url')
const githubRepository = core.getInput('github_repository')
const githubSha = core.getInput('github_sha')
const githubRunId = core.getInput('github_run_id')
const testmoUrl = core.getInput('testmo_url')
const testmoToken = core.getInput('testmoToken')
const testProjectId = core.getInput('testmo_project_id')
const testmoRunId = core.getInput('testmo_run_id') || ''
const testType = core.getInput('test_type')
const testResultsLocation = core.getInput('tests_results_location') || ''
const ciIndex = core.getInput('ci_index')
const ciTotal = core.getInput('ci_total')
const ciLabel = core.getInput('ci_label')
const debug = core.getBooleanInput('debug')
const runUrl = `${githubServerUrl}/${githubRepository}/actions/runs/${githubRunId}`
const testmoRunIdFile = 'testmo_run_id.txt'
const testmoEnvVars = {
  TESTMO_URL: testmoUrl,
  TESTMO_TOKEN: testmoToken
}

const resources = [
  {
    operation: 'automation:resources:add-field',
    args: ['--name', 'git', '--type', 'string', '--value', githubSha, '--resources', 'resources.json']
  },
  {
    operation: 'automation:resources:add-link',
    args: ['--name', 'build', '--url', runUrl, '--resources', 'resources.json']
  }
]
const create: TestmoOperation[] = [
  {
    operation: 'automation:run:create',
    args: [
      '--instance',
      testmoUrl,
      '--project-id',
      testProjectId,
      '--name',
      `"${capitalize(testType)} Tests"`,
      '--source',
      `${testType}-tests`,
      '--resources',
      'resources.json',
      '>',
      testmoRunIdFile
    ],
    env: testmoEnvVars
  }
]
const TESTMO_OPERATIONS: TestmoOperations = {
  resources,
  resources_thread: [...resources, ...create],
  create,
  submit: [
    ...resources,
    {
      operation: 'automation:run:submit',
      args: [
        '--instance',
        testmoUrl,
        '--project-id',
        testProjectId,
        '--name',
        `"${capitalize(testType)} Tests"`,
        '--source',
        `${testType}-tests`,
        '--resources',
        'resources.json',
        '--results',
        testResultsLocation
      ],
      env: testmoEnvVars
    }
  ],
  submit_thread: [
    {
      operation: 'automation:run:submit-thread',
      args: ['--instance', testmoUrl, '--run-id', testmoRunId, '--results', testResultsLocation],
      env: {CI_INDEX: ciIndex, CI_TOTAL: ciTotal, ...testmoEnvVars}
    }
  ],
  complete: [
    {
      operation: 'automation:run:complete',
      args: ['--instance', testmoUrl, '--run-id', testmoRunId],
      env: testmoEnvVars
    }
  ]
}

/**
 * Based on a provided operation, this resolves the Testmo command to run.
 *
 * @param operation Testmo operation
 * @returns a number representing the return code
 */
export const runTestmo = async (operation: string): Promise<number> => {
  const normalized = operation.replace(/-/g, '_')
  const testmoOps: TestmoOperation[] = TESTMO_OPERATIONS[normalized as keyof TestmoOperations]
  if (!testmoOps) {
    core.error(`Cannot resolve Testmo operation from ${operation}`)
    return Promise.reject()
  }
  handleArgs(testmoOps)

  switch (operation) {
    case 'init': {
      return runNpmCi()
    }
    case 'resources':
    case 'complete': {
      return await runTestmoOperations(testmoOps)
    }
    case 'resources-thread':
    case 'submit':
    case 'submit-thread': {
      let rc = await runNpmCi()
      if (rc === 0) {
        rc = await runTestmoOperations(testmoOps)
      }
      return rc
    }
    case 'create': {
      const rc = await runTestmoOperations(testmoOps)
      const newRestmoRunId = fs.readFileSync(testmoRunIdFile, {encoding: 'utf8', flag: 'r'})
      core.setOutput('testmo_run_id', newRestmoRunId)
      return rc
    }
    default: {
      core.error('Cannot resolve operation: ${operation}')
      return Promise.reject(127)
    }
  }
}

const runCommand = async (cmd: Command): Promise<number> => {
  const cmdAndArgs = `${cmd.cmd} ${cmd.args.join(' ')}`
  core.info(`Running command: ${cmdAndArgs}`)
  const rc = await exec.exec(cmd.cmd, cmd.args, {env: cmd.env})
  core.info(`Command "${cmdAndArgs}" returned ${rc} code`)
  return rc
}

const toCommand = (cmd: string, args: string[], env?: {[key: string]: string}): Command => {
  return {
    cmd,
    args,
    env
  }
}

const runTestmoOperation = async (testmoOp: TestmoOperation): Promise<number> => {
  core.info(`Running Testmo operation: ${testmoOp.operation} with args: ${testmoOp.args.join(' ')}`)
  return runCommand(toCommand('npx', ['testmo', testmoOp.operation, ...testmoOp.args], testmoOp.env))
}

const runTestmoOperations = async (testmoOps: TestmoOperation[]): Promise<number> => {
  let rc = await Promise.resolve(0)
  for (const op of testmoOps) {
    if (rc !== 0) {
      break
    }
    rc = await runTestmoOperation(op)
  }
  return rc
}

const handleArgs = (testmoOps: TestmoOperation[]) => {
  if (debug) {
    testmoOps.forEach(op => {
      if (op.operation !== 'automation:run:create') {
        op.args.push('--debug')
      }
    })
  }
}

const runNpmCi = async (): Promise<number> => {
  const cmds = [
    {cmd: 'npm', args: ['init', '-y']},
    {cmd: 'npm', args: ['install']},
    //{cmd: 'npm', args: ['install', 'npx']},
    {cmd: 'npm', args: ['install', '--no-save', '@testmo/testmo-cli']}
  ]
  return runCommands(cmds)
}

const runCommands = async (cmds: Command[]): Promise<number> => {
  if (!cmds) {
    return await Promise.reject(129)
  }

  let rc = await Promise.resolve(0)
  for (const cmd of cmds) {
    if (rc !== 0) {
      break
    }
    rc = await runCommand(cmd)
  }

  return rc
}
