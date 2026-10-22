# Postman authentication

## Ordinary suites: `NONE`

The Node 22 runner logs in with JSON `POST /api/v1/authentication`, retains only
that response's `JSESSIONID` for `POST /api/v1/apitoken`, and gives Newman only the
API JWT (`entity.jwt`) and `serverURL`. It never passes the bootstrap session to
Newman. Both bootstrap requests reject redirects and have 30-second abort timers.
The deprecated login-token endpoint is not used. Newman CLI/console dumps are
suppressed because assertion/debug output can contain token values; inspect the
JUnit report locally for detailed failures and treat those reports as sensitive.

Ordinary authenticated requests use Bearer `{{jwt}}`; anonymous and invalid-token
requests retain their own auth. Workflow users `chris`/`bill` are resolved by exact
email before receiving separate API tokens. If absent from the empty starter,
they are created with random passwords and the backend role; only their new personal
roles receive the layout containing `workflow-schemes`. Existing users and roles are
not changed. Use a disposable test server: newly created fixture users persist.
Created scripting/limited users receive tokens for the ID returned by user creation,
not an admin token. Their fixture roles and existing response assertions are retained.

For standalone Postman imports, set **environment** `serverURL` and a pre-issued
**API token** `jwt`. Collections no longer fall back to password Basic; standalone
bootstrap guards fail with instructions when `jwt` is absent. Do not substitute a
legacy login JWT (`entity.token`). Do not export live tokens or log them. Clear
cached `chrisJwt`/`billJwt` collection variables before a new standalone run if
those fixture tokens have expired; Newman loads fresh collection JSON each run.

The module defaults `postman.basic.auth.mode` to `NONE`. Ordinary discovery is
nonrecursive, so the compatibility directories below are excluded from normal
`all`/`default` shards. The normal shard includes `WebDav_Basic_Denied`, which
checks password and API-JWT Basic rejection for all 12 DAV methods (including
OPTIONS, LOCK and COPY/MOVE). It clears cookies before each denial and first
verifies Bearer access to resolve the starter site.

## Separate compatibility servers

Do **not** set the ordinary suite to `ALL` or `JWT_TOKEN`. Provision a fresh,
separate server for each compatibility command; the Maven property is forwarded
to that server as `DOT_ALLOW_BASIC_AUTH`.

```sh
# All 24 DAV operations (including temp COPY/MOVE), using username + API JWT as the Basic password.
# REST setup remains Bearer; a real-password REST denial distinguishes JWT_TOKEN from ALL.
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false \
  -Dpostman.basic.auth.mode=JWT_TOKEN \
  -Dpostman.collection.dir=src/main/resources/postman/compatibility/jwt-token \
  -Dpostman.collections=WebDav.postman_collection

# The two original "valid Basic instead of required JWT" cases still assert 401.
# A successful password-Basic REST control proves that Basic is actually enabled.
./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false \
  -Dpostman.basic.auth.mode=ALL \
  -Dpostman.collection.dir=src/main/resources/postman/compatibility/all \
  -Dpostman.collections=Push_Publish_JWT_Token_Test.postman_collection,Integrity_Checker_JWT_Token_Test.postman_collection
```

The ALL-only negatives clear the cookie jar before sending credentials, so the
positive control cannot establish an ambient admin session for them. The DAV CRUD
collection retains all XML/content/status assertions and cleanup; the NONE
rejection collection does not replace that functional coverage. The dedicated CI
jobs must use these directory/collection selections and independent server modes.

## Focused local checks

Maven runs both Node regression files in the `test` phase whenever Postman tests are enabled
(`-Dpostman.test.skip=false`), before starting the live test server. Postman CI shards disable
the Maven build cache so neither these guards nor the live collections can be skipped.

```sh
# No packages or running dotCMS needed; use the repository-pinned Node 22 binary.
installs/node/node --test dotcms-postman/test/*.test.cjs
```

Direct CLI runs exit nonzero for bootstrap/load/execution/assertion failures and
zero-execution runs. Maven alone passes `--deferFailureExit`: a failing summary is
written first, JaCoCo dumps and Docker stop run, then `failsafe:verify` fails the
build. Do not use that flag in direct automation without checking the XML result.

The tests execute the entire runner with mocked fetch/Newman boundaries, exercise
actor provisioning scripts with synthetic responses, and inspect effective
collection auth. They do not prove live token subjects, permissions, HTTP cookies,
server-mode enforcement or full CI/Newman success. Protected-positive and
anonymous preservation checks are representative, not a golden snapshot of every
request. Live verification must also check nonzero `TEST-*.xml` executions and
`failsafe-summary.xml` failures/errors, not just the runner's process exit code.
