// Offline tests for the actual github-script blocks; no API calls or credentials.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { execFileSync } = require('node:child_process');
const path = require('node:path');
const os = require('node:os');
const { test } = require('node:test');

const root = path.resolve(__dirname, '../../..');
const workflowPath = path.join(root, '.github/workflows/cicd_post-pr-cloud-control.yml');
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;

function script(id) {
    const workflow = fs.readFileSync(workflowPath, 'utf8');
    const step = workflow.split(/(?=^      - name:)/m).find(block => block.includes(`id: ${id}\n`));
    assert.ok(step, `Missing step ${id}`);
    return step.split('          script: |\n')[1].split('\n')
        .map(line => line.startsWith('            ') ? line.slice(12) : line).join('\n').trim();
}

async function deploy({ metadata = {}, pr = {}, existing = null, status = 200, calls = [],
    mutationStatus = 200, afterConflict = null, run = {}, registryStatus = 200,
    immutableDigest = 'sha256:' + 'a'.repeat(64), mutableDigest = immutableDigest } = {}) {
    const sha = 'a'.repeat(40);
    const pull = {
        number: 1234, state: 'open', labels: [{ name: 'PR: docker image' }],
        head: { sha, ref: 'feat/My_branch.with-dots', repo: { full_name: 'dotcms/core' } },
        ...pr,
    };
    const manifest = { pr: 1234, sha, ...metadata };
    const registryCalls = [];
    const logs = [];
    let reads = 0;
    const context = { repo: { owner: 'dotcms', repo: 'core' }, payload: {
        workflow_run: { event: 'pull_request', head_sha: sha, head_branch: pull.head.ref, ...run },
    } };
    const github = { rest: { pulls: { get: async () => ({ data: pull }) } } };
    const core = { info: message => logs.push(message), summary: {
        addRaw: () => core.summary, write: async () => {},
    } };
    const fetch = async (url, options = {}) => {
        if (url.startsWith('https://hub.docker.com/')) {
            registryCalls.push(url);
            assert.equal(options.headers?.Authorization, undefined, 'Cloud Control credentials must not reach Docker Hub');
            return { status: registryStatus, ok: registryStatus === 200,
                json: async () => ({ digest: registryCalls.length === 1 ? immutableDigest : mutableDigest }) };
        }
        const method = options.method || 'GET';
        calls.push({ url, ...options, method, body: options.body && JSON.parse(options.body) });
        const responseStatus = method === 'GET'
            ? (url.includes('/detail?') ? 403 : (reads++ ? 200 : status)) : mutationStatus;
        const row = reads > 1 ? afterConflict : existing;
        const body = row ? [{ org_key: 'dotpr', env_key: '1234', ...row }] : [];
        return { status: responseStatus, ok: responseStatus >= 200 && responseStatus < 300,
            json: async () => body };
    };
    const env = { CLOUD_CONTROL_DOTCMS_MACHINE_TOKEN: 'test-token', RUNNER_TEMP: '/tmp' };
    const requireMock = name => {
        assert.equal(name, 'node:fs');
        return { readFileSync: () => JSON.stringify(manifest) };
    };
    await new AsyncFunction('github', 'context', 'core', 'fetch', 'process', 'require', script('deploy'))(
        github, context, core, fetch, { env }, requireMock);
    return { calls, registryCalls, logs };
}

const image = 'dotcms/dotcms-test:pr-1234-feat-my_branch.with-dots';
const stopped = { image, deploy_status: 'stopped', replicas: 0, pending_delete: false };

test('trusted workflow covers both publishers and never checks out PR code', () => {
    const workflow = fs.readFileSync(workflowPath, 'utf8');
    assert.match(workflow, /workflow_run:/);
    assert.match(workflow, /'-1 PR Check'/);
    assert.match(workflow, /'Publish PR Test Image'/);
    assert.doesNotMatch(workflow, /actions\/checkout|pull_request_target:/);
    assert.match(workflow, /head_repository.full_name == github.repository/);
    assert.match(workflow, /secrets.CLOUD_CONTROL_DOTCMS_MACHINE_TOKEN/);
    assert.doesNotMatch(workflow, /pull-requests: write|contents: write/);
    const publisher = fs.readFileSync(path.join(root, '.github/workflows/cicd_comp_publish-pr-test-image.yml'), 'utf8');
    assert.ok(publisher.indexOf('name: Record Cloud Control deployment') > publisher.indexOf('id: push\n'));
    assert.match(publisher, /name: pr-cloud-control/);
    assert.match(publisher, /steps.push.outcome == 'success'/);
    assert.doesNotMatch(publisher, /CLOUD_CONTROL_DOTCMS_MACHINE_TOKEN/);
});

