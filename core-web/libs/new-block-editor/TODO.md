# TODO

Pending items to revisit tomorrow.

## Features

- [x] Add a feature flag to let users switch between the legacy and the new block editor — avoids breaking existing instances until full QA is done

## Bugs

- [ ] Add a mask / transparent overlay as a background for popovers when they open inline
- [ ] Add the "video/image by URL" option to the assets modal
- [ ] Investigate why normal text cannot be edited after AI-generated content is inserted
- [ ] Add styles to the code block
- [ ] Improve the styles for the selected node when highlighted — likely fixable by reducing the X-axis padding on the block editor and adding a small amount of padding to all blocks

## Cleanup (post-flag-removal)

When `NEW_BLOCK_EDITOR_FEATURE_FLAG` exits QA and is dropped, revert the rollback scaffolding in a single PR:

- [ ] Remove `dot-old-block-editor` custom-element registration in `apps/dotcms-block-editor/src/main.ts`
- [ ] Remove `BlockEditorModule` from `apps/dotcms-block-editor/src/app/app.config.ts` providers
- [ ] In `libs/edit-content/src/lib/fields/dot-edit-content-block-editor/`: drop the `@if (isNewBlockEditorEnabled())` branches and the `DotPropertiesService` injection / signal
- [ ] In `libs/portlets/edit-ema/portlet/src/lib/components/dot-block-editor-sidebar/`: same — drop the flag branch and the service injection
- [ ] In `libs/portlets/edit-ema/ui/src/lib/dot-content-compare/components/dot-content-compare-block-editor/`: drop the flag branch, the `AnyBlockEditor` type union, and the `getEditorInstance()` shim (use `this.blockEditor.editor()` directly)
- [ ] Delete the legacy lib `libs/block-editor/`
- [ ] Remove `NEW_BLOCK_EDITOR_FEATURE_FLAG` from `dotCMS/src/main/java/com/dotcms/featureflag/FeatureFlagName.java`, `dotCMS/src/main/java/com/dotcms/rest/api/v1/system/ConfigurationResource.java`, the JSP guard in `edit_field.jsp`, and `core-web/libs/dotcms-models/src/lib/shared-models.ts`
- [ ] Remove `NEW_BLOCK_EDITOR_FEATURE_FLAG=false` from `dotCMS/src/main/resources/dotmarketing-config.properties`
