#!/usr/bin/env bash
# Offline wiring/security checks; discovered by the workflow-lint job.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
python3 - <<'PY'
from pathlib import Path
import hashlib
import json
import os
import re
import shlex
import shutil
import subprocess
import tempfile
from types import SimpleNamespace
import unittest

WORKFLOWS = Path('.github/workflows')
TOKEN = 'NX_SELF_HOSTED_REMOTE_CACHE_ACCESS_TOKEN'
SERVER = 'NX_SELF_HOSTED_REMOTE_CACHE_SERVER'
READER = 'NX_SELF_HOSTED_REMOTE_CACHE_READ_SERVER'


def value(block, key):
    setting = rf'(?:{key}|nx-remote-cache-server)' if key == SERVER else key
    match = re.search(rf'^\s+{setting}:\s*(?:>-\s*)?(\$\{{\{{[^\n]+\}}\}})', block, re.M)
    assert match, f'Missing expression for {key}'
    return match[1]


def evaluate(expression, ref='refs/heads/main', protected=False,
             token='test-token', server='https://cache.example.test',
             test_type='frontend', disabled='false', event='push',
             reader='https://reader.example.test', labels=(), remote_server='', local_server='',
             save_cache='false'):
    # Only &&/||, !contains and startsWith are supported in this expression subset.
    expression = expression[3:-2].strip().replace('&&', ' and ').replace('||', ' or ')
    expression = expression.replace('!contains(', 'not contains(')
    expression = expression.replace('github.event.pull_request.labels.*.name', 'labels')
    expression = expression.replace('inputs.nx-remote-cache-server', 'remote_server')
    expression = expression.replace('inputs.save-nx-cache', 'save_cache')
    return eval(expression, {'__builtins__': {}}, {
        'github': SimpleNamespace(ref=ref, ref_protected=protected, event_name=event),
        'secrets': SimpleNamespace(**{TOKEN: token}),
        'vars': SimpleNamespace(**{SERVER: server, READER: reader, 'BUILD_CACHE_DISABLED': disabled}),
        'matrix': SimpleNamespace(test_type=test_type),
        'startsWith': str.startswith,
        'contains': lambda values, item: item in values,
        'labels': labels,
        'remote_server': remote_server,
        'save_cache': save_cache,
        'env': SimpleNamespace(**{SERVER: local_server}),
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

    def test_package_verification_hashes_the_built_artifact(self):
        for project in ('create-app', 'cli'):
            with self.subTest(project=project):
                config = json.loads(Path(f'core-web/libs/sdk/{project}/project.json').read_text())
                target = config['targets']['verify-package']
                self.assertIn('build', target['dependsOn'])
                self.assertIn('default', target['inputs'])
                self.assertIn({'dependentTasksOutputFiles': '**/*'}, target['inputs'])
                if project == 'cli':
                    self.assertIn('{workspaceRoot}/libs/sdk/*/package.json', target['inputs'])

    def test_cli_private_package_guard_ignores_nx_forced_colors(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            scripts = root / 'sdk/cli/scripts'
            scripts.mkdir(parents=True)
            shutil.copy2('core-web/libs/sdk/cli/scripts/verify-package.sh', scripts)
            (scripts.parent / 'package.json').write_text('{"name":"dotcms"}')
            sibling = root / 'sdk/events'
            sibling.mkdir()
            (sibling / 'package.json').write_text('{"private":true}')
            dist = root / 'dist'
            dist.mkdir()
            (dist / 'index.js').write_text('#!/usr/bin/env node\n')
            (dist / 'package.json').write_text('{"bin":{"dotcms":"./index.js"}}')
            npm = root / 'npm'
            npm.write_text('#!/bin/sh\nprintf \'%s\\n\' \'[{"path":"index.js"},{"path":"README.md"}]\'\n')
            npm.chmod(0o755)
            result = subprocess.run(['bash', str(scripts / 'verify-package.sh'), str(dist)],
                                    env={**os.environ, 'FORCE_COLOR': '1',
                                         'PATH': f'{root}{os.pathsep}{os.environ["PATH"]}'},
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
            self.assertIn('private package(s) under libs/sdk/: events', result.stdout)

    def test_pr_cache_bypass_disables_nx_server_and_token(self):
        for phase in ('build', 'test'):
            text = (WORKFLOWS / f'cicd_comp_{phase}-phase.yml').read_text()
            for key in (SERVER, TOKEN):
                with self.subTest(phase=phase, key=key):
                    self.assertEqual(evaluate(value(text, key), event='pull_request',
                                              labels=('CI : No Build Cache',)), '')
            self.assertEqual(evaluate(value(text, SERVER), event='pull_request',
                                      labels=('Area : CI/CD',)), 'https://reader.example.test')

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
                self.assertIn(value(text, SERVER), maven_step)
                self.assertIn(value(text, TOKEN), step_env)

    def test_build_preserves_local_cache_without_remote_settings(self):
        text = (WORKFLOWS / 'cicd_comp_build-phase.yml').read_text()
        maven_step = text.split('uses: ./.github/actions/core-cicd/maven-job', 1)[1]
        step_env = maven_step.split('        with:', 1)[0]
        local_server = 'http://127.0.0.1:47321'
        for event in ('pull_request', 'push', 'merge_group', 'schedule', 'workflow_dispatch'):
            with self.subTest(event=event):
                # A caller's explicit env wins over the composite's GITHUB_ENV export.
                actual = local_server
                if re.search(rf'^\s+{SERVER}:', step_env, re.M):
                    actual = evaluate(value(step_env, SERVER), event=event,
                                      token='', server='', reader='')
                self.assertEqual(actual, local_server)

    def test_maven_prefers_remote_server_with_local_cache_fallback(self):
        action = Path('.github/actions/core-cicd/maven-job/action.yml').read_text()
        self.assertRegex(action, r'  nx-remote-cache-server:\n(?:    .*\n)*?    default: \'\'')
        maven_step = action.split('    - id: run-maven-build\n', 1)[1].split('    - ', 1)[0]
        expression = value(maven_step, SERVER)
        for remote, local, save, expected in (
                ('https://cache.example.test', 'http://127.0.0.1:47321', 'false', 'https://cache.example.test'),
                ('https://cache.example.test', 'http://127.0.0.1:47321', 'true', 'http://127.0.0.1:47321'),
                ('https://cache.example.test', '', 'false', 'https://cache.example.test'),
                ('https://cache.example.test', '', 'true', 'https://cache.example.test'),
                ('', 'http://127.0.0.1:47321', 'false', 'http://127.0.0.1:47321'),
                ('', 'http://127.0.0.1:47321', 'true', 'http://127.0.0.1:47321'),
                ('', '', 'false', ''),
                ('', '', 'true', '')):
            with self.subTest(remote=remote, local=local, save=save):
                self.assertEqual(evaluate(expression, remote_server=remote, local_server=local,
                                          save_cache=save), expected)

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
