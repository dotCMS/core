// Static migration guards, not proof of live modes, cookie behavior or token subjects.
// ALL-only negatives and JWT_TOKEN DAV still need isolated server/CI selection and NONE denial runs.
const assert = require('node:assert/strict');
const { readdirSync, readFileSync, existsSync } = require('node:fs');
const { createHash } = require('node:crypto');
const path = require('node:path');
const { test } = require('node:test');
const vm = require('node:vm');

const root = path.resolve(__dirname, '../src/main/resources/postman');
const requests = [], scripts = [];
function visit(node, file, names = [], inherited, variables = {}) {
  const auth = node.auth ?? inherited;
  variables = { ...variables, ...Object.fromEntries((node.variable || []).map((v) => [v.key, v.value])) };
  for (const event of node.event || []) scripts.push({ file, names, event });
  if (node.request) requests.push({ file, names, node, request: node.request,
    auth: node.request.auth ?? auth, variables });
  for (const item of node.item || []) visit(item, file, [...names, item.name], auth, variables);
}
function discover(dir) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const file = path.join(dir, entry.name);
    // Match runnable inventory, plus relocated compatibility collections, not documentation/fixtures.
    if (entry.isDirectory() && !['documentation', 'resources'].includes(entry.name)) discover(file);
    if (!entry.isFile() || !entry.name.endsWith('.json')) continue;
    const collection = JSON.parse(readFileSync(file, 'utf8'));
    if (collection.info && collection.item) visit(collection, path.relative(root, file));
  }
}
discover(root);
const label = (r) => `${r.file}: ${r.names.join(' / ')}`;
const value = (auth, key) => auth?.[auth.type]?.find((entry) => entry.key === key)?.value;
const url = (r) => typeof r.request.url === 'string' ? r.request.url : r.request.url?.raw || '';
const eventCode = (event) => Array.isArray(event.script?.exec) ? event.script.exec.join('\n') : event.script?.exec || '';
const environment = Object.fromEntries(JSON.parse(readFileSync(path.join(root, 'postman_environment.json'), 'utf8'))
  .values.map((v) => [v.key, v.value]));
function resolve(text, variables) {
  for (let i = 0; i < 5; i++) text = String(text ?? '').replace(/\{\{([^{}]+)\}\}/g,
    (match, key) => variables[key] ?? match);
  return text;
}
const effectiveVariables = (r) => ({ ...r.variables, ...environment, jwt: 'synthetic.runner.jwt' });
const manualAuth = (r) => (r.request.header || []).filter((h) => !h.disabled && /^authorization$/i.test(h.key));
function find(file, names) {
  const matches = requests.filter((r) => path.basename(r.file) === file && r.names.join(' / ') === names);
  assert.equal(matches.length, 1, `Preserve request: ${file}: ${names}`);
  return matches[0];
}
const jwtOnlyNegatives = [
  ['Push_Publish_JWT_Token_Test.postman_collection.json', 'using_basic_authentication_instead_JWT_token / request_pp'],
  ['Integrity_Checker_JWT_Token_Test.postman_collection.json', 'generate / using_basic_authentication_instead_JWT_token / request_pp'],
];
const isJwtOnlyNegative = (r) => jwtOnlyNegatives.some(([file, names]) =>
  path.basename(r.file) === file && r.names.join(' / ') === names);

const davPaths = [
  'Test WebDav conection / List directories', 'Create folder / Create new folder',
  'Create folder / Validate folder has been created', 'Upload files to the folder / Upload a new file with content',
  'Upload files to the folder / Validate file has been created', 'Upload files to the folder / Upload a new file without content',
  'Upload files to the folder / Validate the empty file has been created', 'Update file / Update the file',
  'Update file / Validate file has been updated', 'Delete file / Delete the file', 'Delete file / Validate file has been deleted',
  'Delete the folder / Delete the folder', 'Delete the folder / Validate folder has been deleted',
  ...['Create the folder', 'Create a temp folder', 'Upload a file into the temp folder',
    'Copy to a dot-dot destination is rejected', 'Move to a dot-dot destination is rejected',
    'Validate the source is still in place', 'Copy to a plain name', 'Validate the copy has the source content',
    'Move to a plain name', 'Validate the moved file exists', 'Delete the folder']
    .map((name) => `Copy and move in temp storage / ${name}`),
];
const isDavCompatibility = (r) => path.basename(r.file) === 'WebDav.postman_collection.json' &&
  davPaths.includes(r.names.join(' / ')) && /\/webdav\//i.test(url(r));
const davMethods = ['OPTIONS', 'PROPFIND', 'PROPPATCH', 'LOCK', 'UNLOCK', 'COPY', 'MOVE', 'MKCOL', 'GET', 'HEAD', 'PUT', 'DELETE'];
const isDavDenial = (r) => r.file === 'WebDav_Basic_Denied.postman_collection.json' &&
  davMethods.some((method) => ['password', 'api-jwt'].some((credential) => r.names.join(' / ') === `${method} / ${credential}`));
const modeControls = [
  ['compatibility/all/Push_Publish_JWT_Token_Test.postman_collection.json', 'ALL accepts a valid user password on REST', 200],
  ['compatibility/jwt-token/WebDav.postman_collection.json', 'JWT_TOKEN rejects a user password on REST', 401],
];

test('every ordinary configured collection resolves to an existing file', () => {
  const config = JSON.parse(readFileSync(path.resolve(__dirname, '../config.json'), 'utf8'));
  const topLevel = new Set(readdirSync(root));
  for (const group of config) for (const collection of group.collections) {
    assert.ok(topLevel.has(`${collection}.json`), `Missing configured collection: ${collection}`);
  }
});

