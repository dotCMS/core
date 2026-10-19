// Static migration guards, not proof of live modes, cookie behavior or token subjects.
// ALL-only negatives and JWT_TOKEN DAV still need isolated server/CI selection and NONE denial runs.
const assert = require('node:assert/strict');
const { readdirSync, readFileSync } = require('node:fs');
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
        data = { entity: [{ id: 'dotcms.org.1', emailAddress: 'admin@dotcms.com' },
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
  assert.match(pom, /<postman.allow.basic.auth>NONE<\/postman.allow.basic.auth>/);
  assert.match(pom, /<DOT_ALLOW_BASIC_AUTH>\$\{postman.allow.basic.auth\}<\/DOT_ALLOW_BASIC_AUTH>/);
});

test('DAV CRUD keeps username/API-JWT auth, REST setup uses Bearer, no conflicting header', () => {
  const dav = davPaths.map((names) => find('WebDav.postman_collection.json', names));
  assert.equal(dav.length, 13, 'Keep all DAV operations, not just NONE denials');
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

test('representative noauth/invalid/expired/custom-token requests are not elevated to admin', () => {
  const anonymous = [
    ['GraphQLTests.json', 'Page API / Checking Layout and Container Properties / Containers Property is Returned With Anon User'],
    ['GraphQLTests.json', 'Page API / Checking Exception Messages / GivenRequestByURI_Page_EditMode_WithoutAuth'],
    ['GraphQLTests.json', 'Disallow Introspection Query / Introspection Query Anonymous Request'],
    ...['list sections', 'create section', 'update section', 'delete section', 'reorder', 'set tools'].map((name) =>
      ['LayoutResource.postman_collection.json', `Unauthenticated (must run first, before any session cookie exists) / Unauthenticated - ${name} - 401`]),
    ['Push_Publish_JWT_Token_Test.postman_collection.json', 'fail_not_token_sent / request_pp'],
    ['Integrity_Checker_JWT_Token_Test.postman_collection.json', 'generate / fail_not_token_sent / request_integrity_generate'],
  ];
  for (const [file, names] of anonymous) assert.equal(find(file, names).auth?.type, 'noauth', names);
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
