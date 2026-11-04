# Tier-1 JIT CI experiment (#38439)

Replaces the UNLOGGED experiment: all database conversion code, markers and suite hooks are
removed. Both modes use the normal logged databases and unchanged application/test code.

The draft PR caller opts into `tiered-jit-experiment`; every other caller defaults off.
The matrix pairs the 2 JVM unit jobs, 7 integration suites and 11 Postman batches as:

- `[default]`: normal tiered compilation (HotSpot level 4).
- `[tier1]`: `-XX:TieredStopAtLevel=1`, limiting compilation to C1 without profiling/C2.

Both modes disable the Maven build cache and print command-line VM flags. The flag is
inherited through `JAVA_TOOL_OPTIONS` by Maven and forked test/compiler JVMs, without
replacing `MAVEN_OPTS`, Surefire/Failsafe `argLine`, preview/module flags or Java agents.
Postman's opt-in `test-tier1` Maven profile separately forwards the flag into the dotCMS
container; host environment variables are not automatically inherited by Docker.
Postman already prints all effective VM flags, including `TieredStopAtLevel`, on startup.
OpenSearch, the shared image build, CLI, Karate, frontend and E2E jobs are not varied.

The measured scope is the whole Maven action (setup, Maven/compilation, tests, Docker
startup/teardown and artifact handling), not just test methods. VM-flag verification runs
after the measurement and fails if the host level differs from the selected mode.
Each paired job uploads `jit-benchmark-*` JSON and the usual raw XML reports. JSON includes
actual executed counts, errors/failures/skips, retry events, summed XML elapsed time,
requested JVM flags, job outcome and run URL. Missing/zero/all-skipped reports are rejected.
Surefire reports cover JVM unit jobs; Failsafe summaries cover integration/Postman jobs.
Postman's completed count remains request executions, not integration-test methods.

Compare only matched suites/batches with equal nonzero counts, no new failures and retries
reported. Collect at least three matched samples per mode before reporting medians and
percentage change. Full GitHub job times include checkout/native setup; the JSON action
interval does not. Extra runner concurrency and external setup can add noise.

**No speedup established yet.** Tier 1 can reduce startup/JIT CPU cost but lower steady-state
throughput, so long suites may regress. This draft adds 20 jobs to a full backend PR run;
disable the caller input after the experiment, and do not merge it enabled unintentionally.

## Focused checks

```sh
node --test .github/scripts/tiered-jit-experiment/matrix.test.cjs
python3 -m unittest discover -s .github/scripts/tiered-jit-experiment -p 'test_*.py'
bash .github/workflows/tests/test-matrix-equivalence.test.sh
```

For a local single-class smoke test (not a performance comparison):

```sh
JAVA_TOOL_OPTIONS='-XX:+PrintCommandLineFlags -XX:TieredStopAtLevel=1' \
  ./mvnw test -Dprod -pl :dotcms-core -Dtest=com.dotmarketing.util.StringUtilsTest \
  -DskipTests=false -Dmaven.build.cache.enabled=false
```

For Postman, also pass `-Ptest-tier1` and use a unique `-Dcontext.name=...` so the normal
harness does not remove an unrelated local environment. No existing local database is
needed or altered to validate the experiment's configuration.
