import { InjectionToken } from '@angular/core';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotAssetPickerEntryOptions } from './asset-picker-config';

/**
 * Opens `DotAssetPickerComponent` for a host that supports it.
 *
 * Two things about this contract are deliberate.
 *
 * **The caller passes its own `DialogService`.** PrimeNG's `DialogService` keys open dialogs by
 * component type per service instance and refuses to open a second dialog of a type it already has
 * open (`duplicationPermission`, which returns `null` rather than a ref). Every entry point today
 * provides its own component-scoped `DialogService`, so its picker is isolated from every other
 * field's; a launcher that injected one from the host injector would quietly share that map across
 * the whole form. The launcher therefore stays stateless and borrows the caller's service — which
 * also means it needs no `DialogService` provider next to it in any host.
 *
 * **It returns the raw `DynamicDialogRef`.** Each entry point already tracks its refs so it can
 * close an open picker when the field is destroyed; PrimeNG never closes dialogs on service
 * teardown (`DialogService` has no `ngOnDestroy`), so hiding the ref behind an observable would
 * force every caller to invent a second teardown path.
 */
export interface DotAssetPickerLauncher {
    /**
     * @param dialogService The caller's own `DialogService` — see above.
     * @param options Entry-point configuration, translated into picker filters by
     * `buildAssetPickerConfig`.
     * @param overrides `baseZIndex` only — see `buildAssetPickerDialogConfig`.
     */
    open(
        dialogService: DialogService,
        options: DotAssetPickerEntryOptions,
        overrides?: Pick<DynamicDialogConfig, 'baseZIndex'>
    ): DynamicDialogRef;
}

/**
 * DI seam that decides *which* picker an asset-selection entry point opens.
 *
 * Three entry points browse for an asset — the Story Block's image/video/audio nodes, the WYSIWYG
 * `dotAddImage` button, and the File/Image field's "Select Existing File" — and all three render in
 * two very different hosts: the new Angular Edit Content, and the legacy Dojo/JSP edit contentlet,
 * which mounts `<dotcms-block-editor>` and `<dotcms-binary-field>` as custom elements. The new
 * AssetPicker belongs to the former only; the old editor gets the picker it always had.
 *
 * Nothing at those entry points can tell the two hosts apart, so the switch lives here: the Angular
 * Edit Content hosts provide this token, the custom-element bootstraps do not, and every consumer
 * injects it as `{ optional: true }` — **present → new AssetPicker, absent → legacy picker**. A
 * server-side `Config` key would be the wrong instrument: one install renders both hosts at the same
 * time, so the discriminator has to be the host, not a customer setting.
 *
 * Routing the new picker exclusively through this token is also what keeps the seam honest — no
 * entry point imports `DotAssetPickerComponent` any more, so a new one cannot open it by accident
 * in a host that should not have it.
 *
 * Mirrors `IMAGE_EDITOR_LAUNCHER`, which solves the same host-capability problem for the image
 * editor. It lives here in `@dotcms/ui` rather than next to that one in `@dotcms/edit-content`
 * because `@dotcms/edit-content` already imports `@dotcms/new-block-editor`, so the Story Block
 * reaching back for it would close a project cycle.
 *
 * Provided by — and only by — the three Angular Edit Content hosts:
 * - `EditContentShellComponent` (full-screen route)
 * - `DotEditContentSidePanelComponent` (slide-in panel)
 * - `DotEditContentDialogComponent` (overlay dialog: UVE, Relationship field)
 */
export const ASSET_PICKER_LAUNCHER = new InjectionToken<DotAssetPickerLauncher>(
    'ASSET_PICKER_LAUNCHER'
);
