# 01 · The site

**First build step, both delivery modes.** Everything after this needs the site's
identifier: content types scope content to it, assets upload into its folder tree,
the template payload carries `siteId`, and pages are created under it. Create it
before anything else.

You need a hostname before starting — from PLAN.md §2 if you came from
`dotcms-create-sites`, otherwise ask for it; never invent one. Sites have no purpose-built tool —
go through the dotCMS MCP server.

## Create (or reuse) the site

Look it up by hostname first so a re-run doesn't fail or duplicate:

```
GET  /api/v1/site?perPage=100      → match entity[].hostname
POST /api/v1/site                  → { "siteName": "<hostname>", "description": "…", "languageId": 1 }
```

The created site's `identifier` is what every later step means by "site id".

## Set aliases, then publish

A new site is **unpublished** — it serves nothing until you publish it. Aliases are
set with a separate `PUT`, which has two traps:

```
PUT /api/v1/site?id=<siteId>       → { "siteName": "<hostname>", "aliases": "<alias>", "forceExecution": true }
PUT /api/v1/site/<siteId>/_publish
```

- **The id goes in the query string.** An `identifier` in the body is ignored.
- **`siteName` is mandatory on the PUT** even when you are only changing aliases;
  omit it and the call fails.
- **`forceExecution: true`** is required for the change to apply.

## Give it a `*.localhost` alias for verification

Verification requests resolve the site by **hostname**, and a fetch client cannot
fake one: Node's `fetch` silently drops a `Host` header (the Fetch spec forbids it),
so the request lands on the *default* site and every check 404s while the pages are
actually fine.

Set an alias ending in `.localhost` — e.g. `mysite.localhost` for the site
`mysite.local`. Browsers and Node resolve `*.localhost` to `127.0.0.1` with no
`/etc/hosts` entry, so `http://mysite.localhost:8080/` reaches the right site.
Verify against the alias, not the canonical hostname.

## Done when

`GET /api/v1/site?perPage=100` shows the site with your hostname, its alias set, and
`http://<alias>:8080/` returns something other than the default site. Record the
identifier — [02](02-content-types.md) onward all need it.