test('both entry points serialize publishing per PR without cancelling an active image push', () => {
    const publisher = fs.readFileSync(path.join(root, '.github/workflows/cicd_comp_publish-pr-test-image.yml'), 'utf8');
    assert.match(publisher, /    concurrency:\n      group: publish-pr-image-job-\$\{\{ inputs\.pr \}\}\n      cancel-in-progress: false\n      queue: max/);
    for (const file of ['cicd_1-pr.yml', 'cicd_publish-pr-test-image.yml']) {
        assert.match(fs.readFileSync(path.join(root, '.github/workflows', file), 'utf8'),
            /uses: \.\/\.github\/workflows\/cicd_comp_publish-pr-test-image\.yml/);
    }
});

test('queued publishers check the current PR SHA before discovering artifacts or writing image tags', () => {
    const publisher = fs.readFileSync(path.join(root, '.github/workflows/cicd_comp_publish-pr-test-image.yml'), 'utf8');
    const step = publisher.split('      - name: Resolve artifact run and tag\n')[1].split('      - name:')[0];
    const shell = step.split('        run: |\n')[1].split('\n')
        .map(line => line.startsWith('          ') ? line.slice(10) : line).join('\n');
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'pr-publish-guard-'));
    try {
        fs.writeFileSync(path.join(dir, 'gh'), `#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$GH_CALLS"
if [[ "$*" == *'repos/dotcms/core/pulls/1234'* ]]; then
  printf '%s' "$CURRENT_SHA"
else
  echo 'Stale publisher tried to discover artifacts' >&2
  exit 99
fi
`, { mode: 0o755 });
        for (const stale of [false, true]) {
            const output = path.join(dir, stale ? 'stale-output' : 'current-output');
            execFileSync('bash', ['-e', '-c', shell], { cwd: dir, env: {
                ...process.env, PATH: `${dir}:${process.env.PATH}`, GITHUB_OUTPUT: output, GH_CALLS: output + '-calls',
                CURRENT_SHA: (stale ? 'b' : 'a').repeat(40), SHA: 'a'.repeat(40),
                REPO: 'dotcms/core', PR: '1234', BRANCH: 'feature/test',
                RUN_ID_IN: stale ? '' : '789', GITHUB_RUN_ID: '42', GH_TOKEN: 'fake-token',
            } });
            const apiCalls = fs.readFileSync(output + '-calls', 'utf8').trim().split('\n');
            assert.equal(apiCalls.length, 1);
            assert.ok(apiCalls[0].includes('repos/dotcms/core/pulls/1234'));
            const values = fs.readFileSync(output, 'utf8');
            assert.ok(values.includes(`found=${!stale}\n`));
            if (stale) assert.doesNotMatch(values, /tags<<|dotcms\/dotcms-test/);
            else assert.ok(values.includes('dotcms/dotcms-test:pr-1234-feature-test'));
        }
    } finally {
        fs.rmSync(dir, { recursive: true, force: true });
    }
});

test('only a non-expired manifest from this run is selected', async () => {
    const outputs = {};
    const endpoint = {};
    await new AsyncFunction('github', 'context', 'core', script('artifact'))({
        rest: { actions: { listWorkflowRunArtifacts: endpoint } },
        paginate: async (actual, args) => {
            assert.equal(actual, endpoint);
            assert.equal(args.run_id, 42);
            return [{ id: 1, name: 'pr-cloud-control', expired: true },
                { id: 2, name: 'other', expired: false },
                { id: 3, name: 'pr-cloud-control', expired: false }];
        },
    }, { repo: { owner: 'dotcms', repo: 'core' }, payload: { workflow_run: { id: 42 } } },
    { setOutput: (key, value) => { outputs[key] = value; } });
    assert.equal(outputs.id, 3);
});

test('missing environment is provisioned with the existing mutable image tag', async () => {
    const { calls } = await deploy({ mutationStatus: 201 });
    assert.equal(calls.length, 2);
    assert.equal(calls[0].url, 'https://cloud.dotcms.dev/api/envs?org_key=dotpr&name=1234&scope=all');
    assert.equal(calls[1].url, 'https://cloud.dotcms.dev/api/provision');
    assert.equal(calls[1].method, 'POST');
    assert.deepEqual(calls[1].body, { wizard: true, org: { org_key: 'dotpr' },
        env: { env_key: '1234', image, replicas: 1 } });
    assert.equal(calls[1].headers.Authorization, 'Bearer test-token');
});

