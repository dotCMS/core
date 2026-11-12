#!/usr/bin/env bash
# Offline wiring/security checks; discovered by the workflow-lint job.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
python3 - <<'PY'
from pathlib import Path
import hashlib
import json
import re
import shlex
import subprocess
from types import SimpleNamespace
import unittest

WORKFLOWS = Path('.github/workflows')
TOKEN = 'NX_SELF_HOSTED_REMOTE_CACHE_ACCESS_TOKEN'
SERVER = 'NX_SELF_HOSTED_REMOTE_CACHE_SERVER'
READER = 'NX_SELF_HOSTED_REMOTE_CACHE_READ_SERVER'


def value(block, key):
    match = re.search(rf'^\s+{key}:\s*(?:>-\s*)?(\$\{{\{{[^\n]+\}}\}})', block, re.M)
    assert match, f'Missing expression for {key}'
    return match[1]


def evaluate(expression, ref='refs/heads/main', protected=False,
             token='test-token', server='https://cache.example.test',
             test_type='frontend', disabled='false', event='push',
             reader='https://reader.example.test'):
    # Evaluate only the small expression subset used in this cache wiring.
    expression = expression[3:-2].strip().replace('&&', ' and ').replace('||', ' or ')
    return eval(expression, {'__builtins__': {}}, {
        'github': SimpleNamespace(ref=ref, ref_protected=protected, event_name=event),
        'secrets': SimpleNamespace(**{TOKEN: token}),
        'vars': SimpleNamespace(**{SERVER: server, READER: reader, 'BUILD_CACHE_DISABLED': disabled}),
        'matrix': SimpleNamespace(test_type=test_type),
        'startsWith': str.startswith,
    })


