# Contracts: Opt-In Second-Level Relationship Filter

No new or changed REST contract. The existing Push Publishing Filter REST surface already
serializes a filter's `filters` property as a generic `Map<String,Object>` in both directions:

- Input: `com.dotcms.rest.api.v1.pushpublish.FilterDescriptorForm#getFilters()`
- Output: `com.dotcms.publishing.FilterDescriptor#getFilters()`, wrapped by
  `ResponseEntityFilterDescriptorView` / `ResponseEntityFilterDescriptorsView`

Adding the new `relationshipsSecondLevel` boolean key flows through this existing, unconstrained
map without any `@Schema`, DTO, or `openapi.yaml` change — see research.md R2 for the full
rationale. The one new, user-visible surface is the new YAML filter file itself becoming
selectable wherever the PP filter dropdown already lists filters discovered from
`dotCMS/src/main/webapp/WEB-INF/publishing-filters/` — no new UI contract, since that discovery
mechanism is unchanged.