test('moved ALL upload fixtures still resolve real original payloads from Newman workingDir', () => {
  const uploads = requests.filter((r) => r.file.startsWith('compatibility/all/')).flatMap((r) => {
    const files = r.request.body?.mode === 'file' ? [r.request.body.file] :
      (r.request.body?.formdata || []).filter((f) => f.type === 'file');
    return files.map((f) => ({ r, src: f.src }));
  });
  assert.equal(uploads.length, 2);
  for (const { r, src } of uploads) {
    const file = path.resolve(path.dirname(path.join(root, r.file)), src);
    assert.ok(existsSync(file), `Missing real upload payload: ${r.file}: ${src}`);
    assert.equal(file, path.join(root, 'resources/Push_publish/bundle_test-01EAJHA26FZGPAEZBSGJGNG82A.tar.gz'));
    assert.ok(readFileSync(file).length > 0);
  }
});

for (const file of ['PagesResourceTests.json', 'resources/PagesResourceTests.json']) {
  test(`${file} logout/cache sequence retains supplied JWT while remaining anonymous and cookie-free`, () => {
    const collection = JSON.parse(readFileSync(path.join(root, file), 'utf8'));
    const logout = collection.item.find((i) => i.name === 'invalidateSession');
    const values = new Map([['jwt', 'synthetic.supplied.jwt'], ['serverURL', 'http://dotcms.invalid']]);
    const environment = { get: (key) => values.get(key), unset: (key) => values.delete(key) };
    let clears = 0;
    const pm = { environment, test() {}, cookies: { jar: () => ({ clear(url, callback) {
      assert.equal(url, 'http://dotcms.invalid'); clears++; callback(null);
    } }) } };
    for (const event of logout.event || []) vm.runInNewContext(eventCode(event), { pm });
    assert.equal(clears, 1, 'Logout clears its ambient cookie jar');
    assert.equal(values.get('jwt'), 'synthetic.supplied.jwt', 'Logout must not remove the API JWT needed by collection guards');
    for (const event of collection.event || []) if (event.listen === 'prerequest') {
      vm.runInNewContext(eventCode(event), { pm: { environment } });
    }
    const allItems = (node) => [node, ...(node.item || []).flatMap(allItems)];
    const anonymous = allItems(collection).filter((i) => ['invalidateSession', 'Check Cache-Control'].includes(i.name));
    assert.ok(anonymous.length >= 3);
    for (const item of anonymous) {
      assert.equal(item.request.auth?.type, 'noauth', item.name);
      assert.match((item.event || []).filter((e) => e.listen === 'prerequest').map(eventCode).join('\n'),
        /pm\.cookies\.jar\(\)\.clear\(/, 'Anonymous request must not inherit an earlier session');
    }
    const cache = collection.item.find((i) => i.name === 'Check Cache-Control');
    assert.match((cache.event || []).map(eventCode).join('\n'), /header\(['"]Cache-Control['"],\s*['"]max-age=100['"]\)/);
  });
}

test('Maven defers failed CLI exit until Docker stop and JaCoCo dump have run', () => {
  const pom = readFileSync(path.resolve(__dirname, '../pom.xml'), 'utf8');
  assert.ok(pom.includes('run start --deferFailureExit'), 'Defer only the Maven invocation, never the default CLI');
  assert.ok(pom.indexOf('<artifactId>docker-maven-plugin</artifactId>') < pom.indexOf('<artifactId>maven-failsafe-plugin</artifactId>'),
    'Docker stop runs in verify after JaCoCo post-integration-test, before failsafe:verify can fail the build');
  assert.match(pom, /<id>stop<\/id>[\s\S]*?<phase>verify<\/phase>/);
});

test('Postman CI executes runner and migration guards before live collections without caching them', () => {
  const moduleRoot = path.resolve(__dirname, '..');
  const manifest = JSON.parse(readFileSync(path.join(moduleRoot, 'package.json'), 'utf8'));
  assert.equal(manifest.scripts.test, 'node --test test/*.test.cjs');
  const pom = readFileSync(path.join(moduleRoot, 'pom.xml'), 'utf8');
  const execution = pom.split('<execution>').find((entry) => entry.includes('<id>run node unit tests</id>'))
    ?.split('</execution>')[0];
  assert.ok(execution, 'Bind the existing Node guards into the Postman Maven lifecycle');
  assert.match(execution, /<phase>test<\/phase>/);
  assert.match(execution, /<commandlineArgs>run test<\/commandlineArgs>/);
  assert.match(execution, /<skip>\$\{postman\.test\.skip\}<\/skip>/);
  const matrix = readFileSync(path.resolve(moduleRoot, '../.github/test-matrix.yml'), 'utf8');
  const postman = matrix.split('  postman:')[1]?.split('\n  karate:')[0] || '';
  const commands = [...postman.matchAll(/base_maven_args: >-\n((?: {8,}.*\n)+)/g)]
    .map((match) => match[1]);
  assert.ok(commands.length >= 3, 'Cover the ordinary defaults and both compatibility server commands');
  for (const command of commands) assert.match(command, /-Dmaven\.build\.cache\.enabled=false/,
    'The cache must not skip the guard execution or live Postman tests');
});

test('ordinary effective auth and manual headers have no password-Basic dependency', () => {
  assert.ok(requests.length >= 2915, 'Do not pass by silently dropping collections or requests');
  const violations = requests.filter((r) => {
    // DAV is a separate JWT_TOKEN compatibility protocol; these two negatives belong in isolated ALL.
    if (isDavCompatibility(r) || isJwtOnlyNegative(r) || isDavDenial(r) ||
      modeControls.some(([file, name]) => r.file === file && r.names.join(' / ') === name)) return false;
    return r.auth?.type === 'basic' || manualAuth(r).some((h) => {
      const header = resolve(h.value, effectiveVariables(r));
      return /\{\{/.test(header) || !/^Bearer\s+\S+$/i.test(header);
    });
  });
  assert.equal(violations.length, 0, `Migrate inherited/request/manual auth, not just roots:\n${violations.map(label).join('\n')}`);
});

test('representative protected requests keep effective nonempty runner Bearer, not noauth or deleted auth', () => {
  // Positive coverage is deliberately representative; the no-Basic scan alone cannot prove every migration.
  for (const [file, names] of [
    ['LayoutResource.postman_collection.json', 'Layouts / US1 - list sections as admin - 200'],
    ['Tags_Resource_V2.postman_collection.json', 'Create test Site for Tags'],
    ['Template_Resource.postman_collection.json', 'SystemTemplate / GetAllIncludingSystemTemplate'],
    ['NavResourceTests.json', 'loadJson / SuccessRequest'],
    ['NavResourceTests.json', 'unpublished-items / Request Nav - Unpublished test page'],
    ['JobQueueResourceAPITests.postman_collection.json', 'List Jobs'],
    ['BulkUploadResource.postman_collection.json', 'Setup / Get default site'],
    ['BulkRefreshResource.postman_collection.json', 'pre-execution-scripts / Find a Contentlet to Refresh'],
    ['DotFavoritePage.postman_collection.json', 'CreateLimitedUser'],
  ]) {
    const r = find(file, names);
    assert.equal(r.auth?.type, 'bearer', label(r));
    assert.equal(resolve(value(r.auth, 'token'), effectiveVariables(r)), 'synthetic.runner.jwt', label(r));
    for (const header of manualAuth(r)) {
      assert.equal(resolve(header.value, effectiveVariables(r)), 'Bearer synthetic.runner.jwt', label(r));
    }
  }
});

test('collection/folder/request scripts do not silently bootstrap or send Basic', () => {
  const violations = scripts.filter(({ event }) =>
    /["'`]Basic\s|["']?\btype["']?\s*:\s*["']basic["']/i.test(eventCode(event)));
  assert.equal(violations.length, 0, `Migrate scripted requests too:\n${violations.map(label).join('\n')}`);
});

for (const file of ['JobQueueResourceAPITests', 'BulkUploadResource', 'BulkRefreshResource']) {
  test(`${file} honors an environment API JWT without a collection-scoped fallback request`, () => {
    let sends = 0;
    const logs = [];
    const supplied = 'synthetic.environment.jwt';
    const get = (key) => key === 'jwt' ? supplied : key === 'serverURL' ? 'http://dotcms.invalid' : undefined;
    for (const { event } of scripts.filter((s) => path.basename(s.file) === `${file}.postman_collection.json` &&
      s.names.length === 0 && s.event.listen === 'prerequest')) {
      vm.runInNewContext(eventCode(event), { Buffer,
        console: { log: (...args) => logs.push(args.join(' ')) },
        pm: { environment: { get }, variables: { get },
          collectionVariables: { get: () => undefined, set() {} },
          sendRequest() { sends++; } },
      });
    }
    assert.equal(sends, 0, 'Supplied environment jwt must suppress bootstrap, even with no collection jwt');
    assert.ok(!logs.join('\n').includes(supplied), 'Do not log supplied API JWT');
  });
}

test('workflow resolves exact starter emails and provisions canonical chris/bill API JWTs', async () => {
  const vars = { jwt: 'synthetic.runner.jwt', serverURL: 'http://dotcms.invalid' };
  const subjects = [], sent = [];
  const scope = { get: (key) => vars[key], set: (key, val) => { vars[key] = val; } };
  const pm = { environment: scope, collectionVariables: scope, variables: scope,
    sendRequest(request, callback) {
      sent.push(request);
      let data;
      if (request.method === 'GET') {
        const email = new URL(request.url).searchParams.get('query');
        const actor = email.split('@')[0];
        data = { entity: [{}, { id: 'incomplete-user', emailAddress: null },
          { id: 'dotcms.org.1', emailAddress: 'admin@dotcms.com' },
          { id: `canonical-${actor}-id`, emailAddress: email }] };
      } else {
        const subject = JSON.parse(request.body.raw).userId;
        subjects.push(subject);
        data = { entity: { jwt: `synthetic.${subject}.jwt` } };
      }
      callback(null, { code: 200, json: () => data });
    },
  };
  for (const { event } of scripts.filter((s) => path.basename(s.file) === 'Workflow_Resource_Tests.json' &&
    s.names.length === 0 && s.event.listen === 'prerequest')) {
    await vm.runInNewContext(eventCode(event), { pm, encodeURIComponent });
  }
  assert.deepEqual(subjects.sort(), ['canonical-bill-id', 'canonical-chris-id']);
  for (const actor of ['chris', 'bill']) assert.equal(vars[`${actor}Jwt`], `synthetic.canonical-${actor}-id.jwt`);
  for (const request of sent) assert.equal(request.header.Authorization, 'Bearer synthetic.runner.jwt');
});

for (const fixture of [
  { name: 'empty starter', existing: [] },
  { name: 'existing Chris and missing Bill', existing: ['chris'] },
  { name: 'malformed user lookup', existing: ['chris'], failure: 'lookup' },
  { name: 'ambiguous user lookup', existing: ['chris'], failure: 'ambiguous' },
  { name: 'failed user creation', existing: ['chris'], failure: 'create' },
  { name: 'missing new-user identity', existing: ['chris'], failure: 'identity' },
  { name: 'missing workflow layout', existing: ['chris'], failure: 'layout' },
  { name: 'failed personal-role grant', existing: ['chris'], failure: 'grant' },
]) {
  test(`workflow bootstrap handles ${fixture.name} without elevating or changing existing actors`, async () => {
    const vars = { jwt: 'synthetic.runner.jwt', serverURL: 'http://dotcms.invalid' };
    const created = [], grants = [], subjects = [], sent = [];
    const scope = { get: (key) => vars[key], set: (key, value) => { vars[key] = value; },
      replaceIn: () => '00000000-0000-4000-8000-000000000001' };
    const pm = { environment: scope, collectionVariables: scope, variables: scope,
      sendRequest(request, callback) {
        sent.push(request);
        const url = new URL(request.url);
        const body = request.body?.raw ? JSON.parse(request.body.raw) : undefined;
        let code = 200, entity;
        if (url.pathname === '/api/v1/users/filter') {
          const email = url.searchParams.get('query');
          const actor = email.split('@')[0];
          entity = [{}, { emailAddress: null }, ...(fixture.existing.includes(actor) ?
            [{ id: `existing-${actor}-id`, emailAddress: email, roles: [{ id: 'keep-existing-role' }] }] : [])];
          if (actor === 'bill' && fixture.failure === 'lookup') entity = {};
          if (actor === 'bill' && fixture.failure === 'ambiguous') entity =
            [{ id: 'bill-one', emailAddress: email }, { id: 'bill-two', emailAddress: email }];
        } else if (url.pathname === '/api/v1/users' && request.method === 'POST') {
          created.push(body);
          const actor = body.email.split('@')[0];
          entity = fixture.failure === 'identity' ? {} :
            { userID: `created-${actor}-id`, roleId: `personal-${actor}-role` };
          if (fixture.failure === 'create') code = 500;
        } else if (url.pathname === '/api/v1/roles/layouts' && request.method === 'GET') {
          entity = [{ id: 'admin-layout', portletIds: ['roles', 'users'] },
            ...(fixture.failure === 'layout' ? [] : [{ id: 'workflow-layout', portletIds: ['workflow-schemes'] }])];
        } else if (url.pathname === '/api/v1/roles/layouts' && request.method === 'POST') {
          grants.push(body);
          entity = { savedLayouts: true };
          if (fixture.failure === 'grant') code = 403;
        } else if (url.pathname === '/api/v1/apitoken' && request.method === 'POST') {
          subjects.push(body.userId);
          entity = { jwt: `synthetic.${body.userId}.jwt` };
        } else assert.fail(`Unexpected fixture mutation: ${request.method} ${url.pathname}`);
        callback(null, { code, json: () => ({ entity }) });
      },
    };
    const bootstrap = scripts.find((s) => s.file === 'Workflow_Resource_Tests.json' &&
      s.names.length === 0 && s.event.listen === 'prerequest');
    const execute = async () => vm.runInNewContext(eventCode(bootstrap.event), { pm, encodeURIComponent });
    if (fixture.failure) {
      await assert.rejects(execute);
      assert.equal(created.length, ['lookup', 'ambiguous'].includes(fixture.failure) ? 0 : 1,
        'Create only when the lookup unambiguously proves the actor is missing');
      assert.equal(vars.billJwt, undefined, 'Never fall back to the runner token after setup fails');
      assert.ok(!subjects.includes('created-bill-id'), 'Do not issue a token before the new actor has workflow access');
    } else {
      await execute();
      const missing = ['chris', 'bill'].filter((actor) => !fixture.existing.includes(actor));
      assert.deepEqual(created.map((body) => body.email).sort(), missing.map((actor) => `${actor}@dotcms.com`).sort());
      for (const body of created) {
        assert.deepEqual(body.roles, ['DOTCMS_BACK_END_USER'], 'New actors are backend users, never administrators');
        assert.ok(body.password && body.password !== 'admin', 'Do not create a known-password account');
      }
      assert.deepEqual(grants.map((body) => body.roleId).sort(), missing.map((actor) => `personal-${actor}-role`).sort());
      for (const grant of grants) assert.deepEqual(grant.layoutIds, ['workflow-layout']);
      for (const actor of ['chris', 'bill']) {
        const id = `${fixture.existing.includes(actor) ? 'existing' : 'created'}-${actor}-id`;
        assert.ok(subjects.includes(id), 'Mint for the returned canonical identity');
        assert.equal(vars[`${actor}Jwt`], `synthetic.${id}.jwt`);
      }
      const calls = sent.length;
      await execute();
      assert.equal(sent.length, calls, 'Reuse the collection-owned actor tokens without reprovisioning');
    }
    for (const request of sent) assert.equal(request.header.Authorization, 'Bearer synthetic.runner.jwt');
  });
}

test('valid-Basic-vs-required-JWT negatives remain Basic and assert 401 (ALL-only boundary)', () => {
  for (const [file, names] of jwtOnlyNegatives) {
    const r = find(file, names);
    assert.equal(r.auth?.type, 'basic');
    assert.equal(value(r.auth, 'username').toLowerCase(), 'admin@dotcms.com');
    assert.equal(value(r.auth, 'password'), 'admin');
    assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/);
  }
});

test('ALL/JWT_TOKEN compatibility is outside ordinary discovery; NONE retains method-complete DAV denials', () => {
  for (const [file, names] of jwtOnlyNegatives) {
    const r = find(file, names);
    assert.ok(r.file.startsWith('compatibility/all/'));
    assert.match((r.node.event || []).filter((e) => e.listen === 'prerequest').map(eventCode).join('\n'),
      /pm\.cookies\.jar\(\)\.clear\(/, 'Clear sessions established by the ALL positive control');
  }
  for (const names of davPaths) assert.ok(find('WebDav.postman_collection.json', names).file.startsWith('compatibility/jwt-token/'));
  for (const [file, name, status] of modeControls) {
    const r = find(path.basename(file), name);
    assert.equal(r.file, file);
    assert.equal(r.auth?.type, 'basic');
    assert.equal(value(r.auth, 'password'), 'admin');
    assert.ok(/\/api\/v1\/site\/currentSite$/.test(url(r)));
    assert.match((r.node.event || []).map(eventCode).join('\n'), new RegExp(`\\.status\\(${status}\\)`));
  }
  const denials = requests.filter(isDavDenial);
  assert.equal(denials.length, davMethods.length * 2);
  for (const method of davMethods) for (const credential of ['password', 'api-jwt']) {
    const r = find('WebDav_Basic_Denied.postman_collection.json', `${method} / ${credential}`);
    assert.equal(r.request.method, method);
    assert.equal(r.auth?.type, 'basic');
    assert.equal(value(r.auth, 'username'), 'admin@dotcms.com');
    assert.equal(value(r.auth, 'password'), credential === 'password' ? 'admin' : '{{jwt}}');
    assert.ok(/\/webdav\//.test(url(r)));
    assert.equal(manualAuth(r).length, 0);
    assert.match((r.node.event || []).filter((e) => e.listen === 'prerequest').map(eventCode).join('\n'),
      /pm\.cookies\.jar\(\)\.clear\(/, 'A previous response must not mask a NONE denial with session auth');
    assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/);
  }
  const pom = readFileSync(path.resolve(__dirname, '../pom.xml'), 'utf8');
  assert.match(pom, /<postman.basic.auth.mode>NONE<\/postman.basic.auth.mode>/);
  assert.match(pom, /<DOT_ALLOW_BASIC_AUTH>\$\{postman.basic.auth.mode\}<\/DOT_ALLOW_BASIC_AUTH>/);
});

test('DAV CRUD keeps username/API-JWT auth, REST setup uses Bearer, no conflicting header', () => {
  const dav = davPaths.map((names) => find('WebDav.postman_collection.json', names));
  assert.equal(dav.length, 24, 'Keep all DAV operations, not just NONE denials');
  for (const r of dav) {
    assert.equal(r.auth?.type, 'basic', label(r));
    assert.equal(value(r.auth, 'username').toLowerCase(), 'admin@dotcms.com');
    assert.equal(value(r.auth, 'password'), '{{jwt}}', 'DAV must use API JWT as password in JWT_TOKEN mode');
    assert.ok(!(r.request.header || []).some((h) => !h.disabled && /^authorization$/i.test(h.key)), label(r));
  }
  const setup = find('WebDav.postman_collection.json', 'Test WebDav conection / SetSiteName');
  assert.equal(setup.auth?.type, 'bearer');
  assert.equal(value(setup.auth, 'token'), '{{jwt}}');
});

test('List directories resolves its expected href from the hostName variable, not a literal host', () => {
  const r = find('WebDav.postman_collection.json', 'Test WebDav conection / List directories');
  const events = (r.node.event || []).filter((e) => e.listen === 'test');
  assert.ok(events.length, 'Keep the List directories response test');
  // Run the real collection script. xml2Json is a structured stub so this isolates the href assertion, not XML parsing.
  const run = (hostName, responseHref) => {
    const failures = [], statuses = [];
    const replaceIn = (text) => String(text).replace(/\{\{([^{}]+)\}\}/g,
      (match, key) => (key === 'hostName' ? hostName : match));
    const response = { status: 207,
      text: () => `<D:multistatus><D:response><D:href>${responseHref}</D:href></D:response></D:multistatus>`,
      to: { have: { status: (code) => { statuses.push(code); assert.equal(response.status, code); } } } };
    const pm = { variables: { get: (key) => key === 'hostName' ? hostName : undefined, replaceIn },
      response, test: (name, fn) => { try { fn(); } catch (error) { failures.push({ name, error }); } },
      expect: (actual) => ({ to: { eql: (expected) => assert.strictEqual(actual, expected) } }) };
    const xml2Json = () => ({ 'd:multistatus': { 'd:response': [{ 'd:href': responseHref }] } });
    for (const event of events) vm.runInNewContext(eventCode(event), { pm, xml2Json });
    return { failures, statuses };
  };
  for (const hostName of ['default', 'custom.example.test', 'demo.dotcms.com']) {
    const { failures, statuses } = run(hostName, `/webdav/live/1/${hostName}/`);
    assert.deepEqual(statuses, [207], `Keep the 207 assertion for ${hostName}`);
    assert.deepEqual(failures, [], `Accept the ${hostName} site instead of the demo.dotcms.com literal`);
  }
  // Negative control: a response href that disagrees with the resolved variable must still fail.
  const wrong = run('custom.example.test', '/webdav/live/1/demo.dotcms.com/');
  assert.deepEqual(wrong.statuses, [207]);
  assert.equal(wrong.failures.length, 1, 'The XML href assertion must reject a mismatched response host');
  assert.match(wrong.failures[0].name, /Validate the XML response/);
});

test('representative noauth/invalid/expired/custom-token requests are not elevated to admin', () => {
  const anonymous = [
    ['GraphQLTests.json', 'Page API / Checking Exception Messages / GivenRequestByURI_Page_EditMode_WithoutAuth'],
    ['GraphQLTests.json', 'Disallow Introspection Query / Introspection Query Anonymous Request'],
    ...['list sections', 'create section', 'update section', 'delete section', 'reorder', 'set tools'].map((name) =>
      ['LayoutResource.postman_collection.json', `Unauthenticated (must run first, before any session cookie exists) / Unauthenticated - ${name} - 401`]),
    ['Push_Publish_JWT_Token_Test.postman_collection.json', 'fail_not_token_sent / request_pp'],
    ['Integrity_Checker_JWT_Token_Test.postman_collection.json', 'generate / fail_not_token_sent / request_integrity_generate'],
  ];
  for (const [file, names] of anonymous) assert.equal(find(file, names).auth?.type, 'noauth', names);
  const containers = find('GraphQLTests.json',
    'Page API / Checking Layout and Container Properties / Containers Property is Returned With Authenticated User');
  assert.equal(containers.auth?.type, 'bearer');
  assert.equal(value(containers.auth, 'token'), '{{jwt}}');
  for (const [file, names, token] of [
    ['Push_Publish_JWT_Token_Test.postman_collection.json', 'expired_token / request_pp', '{{token}}'],
    ['Push_Publish_JWT_Token_Test.postman_collection.json', 'fail_not_valid_token / request_pp', 'not_valid_token'],
    ['Integrity_Checker_JWT_Token_Test.postman_collection.json', 'generate / fail_not_valid_token / request_integrity_generate', 'not_valid_token'],
    ['Integrity_Checker_JWT_Token_Test.postman_collection.json', 'Fix conflicts / with_admin_user / request_fix_conflicts', '{{token}}'],
  ]) {
    const r = find(file, names);
    assert.equal(r.auth?.type, 'bearer');
    assert.equal(value(r.auth, 'token'), token, names);
  }
  const loggerAnonymous = find('Logger_Resource.postman_collection.json', 'Invalid User');
  assert.ok(!loggerAnonymous.auth || loggerAnonymous.auth.type === 'noauth', 'Logger Invalid User stays anonymous');
  assert.equal(manualAuth(loggerAnonymous).length, 0);
  const loggerInvalid = find('Logger_Resource.postman_collection.json', 'Invalid User Copy');
  assert.equal(loggerInvalid.auth?.type, 'bearer');
  assert.equal(manualAuth(loggerInvalid).length, 0, 'Do not override the invalid fixture with a manual header');
  assert.equal(createHash('sha256').update(value(loggerInvalid.auth, 'token')).digest('hex'),
    '62aacd04f5f4a4423ef2a6fd437a61e77cfbb005ce7d3cabec03b720e82bf0a4',
    'Retain the actual invalid JWT fixture, not noauth/empty/admin Bearer');
  for (const r of [loggerAnonymous, loggerInvalid]) {
    assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/);
  }
});

for (const [file, actor, consumer] of [
  ['VelocitySecrets.postman_collection.json', 'scripting', 'Test Velocity Secrets With Scripting User'],
  ['DotFavoritePage.postman_collection.json', 'mylimiteduser', 'FireDotFavoriteWithPermissionsWithLimitedUser'],
]) {
  test(`${actor} setup actually issues and assigns an API JWT for the created canonical user ID`, async () => {
    const subject = `canonical-${actor}-id`, issuedJwt = `synthetic.${actor}.jwt`;
    const vars = { serverURL: 'http://dotcms.invalid', jwt: 'synthetic.runner.jwt' };
    const issuedSubjects = [], createdForms = [], logs = [];
    const scope = { get: (key) => vars[key], set: (key, val) => { vars[key] = val; },
      replaceIn: (text) => resolve(text, vars) };
    // Existing Postman response assertions are inert here; Node assertions below check provisioning.
    const expectation = new Proxy(() => {}, { get: () => expectation, apply: () => expectation });
    const response = (data) => ({ code: 200, json: () => data, to: expectation });
    const send = (request, callback) => {
      const target = resolve(typeof request === 'string' ? request : request.url?.raw || request.url, vars);
      let data = { entity: {} };
      if (/\/api\/v1\/apitoken(?:\?|$)/.test(target)) {
        const form = JSON.parse(resolve(request.body?.raw, vars));
        issuedSubjects.push(form.userId);
        data = { entity: { jwt: issuedJwt } };
      } else if (/\/api\/v1\/users(?:\?|$)/.test(target)) {
        if (request.method === 'POST') createdForms.push(JSON.parse(resolve(request.body?.raw, vars)));
        data = { entity: { userID: subject, user: { id: subject, userId: subject, emailAddress: `${actor}@dotcms.com` } } };
      } else if (/\/roles\/layouts/.test(target)) data = { entity: [{ id: 'fixture-layout-id' }] };
      const result = response(data);
      if (callback) callback(null, result);
      return Promise.resolve(result);
    };
    const pm = { variables: scope, environment: scope, collectionVariables: scope, globals: scope,
      sendRequest: send, test: (_name, callback) => callback(), expect: () => expectation };
    for (const r of requests.filter((r) => path.basename(r.file) === file)) {
      const applicable = scripts.filter((s) => s.file === r.file && s.names.every((name, i) => r.names[i] === name));
      const execute = async (listen) => {
        for (const { event } of applicable.filter((s) => s.event.listen === listen)) {
          await vm.runInNewContext(eventCode(event), { pm, Buffer,
            console: { log: (...args) => logs.push(args.join(' ')), error() {}, warn() {} } });
        }
      };
      await execute('prerequest');
      if (r.names.join(' / ') === consumer) break;
      pm.response = await send(r.request);
      await execute('test');
    }
    assert.ok(createdForms.some((form) => form.email === `${actor}@dotcms.com` &&
      form.roles.includes('DOTCMS_BACK_END_USER') && (actor !== 'scripting' || form.roles.includes('Scripting Developer'))),
      'Keep the created actor email and required fixture roles');
    assert.ok(issuedSubjects.length > 0, 'Setup must actually request an API JWT, not just invent a variable name');
    assert.ok(issuedSubjects.every((id) => id === subject), 'Issue for returned canonical actor ID, not admin/email');
    const r = find(file, consumer);
    assert.equal(r.auth?.type, 'bearer');
    assert.equal(resolve(value(r.auth, 'token'), vars), issuedJwt, 'Assign entity.jwt to the token consumed by this actor');
    assert.ok(!logs.join('\n').includes(issuedJwt), 'Do not log actor tokens');
  });
}

test('protected manifest download rejects invalid Bearer without ambient session, while the valid case stays 200', () => {
  const matches = requests.filter((r) => path.basename(r.file) === 'Manifest_Download_End_Point.postman_collection.json' &&
    r.names[0].startsWith('Try to download Manifest with invalid') && r.names.at(-1) === 'Download manifest');
  assert.equal(matches.length, 1);
  const r = matches[0];
  assert.equal(r.auth?.type, 'bearer');
  assert.equal(value(r.auth, 'token'), 'not_valid_token');
  assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/,
    'BundleResource.downloadManifest requires a backend user before bundle lookup');
  assert.equal(r.names[0], 'Try to download Manifest with invalid token');
  assert.match((r.node.event || []).filter((e) => e.listen === 'prerequest').map(eventCode).join('\n'),
    /pm\.cookies\.jar\(\)\.clear\(/, 'Do not let setup authenticate the invalid-token request through a session');
  assert.equal(manualAuth(r).length, 0);
  assert.ok(!(r.request.header || []).some((h) => !h.disabled && /^cookie$/i.test(h.key)));
  const positive = find('Manifest_Download_End_Point.postman_collection.json', 'Download Manifest Successfully / Download manifest');
  assert.equal(positive.auth?.type, 'bearer');
  assert.equal(value(positive.auth, 'token'), '{{jwt}}');
  assert.match((positive.node.event || []).map(eventCode).join('\n'), /\.status\(200\)/);
});

test('non-admin actors are retained rather than replaced with the runner admin JWT', () => {
  const actors = {
    chris: ['Save Step [POST /api/v1/workflow/steps] / GivenInvalidSchemeId_ShouldRespond404',
      'Save Step [POST /api/v1/workflow/steps] / SupportShortly', 'SaveAction [POST /api/v1/workflow/actions] / InvalidStepId_ShouldRespond404',
      'SaveAction [POST /api/v1/workflow/actions] / InvalidSchemeId_ShouldRespond404', 'findAllSchemesAndSchemesByContentType / Invalid cotnet type ID',
      'ImportScheme / EmptyBody_ShouldRespond400', 'UpdateScheme / InvalidSchemeId', 'FireActionById / InvalidContentTypeId',
      'FireDefaultAction / InvalidContentTypeId', 'DeleteAction (Permanently) / InvalidActionId'],
    bill: ['findAction [GET /api/v1/workflow/actions/{actionId}] / ShortlyIdValidation',
      'ReorderAction / InvalidOrderValue', 'ReorderAction / InvalidStepId', 'ReorderAction / InvalidActionId '],
    scripting: ['Test Velocity Secrets With Scripting User'],
    mylimiteduser: ['FireDotFavoriteWithPermissionsWithLimitedUser'],
  };
  const actorTokens = new Set();
  for (const [actor, names] of Object.entries(actors)) {
    const file = actor === 'scripting' ? 'VelocitySecrets.postman_collection.json' :
      actor === 'mylimiteduser' ? 'DotFavoritePage.postman_collection.json' : 'Workflow_Resource_Tests.json';
    const tokens = new Set();
    for (const name of names) {
      const r = find(file, name);
      // Existing identity is an explicit exemption here, NOT in the no-ordinary-Basic test above.
      if (r.auth?.type === 'basic') assert.equal(value(r.auth, 'username'), `${actor}@dotcms.com`);
      else {
        assert.equal(r.auth?.type, 'bearer', label(r));
        const token = value(r.auth, 'token');
        assert.match(token, /^\{\{.+\}\}$/, 'Use an actor-specific API JWT variable');
        assert.notEqual(token, '{{jwt}}', 'Do not elevate fixture actor to admin');
        tokens.add(token);
      }
    }
    assert.ok(tokens.size <= 1, `Use a consistent token for ${actor}`);
    for (const token of tokens) {
      assert.ok(!actorTokens.has(token), 'Different actors must not share a token');
      actorTokens.add(token);
    }
  }
});

// PageResource.render rejects a caller with no authenticated user before host, page,
// variant, mode or permission resolution (see the 401 OpenAPI contract). The collection
// builds the admin page and experiment through Bearer setup, so its four positive variant
// renders must keep the same admin Bearer; an ambient cookie is not part of the contract.
const renderPositives = [
  'To specific variants / render page with DEFAULT variant',
  'To specific variants / render page with new variant',
  'To specific variants / Render again after Add/remove',
  'To specific variants / Render again after Add/remove DEFAULT variant',
].map((names) => find('Add-Remove_Contentlet_from_a_Page.postman_collection.json', names));

test('Add-Remove render positives resolve to the effective admin Bearer, with no manual override', () => {
  assert.equal(renderPositives.length, 4, 'Keep exactly the four positive variant renders');
  for (const r of renderPositives) {
    assert.equal(r.auth?.type, 'bearer', label(r));
    assert.equal(value(r.auth, 'token'), '{{jwt}}', label(r));
    assert.equal(resolve(value(r.auth, 'token'), effectiveVariables(r)), 'synthetic.runner.jwt', label(r));
    assert.equal(manualAuth(r).length, 0, `Do not override the render auth with a manual header: ${label(r)}`);
  }
});

// Run the real render postscripts against a faithful 200/401 pair. The 401 payload is
// deliberately identical to the 200 one so only the status assertion can reject it; a
// TypeError here would mean the mock, not the assertion, produced the failure.
function runRenderScript(r, status, body) {
  const failures = [];
  const response = {
    status,
    code: status,
    json: () => body,
    text: () => typeof body === 'string' ? body : JSON.stringify(body),
    to: { have: { status: (expected) => assert.equal(status, expected,
      `expected status ${expected} but got ${status}`) } },
  };
  const contains = (text, expected) => assert.ok(String(text).includes(expected),
    `expected ${JSON.stringify(text)} to contain ${JSON.stringify(expected)}`);
  const pm = {
    response,
    test: (name, fn) => { try { fn(); } catch (error) { failures.push({ name, error }); } },
    expect: (actual) => {
      const negative = (expected) => assert.ok(!String(actual).includes(expected),
        `expected ${JSON.stringify(actual)} not to contain ${JSON.stringify(expected)}`);
      return {
        to: { equal: (expected) => assert.equal(actual, expected),
          contains: (expected) => contains(actual, expected),
          eql: (expected) => assert.deepStrictEqual(actual, expected),
          not: { contains: negative, match: (pattern) => assert.ok(!pattern.test(String(actual))) } },
        not: { to: { contains: negative,
          match: (pattern) => assert.ok(!pattern.test(String(actual))) } },
      };
    },
  };
  for (const event of (r.node.event || []).filter((e) => e.listen === 'test')) {
    vm.runInNewContext(eventCode(event), { pm });
  }
  return failures;
}

test('anonymous GraphQL postscripts require a cookie-free 401 without disclosing auth configuration', () => {
  for (const names of [
    'Page API / Checking Exception Messages / GivenRequestByURI_Page_EditMode_WithoutAuth',
    'Disallow Introspection Query / Introspection Query Anonymous Request',
  ]) {
    const r = find('GraphQLTests.json', names);
    assert.equal(r.auth?.type, 'noauth', label(r));
    assert.equal(r.node.protocolProfileBehavior?.disableCookies, true, label(r));
    assert.deepEqual(runRenderScript(r, 401, 'Unauthorized'), [], label(r));
    assert.ok(runRenderScript(r, 400, '').length > 0, 'Do not retain the old GraphQL 400 workaround');
    assert.ok(runRenderScript(r, 200, 'Unauthorized').length > 0, 'Reject successful anonymous access');
    assert.ok(runRenderScript(r, 401, 'Unauthorized: CONTENT_APIS_ALLOW_ANONYMOUS permission denied').length > 0,
      'Keep detailed configuration errors out of the public response');
    assert.ok(runRenderScript(r, 401, '{"data":{"page":{}},"errors":[{"message":"Unauthorized"}]}').length > 0,
      'Do not return GraphQL data alongside an anonymous denial');
  }
});

test('bundle import fixtures assert existence as an authenticated reader, never anonymously', () => {
  // The bundle "content exists" check asserts the import created the contentlet, not that it is
  // publicly readable; under the CONTENT_APIS_ALLOW_ANONYMOUS=NONE default an anonymous read is
  // 401, which bails the runner and silently skips every later collection in the shard.
  const exists = find('Bundle_Resource.postman_collection.json', 'Upload Bundle / ContentShouldExist');
  assert.equal(exists.auth?.type, 'bearer', label(exists));
  assert.equal(resolve(value(exists.auth, 'token'), effectiveVariables(exists)), 'synthetic.runner.jwt', label(exists));
  assert.equal(exists.names[0], 'Upload Bundle');
});

test('language i18n fixtures authenticate; only the designated default-language probe stays anonymous', () => {
  // getAllMessages asserts the served message bundle, not anonymous access; under the
  // CONTENT_APIS_ALLOW_ANONYMOUS=NONE default a noauth read is 401 and bails the shard. The
  // _getdefault probe stays anonymous: that endpoint serves the default language without the
  // content-API gate and is asserted in CI as 200 while anonymous.
  const messages = requests.filter((r) => r.file === 'LanguageResourceTests.json' &&
    /getAllMessages/.test(r.names.join(' / ')));
  assert.equal(messages.length, 4, 'Keep the four getAllMessages fixtures');
  for (const r of messages) {
    assert.equal(r.auth?.type, 'bearer', label(r));
    assert.equal(resolve(value(r.auth, 'token'), effectiveVariables(r)), 'synthetic.runner.jwt', label(r));
  }
  const probe = find('LanguageResourceTests.json', 'GetLanguage / Get Default Language');
  assert.equal(probe.auth?.type, 'noauth', label(probe));
});

test('Add-Remove render postscripts pass an authorized 200 and fail a 401 on the status assertion', () => {
  const rendered = [
    'content_1 content_2 content_3',
    'content_1 content_2 content_3',
    'content_1 content_4',
    'content_1 content_2 content_3',
  ];
  renderPositives.forEach((r, i) => {
    const body = { entity: { page: { rendered: rendered[i] } } };
    const passed = runRenderScript(r, 200, body);
    assert.equal(passed.length, 0, `Authorized render must satisfy its own postscript: ${label(r)}: ${passed.map((f) => f.error.message).join('; ')}`);
    const denied = runRenderScript(r, 401, body);
    assert.equal(denied.length, 1, `A 401 must trip the render status assertion: ${label(r)}`);
    assert.ok(!(denied[0].error instanceof TypeError),
      `The 401 must fail the status assertion, not a mock TypeError: ${label(r)}`);
    assert.match(denied[0].error.message, /status 200/, label(r));
  });
});

// Keep protected page fixtures independent of the login cookie formerly created by Basic setup.
const protectedPageRequests = requests.filter((r) =>
  /\/api\/v1\/page\/(?:json|render(?:HTML)?)(?![A-Za-z])/.test(url(r)));
const pageAuthNegatives = [
  'JSON [api/v1/page/json] / Using Actual Page URIs / Get Page Metadata With Invalid User',
  'Render [api/v1/page/render] / Using Actual Page URIs / Get Page Metadata With Invalid User',
  'Render HTML [/api/v1/page/renderHTML] / UserAuthenticationValidation',
].map((names) => find('PagesResourceTests.json', names));
test('protected page APIs carry an effective resolved Bearer except explicit anonymous 401 tests', () => {
  assert.ok(protectedPageRequests.length >= 61, 'Do not pass by dropping protected page API coverage');
  assert.equal(protectedPageRequests.filter((r) => pageAuthNegatives.includes(r)).length, 3);
  for (const r of pageAuthNegatives) {
    assert.equal(r.auth?.type, 'noauth', label(r));
    assert.equal(manualAuth(r).length, 0, label(r));
    assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/, label(r));
  }
  const violations = protectedPageRequests.filter((r) => !pageAuthNegatives.includes(r)).filter((r) => {
    const headers = manualAuth(r);
    if (headers.length) return headers.some((h) => {
      const header = resolve(h.value, effectiveVariables(r));
      return !/^Bearer\s+\S+$/i.test(header) || /\{\{/.test(header);
    });
    const token = resolve(value(r.auth, 'token'), effectiveVariables(r));
    return r.auth?.type !== 'bearer' || !/^\S+$/.test(token) || /\{\{/.test(token);
  });
  assert.equal(violations.length, 0,
    `Protected page API requests need an effective Bearer:\n${violations.map(label).join('\n')}`);
});
