# Skip list: classes that stay on `@Injectable`

Result of the real migration on 2026-10-14 (`nx g @angular/core:service --path=./`, then the build).

## Outside the migration on purpose

| Class / area | Reason |
|---|---|
| `libs/sdk/angular/src/lib/store/dotcms.store.ts`, `libs/sdk/angular/src/lib/services/dotcms-editable-page.service.ts` | `@dotcms/angular` is published with `@angular/core >=21.2.0` as a peer; `@Service` needs 22.1. Raising the range is a dotCMS release decision (ADR-0019). |
| Spec, mock and testing-helper test doubles | Decision: leave as is (throwaway doubles; the schematic does not reach them through Nx). |

## Reverted after the build failed (`NG2028`)

Each extends a library base class whose constructor uses constructor injection, which the schematic cannot see (it only reads our own source):

- `apps/dotcdn/src/app/dotcdn.component.store.ts`
- `apps/dotcms-ui/src/app/portlets/dot-containers/container-list/store/dot-container-list.store.ts`
- `apps/dotcms-ui/src/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store.ts`
- `apps/dotcms-ui/src/app/portlets/dot-containers/dot-container-create/dot-container-properties/store/dot-container-properties.store.ts`
- `apps/dotcms-ui/src/app/portlets/dot-pages/dot-pages-store/dot-pages.store.ts`
- `apps/dotcms-ui/src/app/portlets/dot-templates/dot-template-create-edit/store/dot-template.store.ts`
- `apps/dotcms-ui/src/app/portlets/shared/dot-content-types-listing/dot-content-type.store.ts`
- `apps/dotcms-ui/src/app/shared/services/dot-title-strategy.service.ts`
- `apps/dotcms-ui/src/app/view/components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store.ts`
- `apps/dotcms-ui/src/app/view/components/dot-toolbar/components/dot-toolbar-user/store/dot-toolbar-user.store.ts`
- `libs/block-editor/src/lib/extensions/ai-content-prompt/store/ai-content-prompt.store.ts`
- `libs/portlets/dot-experiments/portlet/src/lib/old/dot-experiments-configuration/store/dot-experiments-configuration-store.ts`
- `libs/portlets/dot-experiments/portlet/src/lib/old/dot-experiments-list/store/dot-experiments-list-store.ts`
- `libs/portlets/dot-experiments/portlet/src/lib/old/dot-experiments-reports/store/dot-experiments-reports-store.ts`
- `libs/portlets/dot-experiments/portlet/src/lib/old/dot-experiments-shell/store/dot-experiments.store.ts`
- `libs/portlets/dot-locales/portlet/src/lib/dot-locales-list/store/dot-locales-list.store.ts`
- `libs/portlets/edit-ema/portlet/src/lib/components/dot-ema-dialog/store/dot-ema-dialog.store.ts`
- `libs/portlets/edit-ema/ui/src/lib/dot-content-compare/store/dot-content-compare.store.ts`
- `libs/portlets/edit-ema/ui/src/lib/dot-favorite-page/store/dot-favorite-page.store.ts`
- `libs/portlets/edit-ema/ui/src/lib/dot-page-tools-seo/store/dot-page-tools-seo.store.ts`
- `libs/template-builder/src/lib/components/template-builder/store/template-builder.store.ts`
- `libs/ui/src/lib/components/dot-asset-search/store/dot-asset-search.store.ts`

Fix later: move these stores to SignalStore, then migrate.

## Fixed instead of skipped

- `libs/dotcms-js/src/lib/core/util/notification.service.ts`: removed a no-op class-level `@Inject('config')` (rejected by `@Service`, `NG1006`); migrated.
- Issue candidates `Rule.ts`, `i18n.service.ts`, `relationship-field.service.ts`: not skipped; the `constructor(` hits are on non-service classes or in JSDoc. Migrated by the schematic and the build passes.
- Five files outside any tsconfig the schematic reads were converted by hand: `dynamic-route-initializer.service.ts`, `dot-template-list-resolver.service.ts`, `SharedProducts.service.ts`, `dot-legacy-image-editor-launcher.service.ts` (plus comment edits in the experiments and asset-picker files).