class NxRemoteCacheTests(unittest.TestCase):
    def test_sdk_builds_hash_shared_rollup_helper(self):
        config = json.loads(Path('core-web/nx.json').read_text())
        self.assertIn('sharedGlobals', config['namedInputs']['default'])
        self.assertIn('default', config['namedInputs']['production'])
        self.assertIn('{workspaceRoot}/tools/rollup/types-first.cjs',
                      config['namedInputs']['sharedGlobals'])

    def test_frontend_tests_hash_shared_vitest_plugin(self):
        config = json.loads(Path('core-web/nx.json').read_text())
        self.assertIn('{workspaceRoot}/tools/vitest-stub-scss.mts',
                      config['targetDefaults']['test']['inputs'])

    def test_builds_and_tests_hash_all_locked_dependency_versions(self):
        config = json.loads(Path('core-web/nx.json').read_text())
        self.assertIn('default', config['targetDefaults']['test']['inputs'])
        self.assertIn('sharedGlobals', config['namedInputs']['default'])
        self.assertIn('default', config['namedInputs']['production'])
        self.assertIn('{workspaceRoot}/pnpm-lock.yaml', config['namedInputs']['sharedGlobals'])

    def test_sdk_ai_hashes_backend_openapi_source(self):
        config = json.loads(Path('core-web/libs/sdk/ai/project.json').read_text())
        inputs = config['namedInputs']['default']
        self.assertIn('{projectRoot}/**/*', inputs)
        self.assertIn('sharedGlobals', inputs)
        self.assertIn('production', config['targets']['build']['inputs'])
        runtime = next(item['runtime'] for item in inputs if isinstance(item, dict))
        actual = subprocess.check_output(shlex.split(runtime), cwd='core-web', text=True).strip()
        source = Path('dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml').read_bytes()
        self.assertEqual(actual, hashlib.sha256(source).hexdigest())

    def test_shared_workflows_disable_remote_without_settings_or_with_kill_switch(self):
        for phase in ('build', 'test'):
            text = (WORKFLOWS / f'cicd_comp_{phase}-phase.yml').read_text()
            with self.subTest(phase=phase):
                self.assertRegex(text, rf'      {TOKEN}:\n(?:        .*\n)*?        required: false')
                expression = value(text, SERVER)
                self.assertEqual(evaluate(expression), 'https://cache.example.test')
                self.assertEqual(evaluate(expression, token=''), '')
                self.assertEqual(evaluate(expression, server=''), '')
                self.assertEqual(evaluate(expression, disabled='true'), '')
                maven_step = text.split('uses: ./.github/actions/core-cicd/maven-job', 1)[1]
                step_env = maven_step.split('        with:', 1)[0]
                self.assertIn(value(text, SERVER), step_env)
                self.assertIn(value(text, TOKEN), step_env)

    def test_only_frontend_matrix_jobs_receive_credentials(self):
        text = (WORKFLOWS / 'cicd_comp_test-phase.yml').read_text()
        for test_type in ('jvm', 'cli', 'integration', 'postman', 'e2e'):
            for key in (SERVER, TOKEN):
                with self.subTest(test_type=test_type, key=key):
                    self.assertEqual(evaluate(value(text, key), test_type=test_type), '')
        self.assertEqual(evaluate(value(text, TOKEN)), 'test-token')

    def test_prs_use_reader_without_writer_token(self):
        for phase in ('build', 'test'):
            text = (WORKFLOWS / f'cicd_comp_{phase}-phase.yml').read_text()
            for token in ('', 'accidentally-forwarded-writer'):
                with self.subTest(phase=phase, token_present=bool(token)):
                    self.assertEqual(evaluate(value(text, SERVER), event='pull_request', token=token),
                                     'https://reader.example.test')
                    self.assertEqual(evaluate(value(text, TOKEN), event='pull_request', token=token), '')
                    self.assertEqual(evaluate(value(text, SERVER), event='pull_request', token=token, reader=''), '')
                    self.assertEqual(evaluate(value(text, SERVER), event='pull_request', token=token, disabled='true'), '')
                    self.assertEqual(evaluate(value(text, TOKEN), disabled='true'), '')
            if phase == 'test':
                for test_type in ('jvm', 'cli', 'integration', 'postman', 'e2e'):
                    for key in (SERVER, TOKEN):
                        self.assertEqual(evaluate(value(text, key), event='pull_request', test_type=test_type), '')

    def test_non_prs_never_fall_back_to_public_reader(self):
        for phase in ('build', 'test'):
            text = (WORKFLOWS / f'cicd_comp_{phase}-phase.yml').read_text()
            for event in ('push', 'merge_group', 'workflow_dispatch', 'schedule'):
                with self.subTest(phase=phase, event=event):
                    self.assertEqual(evaluate(value(text, SERVER), event=event, token=''), '')
                    self.assertEqual(evaluate(value(text, SERVER), event=event, server=''), '')

    def test_trusted_callers_forward_token_to_build_and_test(self):
        cases = {
            'cicd_2-merge-queue.yml': ('refs/heads/gh-readonly-queue/main/pr-1', False),
            'cicd_3-trunk.yml': ('refs/heads/main', False),
            'cicd_4-nightly.yml': ('refs/heads/main', False),
            'cicd_5-lts.yml': ('refs/heads/release-26.01', True),
        }
        for filename, (ref, protected) in cases.items():
            text = (WORKFLOWS / filename).read_text()
            for job in ('build', 'test'):
                with self.subTest(filename=filename, job=job):
                    block = re.search(rf'^  {job}:\n(.*?)(?=^  [\w-]+:|\Z)', text, re.M | re.S)[1]
                    expression = value(block, TOKEN)
                    self.assertEqual(evaluate(expression, ref=ref, protected=protected), 'test-token')
                    self.assertEqual(evaluate(expression, ref=ref, protected=protected, token=''), '')
                    if filename != 'cicd_2-merge-queue.yml':
                        self.assertEqual(evaluate(expression, ref='refs/heads/feature'), '')
                        self.assertEqual(evaluate(expression, ref='refs/tags/v26.01'), '')
                    if filename == 'cicd_5-lts.yml':
                        self.assertEqual(evaluate(expression, ref=ref, protected=False), '')

    def test_no_other_callers_receive_write_token(self):
        enabled = {'cicd_2-merge-queue.yml', 'cicd_3-trunk.yml',
                   'cicd_4-nightly.yml', 'cicd_5-lts.yml',
                   'cicd_comp_build-phase.yml', 'cicd_comp_test-phase.yml'}
        for path in WORKFLOWS.glob('*.yml'):
            if path.name not in enabled:
                with self.subTest(workflow=path.name):
                    self.assertNotIn(TOKEN, path.read_text())


unittest.main()
PY
