const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const workflow = fs.readFileSync(path.join(__dirname,
  '../../.github/workflows/cicd_comp_test-matrix.yml'), 'utf8');
// Exercise the actual inline fan-out, so changes cannot drift from a duplicate implementation.
const start = workflow.indexOf("            if (inputs['unlogged-experiment']) {");
const end = workflow.indexOf('            console.log(`Generated matrix', start);
const fanout = new Function('matrix', 'inputs', workflow.slice(start, end));
const fixture = [
  { name: 'suite', stage_name: 'IT suite', test_type: 'integration', maven_args: 'verify -Dit.test=MainSuite3a' },
  { name: 'batch', stage_name: 'Postman template', test_type: 'postman', maven_args: 'verify -Dpostman.collections=template' },
  { name: 'JVM', stage_name: 'JVM', test_type: 'jvm', maven_args: 'test' },
];

test('normal callers retain the exact matrix', () => {
  const matrix = structuredClone(fixture);
  fanout(matrix, { 'unlogged-experiment': false });
  assert.deepEqual(matrix, fixture);
});

test('experiment pairs only integration/Postman, retaining selection and unique artifact names', () => {
  const matrix = structuredClone(fixture);
  fanout(matrix, { 'unlogged-experiment': true });
  assert.equal(matrix.length, 5);
  assert.equal(new Set(matrix.map(entry => entry.stage_name)).size, 5);
  assert.deepEqual(matrix.at(-1), fixture.at(-1));
  for (const [index, source] of fixture.slice(0, 2).entries()) {
    const [logged, unlogged] = matrix.slice(index * 2, index * 2 + 2);
    assert.equal(logged.database_mode, 'logged');
    assert.equal(unlogged.database_mode, 'unlogged');
    assert.equal(logged.maven_args, source.maven_args + ' -Dmaven.build.cache.enabled=false');
    assert.equal(unlogged.maven_args, logged.maven_args + ' -Ptest-unlogged');
  }
});
