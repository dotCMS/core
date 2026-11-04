const childProcess = require("node:child_process");
const fs = require("node:fs");

// Called once after server readiness, before any collection, and disabled by default.
function prepareUnlogged() {
  const container = process.env.TEST_UNLOGGED_CONTAINER;
  if (!container) return;
  const started = performance.now();
  childProcess.execFileSync("docker", [
    "exec", "-i", container, "psql", "-X", "-v", "ON_ERROR_STOP=1",
    "-U", "postgres", "-d", "dotcms", "-c", "SET statement_timeout = '120s'",
    "-f", "-",
  ], {
    input: fs.readFileSync(process.env.TEST_UNLOGGED_SQL),
    stdio: ["pipe", "inherit", "inherit"],
    timeout: 130_000,
  });
  console.log(`UNLOGGED batch preparation_ms=${Math.round(performance.now() - started)}`);
}

module.exports = { prepareUnlogged };