test('trusted deployment verifies matching immutable and mutable registry digests before Cloud Control access', async () => {
    const { calls, registryCalls } = await deploy({ existing: { ...stopped, deploy_status: 'deployed', replicas: 1 } });
    assert.deepEqual(registryCalls, [
        'https://hub.docker.com/v2/repositories/dotcms/dotcms-test/tags/pr-1234-feat-my_branch.with-dots_aaaaaaa',
        'https://hub.docker.com/v2/repositories/dotcms/dotcms-test/tags/pr-1234-feat-my_branch.with-dots',
    ]);
    assert.equal(calls.length, 1);
});

test('a forged publication manifest cannot provision an absent, invalid or mismatched image', async () => {
    for (const registry of [{ registryStatus: 404 }, { registryStatus: 500 },
        { immutableDigest: null }, { mutableDigest: 'sha256:' + 'b'.repeat(64) }]) {
        const calls = [];
        await assert.rejects(deploy({ calls, ...registry }), /image|publication|registry/i);
        assert.equal(calls.length, 0);
    }
});

test('stopped matching environment starts without changing its image', async () => {
    const { calls } = await deploy({ existing: stopped });
    assert.equal(calls.length, 2);
    assert.equal(calls[1].method, 'PATCH');
    assert.equal(calls[1].url, 'https://cloud.dotcms.dev/api/envs/dotpr/detail?env_key=1234');
    assert.deepEqual(calls[1].body, { replicas: 1 });
});

test('running or provisioning matching environment needs no writes', async () => {
    for (const deploy_status of ['deployed', 'pending', 'provisioning', 'reconfiguring']) {
        const { calls } = await deploy({ existing: { ...stopped, deploy_status, replicas: 1 } });
        assert.equal(calls.length, 1);
    }
});

test('wrong image fails without changing the environment', async () => {
    await assert.rejects(deploy({ existing: { ...stopped, image: 'dotcms/dotcms:latest' } }), /image.*match/i);
});

test('failed, deleting and decommissioned environments are not restarted', async () => {
    for (const existing of [{ ...stopped, deploy_status: 'failed' },
        { ...stopped, pending_delete: true }, { ...stopped, deploy_status: 'decommissioned' }]) {
        await assert.rejects(deploy({ existing }), /cannot.*start/i);
    }
});

test('environment keys use only the PR number regardless of branch name', async () => {
    for (const branch of ['feature/' + 'a-'.repeat(100), 'UPPER.case_under/branch']) {
        const head = { sha: 'a'.repeat(40), ref: branch, repo: { full_name: 'dotcms/core' } };
        const { calls } = await deploy({ pr: { head }, mutationStatus: 201 });
        assert.equal(calls[1].body.env.env_key, '1234');
    }
});

test('the reported PR uses a short numeric environment key', async () => {
    const branch = 'issue-38556-preserve-legacy-tools-on-reorder';
    const head = { sha: 'a'.repeat(40), ref: branch, repo: { full_name: 'dotcms/core' } };
    const { calls } = await deploy({ metadata: { pr: 38557 }, pr: { number: 38557, head }, mutationStatus: 201 });
    assert.equal(calls[1].body.env.env_key, '38557');
});

test('image tag normalization matches the existing publisher, including long and unusual branches', async () => {
    const publisher = fs.readFileSync(path.join(root, '.github/workflows/cicd_comp_publish-pr-test-image.yml'), 'utf8');
    const normalization = publisher.match(/^          norm=\$\([\s\S]*?cut -c1-100\)/m)[0];
    for (const branch of ['Feature/A__B.foo', 'a'.repeat(150), '._/☃', '--feature---fix..', '']) {
        const expected = execFileSync('bash', ['-c', `${normalization}\nprintf '%s' "$norm"`],
            { env: { ...process.env, BRANCH: branch } }).toString();
        const head = { sha: 'a'.repeat(40), ref: branch, repo: { full_name: 'dotcms/core' } };
        const { calls } = await deploy({ pr: { head }, mutationStatus: 201 });
        assert.equal(calls[1].body.env.image, `dotcms/dotcms-test:pr-1234-${expected}`);
    }
});

