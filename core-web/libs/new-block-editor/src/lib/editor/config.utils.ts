import { OverlayOptions } from 'primeng/api';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';

/**
 * Base z-index for every body-portaled overlay the editor opens (PrimeNG `DialogService`
 * modals and `<p-select>` / `<p-autoComplete>` panels). The fullscreen editor shell renders
 * its backdrop at `z-[9998]` (see `editor.component.ts`), so overlays appended to
 * `document.body` with PrimeNG's default base (≈1000) get painted *under* the fullscreen
 * overlay and become unreachable. Lifting the base above the shell keeps them clickable.
 * Matches the toolbar's tooltip override (`overlayTooltipOptions`), which uses the same value.
 */
export const OVERLAY_ABOVE_FULLSCREEN_Z_INDEX = 10050;

/**
 * Shared `overlayOptions` for the editor's `<p-select>` / `<p-autoComplete>` controls.
 * PrimeNG 21 exposes overlay z-index through `overlayOptions` (not a `baseZIndex` input), so
 * every body-portaled dropdown panel binds this to clear the fullscreen shell's backdrop.
 * `appendTo` is left to each control's own input (which takes precedence over this).
 */
export const FULLSCREEN_AWARE_OVERLAY_OPTIONS: OverlayOptions = {
    autoZIndex: true,
    baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX
};

/**
 * Shared centered-modal configuration for editor dialogs that mount
 * `DotBrowserSelectorComponent` from `@dotcms/ui` (currently the dotCMS image and
 * video pickers). Locking sizing, mask styling, and the picker's data-payload
 * defaults in one place keeps every browse-an-asset flow consistent and avoids
 * drift when a new mime-type variant is added.
 *
 * Callers provide only the dialog header and the contentlet mime-type allowlist
 * (e.g. `['image']` or `['video']`). Everything else mirrors the file-field's
 * configuration so customers see the same UX whether they're picking an asset
 * for a file field or for a Story Block.
 */
export function buildBrowserSelectorConfig(opts: {
    header: string;
    mimeTypes: string[];
}): DynamicDialogConfig {
    return {
        header: opts.header,
        appendTo: 'body',
        // Modal picker must clear the fullscreen editor shell's `z-[9998]` backdrop.
        baseZIndex: OVERLAY_ABOVE_FULLSCREEN_Z_INDEX,
        closeOnEscape: true,
        closable: true,
        dismissableMask: true,
        draggable: false,
        keepInViewport: false,
        maskStyleClass: 'p-dialog-mask-dynamic',
        resizable: false,
        modal: true,
        width: '90%',
        style: { 'max-width': '1040px', overflow: 'auto' },
        contentStyle: { overflow: 'auto', height: 'min(45rem, 80vh)' },
        data: {
            mimeTypes: opts.mimeTypes,
            showLinks: false,
            showDotAssets: true,
            showPages: false,
            showFiles: true,
            showFolders: false,
            showWorking: true,
            showArchived: false,
            sortByDesc: true
        }
    };
}
