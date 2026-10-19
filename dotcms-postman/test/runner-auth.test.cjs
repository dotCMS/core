// Run after developer approval: node --test dotcms-postman/test/*.test.cjs (Node 22).
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const { format } = require('node:util');
const vm = require('node:vm');

const runnerDir = path.resolve(__dirname, '..');
const source = readFileSync(path.join(runnerDir, 'index.js'), 'utf8');
const jwt = 'synthetic.api.jwt';
const session = 'synthetic-session.route1';
const loginCookies = [
  ['set-cookie', 'access_token=synthetic-login-token; Expires=Wed, 21 Oct 2037 07:28:00 GMT; HttpOnly'],
  ['set-cookie', `JSESSIONID=${session}; Path=/; HttpOnly; SameSite=Lax`],
];

// Execute the WHOLE CLI, not a copied/extracted fetchJWT; no packages, network or disk writes.
async function runRunner({ login, token, failAt, abortAt } = {}) {
  const calls = [], newmanRuns = [], logs = [], exits = [], timers = new Map();
  let timerId = 0;
  const argv = {
    _: ['probe'], serverUrl: 'http://dotcms.invalid',
    postmanTestsDir: '/virtual/collections', configFilePath: '/virtual/config.json',
    postmanTestsResultsDir: '/virtual/results',
  };
  const cli = { argv };
  for (const method of ['command', 'demandCommand', 'option', 'help']) cli[method] = () => cli;
  const modules = {
    fs: { existsSync: () => true, readFileSync: () => '[]', writeFileSync() {} },
    path,
    'yargs/yargs': () => cli,
    'yargs/helpers': { hideBin: (args) => args.slice(2) },
    newman: { run(config, callback) {
      newmanRuns.push(config);
      callback(null, { run: { failures: [], executions: [{}] } });
    } },
    xml2js: { Builder: class { buildObject() { return '<failsafe-summary/>'; } } },
    '/virtual/collections/probe.json': { info: { name: 'probe' }, item: [{
      name: 'protected', request: { url: '{{serverURL}}/api/v1/site/currentSite',
        auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{jwt}}' }] } },
    }] },
  };
  await vm.runInNewContext(source, {
    __dirname: runnerDir, Buffer, URL, Headers, Request, Response, AbortController,
    require(name) {
      assert.ok(Object.hasOwn(modules, name), `Unexpected runner dependency: ${name}`);
      return modules[name];
    },
    process: { argv: ['node', 'index.js', 'probe'], env: {}, chdir() {}, exit(code) { exits.push(code); } },
    console: Object.fromEntries(['log', 'warn', 'error'].map((level) => [level, (...args) => logs.push(format(...args))])),
    setTimeout(callback, delay) { timers.set(++timerId, { callback, delay }); return timerId; },
    clearTimeout(id) { timers.delete(id); },
    async fetch(input, options) {
      const request = new Request(input, options);
      calls.push(request);
      const stage = request.url.endsWith('/authentication') ? 'login' : 'token';
      if (abortAt === stage) {
        for (const timer of timers.values()) timer.callback();
        assert.ok(request.signal.aborted, 'Bootstrap timeout must abort the fetch');
        throw Object.assign(new Error('request aborted'), { name: 'AbortError' });
      }
      if (failAt === stage) throw Object.assign(new Error('synthetic connection failure'), {
        request: { headers: { Cookie: `JSESSIONID=${session}`, Authorization: `Bearer ${jwt}` } },
      });
      if (stage === 'login') return login ? login() : Response.json({ entity: { user: {} } }, { headers: loginCookies });
      assert.ok(request.url.endsWith('/apitoken'), 'Only designated login and API-token routes are allowed');
      return token ? token() : Response.json({ entity: { jwt } });
    },
  }, { filename: path.join(runnerDir, 'index.js') });
  return { calls, newmanRuns, logs: logs.join('\n'), exits, timers };
}

