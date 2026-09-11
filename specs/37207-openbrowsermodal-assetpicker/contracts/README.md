# Interface Contracts

This feature touches three interface boundaries. One is redesigned outright; two change additively.

| # | Contract | Audience | Change |
|---|---|---|---|
| 1 | [`openbrowsermodal-public-api.md`](./openbrowsermodal-public-api.md) | Custom-field VTL templates | 🔄 **Replaced.** The API is new and unshipped, so it is redesigned (FR-010). Both shipped templates migrate in the same change. |
| 2 | [`drive-search-delta.md`](./drive-search-delta.md) | Internal REST, `POST /api/v1/drive/search` | ➕ Additive: `extensions` request field. |
| 3 | [`asset-picker-config.md`](./asset-picker-config.md) | Internal — `@dotcms/ui` consumers | ➕ Additive: `browse` options, `'browse'` mode. |

**Compatibility rules differ by contract, deliberately.**

- **Contract 1** has no external consumers (verified — the `_new.vtl` templates only render when the
  new Edit Content is enabled, which is not the default), so it is redesigned outright. The two
  dotCMS-owned templates are migrated in the same change.
- **Contracts 2 and 3** DO have other consumers — the Content Drive portlet and the AssetPicker's
  four existing entry points — so every addition there is optional with a default that reproduces
  today's behavior exactly.
