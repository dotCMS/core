# Contracts — #37366

This fix changes **no REST contract**. No JAX-RS resource, no `@Schema`, no `openapi.yaml`
regeneration. What changes is the **VTL-facing browse API** that custom-field templates call, plus
the shape of the request the Asset Picker sends to an unchanged endpoint.

| File | What it pins down |
|---|---|
| [`openbrowsermodal-public-api.md`](./openbrowsermodal-public-api.md) | `DotCustomFieldApi.openBrowserModal()` after the change — the kinds it accepts, what it returns, and what a stale `kinds: ['folder']` caller gets |
| [`drive-search-request-delta.md`](./drive-search-request-delta.md) | The exact `/api/v1/drive/search` request body the picker sends, before and after. The endpoint is not changed — only what the picker asks for |

Both are deltas against the contract #37207 shipped
([`specs/37207-openbrowsermodal-assetpicker/contracts/`](../../37207-openbrowsermodal-assetpicker/contracts/)),
which is the baseline to read them against.
