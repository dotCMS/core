# Contract: Content Analytics app config schema addition

## File

`dotCMS/src/main/resources/apps/dotContentAnalytics-config.yml`

## New param

Add under a new "Persistence Mode" section (placement: near the top, alongside `siteAuth`, since
it's a fundamental instance-level setting rather than a rendering/debug option):

```yaml
  persistenceMode:
    hidden: false
    type: "SELECT"
    label: "Persistence Mode"
    hint: "Read & Write persists new analytics events to the Content Analytics infrastructure. Read Only stops this instance from sending new events, while dashboards and existing data keep working exactly as before."
    required: true
    value:
      -
        label: "Read & Write"
        value: "readwrite"
        selected: true
      -
        label: "Read Only"
        value: "readonly"
```

This follows the exact shape `AppDescriptorHelper` already validates for `Type.SELECT`
(list of `{label, value, selected?}` maps, exactly one `selected: true`) — the same pattern
`signatureValidationType` in `dotsaml-config.yml` already uses in production.

## Consumers of this schema

- The generic Apps UI (renders any `SELECT` param as a dropdown automatically — no
  Content-Analytics-specific frontend code needed for the control itself).
- `EventAnalyticsProxyResource` (backend) — reads the saved value via
  `ContentAnalyticsUtil.getAppSecrets(host).get("persistenceMode")`; see
  `contracts/persistence-mode-gate.md`.

## Non-goals

- No change to `openapi.yaml` — App config schemas are not REST-annotated endpoints and are not
  part of the generated OpenAPI spec.
- No new Angular component — the existing generic Apps config form already renders `SELECT`
  params.
