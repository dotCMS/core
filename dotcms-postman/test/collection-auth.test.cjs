// Static migration guards, not proof of live modes, cookie behavior or token subjects.
// ALL-only negatives and JWT_TOKEN DAV still need isolated server/CI selection and NONE denial runs.
const assert = require('node:assert/strict');
const { readdirSync, readFileSync } = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const vm = require('node:vm');

const root = path.resolve(__dirname, '../src/main/resources/postman');
const requests = [], scripts = [];
function visit(node, file, names = [], inherited) {
  const auth = node.auth ?? inherited;
  for (const event of node.event || []) scripts.push({ file, names, event });
  if (node.request) requests.push({ file, names, node, request: node.request,
    auth: node.request.auth ?? auth });
  for (const item of node.item || []) visit(item, file, [...names, item.name], auth);
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

test('ordinary effective auth and manual headers have no password-Basic dependency', () => {
  assert.ok(requests.length >= 2915, 'Do not pass by silently dropping collections or requests');
  const violations = requests.filter((r) => {
    // DAV is a separate JWT_TOKEN compatibility protocol; these two negatives belong in isolated ALL.
    if (/\/webdav\//i.test(url(r)) || isJwtOnlyNegative(r)) return false;
    return r.auth?.type === 'basic' || (r.request.header || []).some((h) =>
      !h.disabled && /^authorization$/i.test(h.key) && /^Basic\s/i.test(h.value));
  });
  assert.equal(violations.length, 0, `Migrate inherited/request/manual auth, not just roots:\n${violations.map(label).join('\n')}`);
});

test('collection/folder/request scripts do not silently bootstrap or send Basic', () => {
  const violations = scripts.filter(({ event }) =>
    /["'`]?Authorization["'`]?\s*:\s*["'`]Basic\b|["']?\btype["']?\s*:\s*["']basic["']/i.test(eventCode(event)));
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

test('valid-Basic-vs-required-JWT negatives remain Basic and assert 401 (ALL-only boundary)', () => {
  for (const [file, names] of jwtOnlyNegatives) {
    const r = find(file, names);
    assert.equal(r.auth?.type, 'basic');
    assert.equal(value(r.auth, 'username').toLowerCase(), 'admin@dotcms.com');
    assert.equal(value(r.auth, 'password'), 'admin');
    assert.match((r.node.event || []).map(eventCode).join('\n'), /\.status\(401\)/);
  }
});

test('DAV CRUD keeps username/API-JWT auth, REST setup uses Bearer, no conflicting header', () => {
  const dav = requests.filter((r) => path.basename(r.file) === 'WebDav.postman_collection.json' && /\/webdav\//i.test(url(r)));
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
  assert.notEqual(value(find('Logger_Resource.postman_collection.json', 'Invalid User Copy').auth, 'token'), '{{jwt}}');
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
