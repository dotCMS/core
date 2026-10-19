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
async function runRunner({ login, token, failAt, abortAt, collectionError, newmanError, newmanThrow,
  newmanSummary, deferFailureExit = false, emptyGroup = false } = {}) {
  const calls = [], newmanRuns = [], logs = [], exits = [], timers = new Map();
  const scheduledTimers = [], aborts = [], summaries = [], writes = [];
  let timerId = 0;
  const argv = {
    _: [emptyGroup ? 'default' : 'probe'], serverUrl: 'http://dotcms.invalid', deferFailureExit,
    postmanTestsDir: '/virtual/collections', configFilePath: '/virtual/config.json',
    postmanTestsResultsDir: '/virtual/results',
  };
  const cli = { argv };
  for (const method of ['command', 'demandCommand', 'option', 'help']) cli[method] = () => cli;
  const modules = {
    fs: { existsSync: (file) => !(emptyGroup && file.endsWith('/default.json')),
      readdirSync: () => [], readFileSync: () => '[]', writeFileSync: (...args) => writes.push(args) },
    path,
    'yargs/yargs': () => cli,
    'yargs/helpers': { hideBin: (args) => args.slice(2) },
    newman: { run(config, callback) {
      newmanRuns.push(config);
      if (newmanThrow) throw newmanThrow;
      callback(newmanError || null, newmanSummary || { run: { failures: [], executions: [{}] } });
    } },
    xml2js: { Builder: class { buildObject(value) {
      summaries.push(JSON.parse(JSON.stringify(value['failsafe-summary'])));
      return '<failsafe-summary/>';
    } } },
    '/virtual/collections/probe.json': { info: { name: 'probe' }, item: [{
      name: 'protected', request: { url: '{{serverURL}}/api/v1/site/currentSite',
        auth: { type: 'bearer', bearer: [{ key: 'token', value: '{{jwt}}' }] } },
    }] },
  };
  await vm.runInNewContext(source, {
    __dirname: runnerDir, Buffer, URL, Headers, Request, Response, AbortController,
    require(name) {
      if (name === '/virtual/collections/probe.json' && collectionError) throw collectionError;
      assert.ok(Object.hasOwn(modules, name), `Unexpected runner dependency: ${name}`);
      return modules[name];
    },
    process: { argv: ['node', 'index.js', 'probe'], env: {}, chdir() {}, exit(code) { exits.push(code); } },
    console: Object.fromEntries(['log', 'warn', 'error'].map((level) => [level, (...args) => logs.push(format(...args))])),
    setTimeout(callback, delay) {
      scheduledTimers.push(delay);
      timers.set(++timerId, { callback, delay });
      return timerId;
    },
    clearTimeout(id) { timers.delete(id); },
    async fetch(input, options) {
      const request = new Request(input, options);
      calls.push(request);
      const stage = request.url.endsWith('/authentication') ? 'login' : 'token';
      if (abortAt === stage) {
        for (const timer of timers.values()) timer.callback();
        aborts.push({ stage, aborted: request.signal.aborted, activeTimers: timers.size });
        throw Object.assign(new Error('request aborted'), { name: 'AbortError' });
      }
      if (failAt === stage) throw Object.assign(new Error('synthetic connection failure'), {
        request: { headers: { Cookie: `JSESSIONID=${session}`, Authorization: `Bearer ${jwt}` } },
      });
      if (stage === 'login') return login ? login() : Response.json({ entity: { userId: 'dotcms.org.1', emailAddress: 'admin@dotcms.com' } }, { headers: loginCookies });
      assert.ok(request.url.endsWith('/apitoken'), 'Only designated login and API-token routes are allowed');
      return token ? token() : Response.json({ entity: { jwt } });
    },
  }, { filename: path.join(runnerDir, 'index.js') });
  return { calls, newmanRuns, logs: logs.join('\n'), exits, timers, scheduledTimers, aborts, summaries, writes };
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
  assert.equal(config.reporter.cli.noConsole, true, 'Collection debug logs can contain token response bodies');
  assert.equal(config.reporter.cli.silent, true, 'CLI assertion output can include secret response values');
  assertNoSecrets(result.logs);
  assert.equal(result.timers.size, 0);
  assert.ok(!result.exits.some((code) => code !== 0));
  assert.equal(result.summaries.length, 1);
  assert.equal(result.summaries[0].$.result, 0);
  assert.equal(result.summaries[0].completed, 1);
  assert.equal(result.summaries[0].errors, 0);
  assert.equal(result.summaries[0].failures, 0);
});