function assertNoSecrets(logs) {
  for (const secret of [jwt, session, 'synthetic-login-token', 'admin', 'admin@dotcms.com',
    Buffer.from('admin@dotcms.com:admin').toString('base64')]) {
    assert.ok(!logs.includes(secret), 'Runner must not log credentials, JWTs or session cookies');
  }
  assert.doesNotMatch(logs, /["']?password["']?\s*[:=]|Authorization\s*[:=]/i);
}

test('JSON login -> isolated JSESSIONID -> API JWT -> Bearer-only Newman environment', async () => {
  const result = await runRunner();
  assert.deepEqual(result.calls.map((r) => new URL(r.url).pathname), [
    '/api/v1/authentication', '/api/v1/apitoken',
  ], 'Password Basic must not bootstrap API-token issuance');
  const [login, token] = result.calls;
  for (const request of result.calls) {
    assert.equal(request.method, 'POST');
    assert.equal(request.redirect, 'error', 'Do not forward credentials/session to redirect destinations');
    assert.equal(request.headers.get('authorization'), null);
    assert.match(request.headers.get('content-type'), /application\/json/);
  }
  assert.equal(login.headers.get('cookie'), null);
  const credentials = await login.json();
  assert.equal(credentials.userId, 'admin@dotcms.com');
  assert.equal(credentials.password, 'admin');
  assert.equal(credentials.backEndLogin, true);
  assert.equal(token.headers.get('cookie'), `JSESSIONID=${session}`,
    'Forward only the rotated servlet session, not access_token, attributes or Expires');
  assert.deepEqual(await token.json(), {
    expirationSeconds: 60000, userId: 'dotcms.org.1', network: '0.0.0.0/0',
    claims: { label: 'postman-tests' },
  });
  assert.equal(result.newmanRuns.length, 1);
  const config = result.newmanRuns[0];
  assert.deepEqual(JSON.parse(JSON.stringify(config.envVar)), [
    { key: 'serverURL', value: 'http://dotcms.invalid' }, { key: 'jwt', value: jwt },
  ]);
  assert.ok(!JSON.stringify(config).includes(session), 'No bootstrap session in Newman configuration');
  assert.ok(!JSON.stringify(config).includes('synthetic-login-token'));
  assert.equal(config.workingDir, '/virtual/collections', 'Keep multipart upload fixture resolution');
  assert.notEqual(config.reporter.cli.showRequestData, true, 'Verbose request data can expose Authorization');
  assertNoSecrets(result.logs);
  assert.equal(result.timers.size, 0);
  assert.ok(!result.exits.some((code) => code !== 0));
});

test('successful CLI execution never logs the returned API JWT', async () => {
  assertNoSecrets((await runRunner()).logs);
});

const failures = [
  ['login 401', { login: () => Response.json({ error: jwt, detail: session }, { status: 401 }) }, 1],
  ['login redirect', { login: () => new Response(null, { status: 302, headers: { location: 'https://elsewhere.invalid' } }) }, 1],
  ['missing session', { login: () => Response.json({ entity: {} }) }, 1],
  ['blank session', { login: () => Response.json({ entity: {} }, { headers: { 'set-cookie': 'JSESSIONID=; Path=/' } }) }, 1],
  ['malformed login JSON', { login: () => new Response('{', { headers: loginCookies }) }, 1],
  ['login connection failure', { failAt: 'login' }, 1],
  ['login timeout', { abortAt: 'login' }, 1],
  ['token 401', { token: () => Response.json({ error: jwt, detail: session }, { status: 401 }) }, 2],
  ['token redirect', { token: () => new Response(null, { status: 302, headers: { location: 'https://elsewhere.invalid' } }) }, 2],
  ['malformed token JSON', { token: () => new Response('{') }, 2],
  ['legacy login token is not API JWT', { token: () => Response.json({ entity: { token: jwt } }) }, 2],
  ['missing token entity', { token: () => Response.json({}) }, 2],
  ['empty API JWT', { token: () => Response.json({ entity: { jwt: '' } }) }, 2],
  ['non-string API JWT', { token: () => Response.json({ entity: { jwt: {} } }) }, 2],
  ['token connection failure', { failAt: 'token' }, 2],
  ['token timeout', { abortAt: 'token' }, 2],
];
for (const [name, scenario, expectedCalls] of failures) {
  test(`bootstrap fails closed: ${name}`, async () => {
    const result = await runRunner(scenario);
    assert.equal(result.calls.length, expectedCalls, 'Stop immediately at failed bootstrap stage');
    assert.equal(result.newmanRuns.length, 0, 'Never run collections with missing/invalid authentication');
    assert.ok(result.exits.some((code) => code !== 0), 'Bootstrap failure must fail the CLI');
    assert.match(result.logs, /error|fail|timed?\s*out/i, 'Report an actionable bootstrap error');
    assertNoSecrets(result.logs);
    assert.equal(result.timers.size, 0, 'Clear abort timers on failure as well as success');
  });
}
