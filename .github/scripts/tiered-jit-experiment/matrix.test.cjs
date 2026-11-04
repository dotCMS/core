const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const workflow = fs.readFileSync(path.join(__dirname,
  '../../workflows/cicd_comp_test-matrix.yml'), 'utf8');
// Exercise the actual inline fan-out, not a duplicate implementation.
const start = workflow.indexOf("            if (inputs['tiered-jit-experiment']) {");
const end = workflow.indexOf('            console.log(`Generated matrix', start);
assert.ok(start >= 0 && end > start);
const fanout = new Function('matrix', 'inputs', workflow.slice(start, end));
const fixture = [
  { name: 'suite', stage_name: 'IT suite', test_type: 'integration', maven_args: 'verify -Dit.test=MainSuite3a' },
  { name: 'batch', stage_name: 'Postman template', test_type: 'postman', maven_args: 'verify -Dpostman.collections=template' },
  { name: 'JVM Unit Tests', stage_name: 'JVM Tests', test_type: 'jvm', maven_args: 'test -pl :dotcms-core' },
  { name: 'CLI', stage_name: 'CLI Tests', test_type: 'cli', maven_args: 'verify -pl :dotcms-cli' },
];

test('normal callers retain the exact matrix', () => {
  const matrix = structuredClone(fixture);
  fanout(matrix, { 'tiered-jit-experiment': false });
  assert.deepEqual(matrix, fixture);
});

test('pairs unit/integration/Postman jobs; only experimental Postman enables its Docker profile', () => {
  const matrix = structuredClone(fixture);
  fanout(matrix, { 'tiered-jit-experiment': true });
  assert.equal(matrix.length, 7);
  assert.equal(new Set(matrix.map(entry => entry.stage_name)).size, 7);
  assert.deepEqual(matrix.at(-1), fixture.at(-1));
  for (const [index, source] of fixture.slice(0, 3).entries()) {
    const [baseline, tier1] = matrix.slice(index * 2, index * 2 + 2);
    assert.equal(baseline.jit_mode, 'default');
    assert.equal(tier1.jit_mode, 'tier1');
    assert.equal(baseline.java_tool_options, '-XX:+PrintCommandLineFlags');
    assert.equal(tier1.java_tool_options, '-XX:+PrintCommandLineFlags -XX:TieredStopAtLevel=1');
    assert.equal(baseline.maven_args, source.maven_args + ' -Dmaven.build.cache.enabled=false');
    assert.equal(tier1.maven_args, baseline.maven_args + (source.test_type === 'postman' ? ' -Ptest-tier1' : ''));
  }
});

test('paired JVM unit jobs still get native libvips setup and inherit the flag without replacing argLine', () => {
  const phase = fs.readFileSync(path.join(__dirname, '../../workflows/cicd_comp_test-phase.yml'), 'utf8');
  assert.ok(phase.includes("startsWith(matrix.name, 'JVM Unit Tests')"));
  assert.ok(phase.includes('EXPERIMENT_JAVA_TOOL_OPTIONS: ${{ matrix.java_tool_options }}'));
  assert.ok(phase.includes('JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }$EXPERIMENT_JAVA_TOOL_OPTIONS'));
  assert.ok(!workflow.includes('-DargLine='));
});