test('successful CLI execution never logs the returned API JWT', async () => {
  assertNoSecrets((await runRunner()).logs);
});

const executionFailures = [
  ['missing collection', { collectionError: Object.assign(new Error(jwt), { code: 'MODULE_NOT_FOUND' }) }, 0, 1, 0],
  ['malformed collection', { collectionError: new SyntaxError(jwt) }, 0, 1, 0],
  ['synchronous Newman throw', { newmanThrow: new Error(jwt) }, 0, 1, 0],
  ['Newman callback error', { newmanError: new Error(jwt) }, 0, 1, 0],
  ['IP callback error is not success', { newmanError: Object.assign(new Error(jwt), { code: 'ERR_INVALID_IP_ADDRESS' }) }, 0, 1, 0],
  ['assertion failure', { newmanSummary: { run: { executions: [{}], failures: [{ error: { message: jwt } }] } } }, 1, 0, 1],
  ['zero executions', { newmanSummary: { run: { executions: [], failures: [] } } }, 0, 1, 0],
  ['empty collection selection', { emptyGroup: true }, 0, 1, 0],
];
for (const [name, scenario, completed, errors, failures] of executionFailures) {
  for (const deferFailureExit of [false, true]) test(`${name}: accurate summary and ${deferFailureExit ? 'Maven-deferred' : 'direct failing'} exit`, async () => {
    const result = await runRunner({ ...scenario, deferFailureExit });
    assert.equal(result.summaries.length, 1);
    const summary = result.summaries[0];
    assert.equal(summary.$.result, 255, 'Errors/failures/empty runs cannot report XML success');
    assert.equal(summary.completed, completed);
    assert.equal(summary.errors, errors, 'Count each error exactly once, including load/sync errors');
    assert.equal(summary.failures, failures);
    assert.ok(result.writes.some(([file]) => file.endsWith('/failsafe-summary.xml')));
    assert.equal(result.exits.some((code) => code !== 0), !deferFailureExit,
      'Only the Maven invocation defers failure until verify, after container cleanup');
    assertNoSecrets(result.logs);
  });
}

test('Maven bootstrap errors are recorded and deferred, never silently successful', async () => {
  const result = await runRunner({ failAt: 'login', deferFailureExit: true });
  assert.equal(result.summaries.length, 1);
  assert.equal(result.summaries[0].$.result, 255);
  assert.equal(result.summaries[0].errors, 1);
  assert.equal(result.summaries[0].completed, 0);
  assert.ok(!result.exits.some((code) => code !== 0));
  assertNoSecrets(result.logs);
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
    if (scenario.abortAt) {
      assert.equal(result.aborts.length, 1, 'Reach the requested bootstrap timeout stage');
      assert.equal(result.aborts[0].stage, scenario.abortAt);
      assert.ok(result.aborts[0].activeTimers > 0, 'Schedule an abort timeout before fetching');
      assert.equal(result.aborts[0].aborted, true, 'The scheduled callback must abort the fetch signal');
      assert.ok(result.scheduledTimers.length > 0);
      assert.ok(result.scheduledTimers.every((delay) => Number.isFinite(delay) && delay > 0 && delay <= 30000),
        'Bootstrap requests need finite, positive timeouts bounded by 30 seconds');
    }
    assert.equal(result.calls.length, expectedCalls, 'Stop immediately at failed bootstrap stage');
    assert.equal(result.newmanRuns.length, 0, 'Never run collections with missing/invalid authentication');
    assert.ok(result.exits.some((code) => code !== 0), 'Bootstrap failure must fail the CLI');
    assert.match(result.logs, /error|fail|timed?\s*out/i, 'Report an actionable bootstrap error');
    assertNoSecrets(result.logs);
    assert.equal(result.timers.size, 0, 'Clear abort timers on failure as well as success');
  });
}