test('image sticky comment links to the matching Cloud Control detail page on create and update', async () => {
    const publisher = fs.readFileSync(path.join(root, '.github/workflows/cicd_comp_publish-pr-test-image.yml'), 'utf8');
    const step = publisher.split('      - name: Sticky comment with image tags\n')[1];
    const shell = step.split('        run: |\n')[1].split('\n')
        .map(line => line.startsWith('          ') ? line.slice(10) : line).join('\n');
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'pr-cloud-comment-'));
    try {
        fs.writeFileSync(path.join(dir, 'gh'), `#!/usr/bin/env bash
if [[ "$*" == *'-X GET'* ]]; then
  printf '%s' "$COMMENT_ID"
else
  printf '%s' "$*" > "$PWD/request"
  cat > "$PWD/payload.json"
fi
`, { mode: 0o755 });
        for (const commentId of ['', '99']) {
            for (const branch of ['feat/My_branch.with-dots', 'feature/' + 'a-'.repeat(100), '._/☃']) {
                const head = { sha: 'a'.repeat(40), ref: branch, repo: { full_name: 'dotcms/core' } };
                const { calls } = await deploy({ pr: { head }, mutationStatus: 201 });
                assert.equal(calls[1].body.env.env_key, '1234');
                const mutable = calls[1].body.env.image;
                execFileSync('bash', ['-e', '-c', shell], { cwd: dir, env: {
                    ...process.env, PATH: `${dir}:${process.env.PATH}`, COMMENT_ID: commentId,
                    REPO: 'dotcms/core', PR: '1234', SHA: head.sha, BRANCH: branch,
                    TAGS: `${mutable}\n${mutable}_aaaaaaa`, GH_TOKEN: 'fake-github-token',
                } });
                const { body } = JSON.parse(fs.readFileSync(path.join(dir, 'payload.json'), 'utf8'));
                assert.match(body, /<!-- pr-docker-image-sticky -->/);
                assert.ok(body.includes(`docker pull ${mutable}\n`));
                assert.ok(body.includes(`docker pull ${mutable}_aaaaaaa\n`));
                assert.ok(body.includes('https://cloud.dotcms.dev/admin/envs/dotpr%2F1234)'));
                assert.match(body, /[Ss]ign in.*Cloud Control/);
                assert.match(body, /permissions/);
                assert.doesNotMatch(body, /fake-github-token|test-token/);
                const request = fs.readFileSync(path.join(dir, 'request'), 'utf8');
                assert.ok(request.includes(commentId ? '-X PATCH repos/dotcms/core/issues/comments/99'
                    : '-X POST repos/dotcms/core/issues/1234/comments'));
            }
        }
    } finally {
        fs.rmSync(dir, { recursive: true, force: true });
    }
});

test('closed, unlabeled, fork and stale PRs cannot use the Cloud Control token', async () => {
    const head = { sha: 'a'.repeat(40), ref: 'feature/x', repo: { full_name: 'other/fork' } };
    for (const pr of [{ state: 'closed' }, { labels: [] }, { head },
        { head: { ...head, sha: 'b'.repeat(40), repo: { full_name: 'dotcms/core' } } }]) {
        assert.equal((await deploy({ pr })).calls.length, 0);
    }
});

test('manifest must match the PR build run, but label runs use their manifest SHA', async () => {
    assert.equal((await deploy({ run: { head_sha: 'b'.repeat(40) } })).calls.length, 0);
    const { calls } = await deploy({ run: { event: 'pull_request_target', head_sha: 'b'.repeat(40) }, existing: stopped });
    assert.equal(calls.length, 2);
});

test('invalid manifest is rejected before Cloud Control access', async () => {
    for (const metadata of [{ pr: '../bad' }, { pr: 0 }, { sha: 'not-a-sha' }]) {
        await assert.rejects(deploy({ metadata }), /manifest/i);
    }
});

test('API errors are not mistaken for missing environments or leaked in logs', async () => {
    for (const status of [401, 403, 404, 429, 500]) {
        await assert.rejects(deploy({ status }), new RegExp(`HTTP ${status}`));
    }
    await assert.rejects(deploy({ existing: stopped, mutationStatus: 403 }), /HTTP 403/);
});

test('a concurrent create rechecks the environment instead of overwriting it', async () => {
    const { calls } = await deploy({ mutationStatus: 409,
        afterConflict: { ...stopped, deploy_status: 'deployed', replicas: 1 } });
    assert.deepEqual(calls.map(call => call.method), ['GET', 'POST', 'GET']);
});
