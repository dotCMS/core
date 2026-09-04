# @dotcms/http

The HTTP layer for talking to a dotCMS instance from Node: one `fetch`-based client, retry
reporting, a `Result` type, and the API paths.

Internal to this workspace — `package.json` is `private`, and consumers inline it at build
time, so it is never published to npm.

## What's in it

| Module | |
|---|---|
| `http.ts` | `httpGet` / `httpPost` over native `fetch`. Bearer auth, per-request timeout via `AbortController`, and `HttpError` carrying either an HTTP `status` or a transport `code` (`ECONNREFUSED`, `ETIMEDOUT`). |
| `fetch-retry.ts` | `describeRequestFailure()` turns a failure into a sentence a user can act on; `RetryReport` hands progress to whoever owns the terminal instead of writing over a spinner. |
| `result.ts` | `Result<T, E>` with `Ok` / `Err`, for calls whose failure is expected rather than exceptional. |
| `endpoints.ts` | `DOTCMS_API` paths, in one place so consumers cannot drift apart on them. |

## Why it exists

`@dotcms/create-app` and the `dotcms` CLI both authenticate against a dotCMS instance. This
code sets `Authorization` headers and follows redirects — the exact surface `axios` was
removed from this workspace over, after its Node adapter was found to leak
`Proxy-Authorization` across a redirect to a non-proxied origin (dotCMS/core#37264). A second
copy is where the next fix of that kind lands in one package and not the other.

Native `fetch` is used deliberately: the spec requires stripping `Authorization` on a
cross-origin redirect, which is the protection axios lacked. Requires Node ≥ 22.22.3.

## Using it

```ts
import { httpGet, HttpError, DOTCMS_API, endpoint } from '@dotcms/http';

const { data } = await httpGet(endpoint(url, DOTCMS_API.currentUser), { token });
```

Two things a consumer must wire up, both of which fail confusingly if missed:

- **Jest** — this workspace does not map `tsconfig.base.json` paths in `jest.preset.js`, so add
  `moduleNameMapper: { '^@dotcms/http$': '<rootDir>/../../http/src/index.ts' }`. Without it the
  alias compiles but never resolves at test time.
- **Buildable libs only** — `enforceBuildableLibDependency` is on workspace-wide, so a library
  importing this one needs its own build target.

## Tests

```bash
nx test http
```
