const { test, afterEach, mock } = require("node:test");
const assert = require("node:assert/strict");
const childProcess = require("node:child_process");
const fs = require("node:fs");
const { prepareUnlogged } = require("./prepare-unlogged");
const original = { ...process.env };

afterEach(() => {
  mock.restoreAll();
  process.env = { ...original };
});

test("disabled mode never reads SQL or invokes Docker", () => {
  delete process.env.TEST_UNLOGGED_CONTAINER;
  const exec = mock.method(childProcess, "execFileSync", () => assert.fail("Docker called"));
  prepareUnlogged();
  assert.equal(exec.mock.callCount(), 0);
});

test("converts the explicitly selected container once and propagates failures", () => {
  process.env.TEST_UNLOGGED_CONTAINER = "isolated_database";
  process.env.TEST_UNLOGGED_SQL = "fixture.sql";
  mock.method(fs, "readFileSync", (file) => {
    assert.equal(file, "fixture.sql");
    return "DO fixture";
  });
  const exec = mock.method(childProcess, "execFileSync", (command, args, options) => {
    assert.equal(command, "docker");
    assert.deepEqual(args.slice(0, 4), ["exec", "-i", "isolated_database", "psql"]);
    assert.ok(args.includes("ON_ERROR_STOP=1"));
    assert.equal(options.input, "DO fixture");
    assert.equal(options.timeout, 130_000);
  });
  prepareUnlogged();
  assert.equal(exec.mock.callCount(), 1);
  exec.mock.mockImplementation(() => { throw new Error("conversion failed"); });
  assert.throws(prepareUnlogged, /conversion failed/);
});
