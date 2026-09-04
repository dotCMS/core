# 05 · Next.js: verify

**dotCMS page verification does not apply.** It renders dotCMS's own VTL output, which a headless
site never serves. A page can verify green there and still be blank in Next.js.

Verify in two layers instead:

1. **dotCMS has the data.** `dotCMSClient.page.get('/path')` returns a `pageAsset`
   whose `layout.body.rows[].columns[].containers[].contentlets` are populated. Empty
   contentlets = a placement or publish problem, so go back to
   [core/09](../core/09-placement.md) and [core/00](../core/00-what-must-exist.md) item 3.
2. **The app renders it.** Before running anything: read `package.json` to find the package
   manager (a lockfile settles it) and the dev script, confirm the `@dotcms/*` packages are
   installed, and check any Node version the project pins. Do not install or upgrade anything
   without asking. Then run that dev script and request the route — every content type on the
   page must resolve to a component.

## Triage

| Symptom | Cause |
|---|---|
| Slot renders "no component" / empty | component-map key ≠ the type's `Var`, case-exact ([01](01-component-contract.md)) |
| Page 404s in Next.js, exists in dotCMS | page not published, or wrong `siteId` in the client config |
| Content updates never appear | client caching — UVE needs `cache: 'no-cache'` |
| Editing does nothing in UVE | page not wrapped in `useEditableDotCMSPage`, or the UVE app is not configured for this host |

UVE setup and troubleshooting are owned upstream: **@dotcms/uve README → Prerequisites
& Setup**, **@dotcms/react README → Troubleshooting**, and `examples/nextjs/CLAUDE.md`
→ *Common Troubleshooting*.
