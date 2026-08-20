import { OverlayOptions } from 'primeng/api';

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

// The image / video / audio pickers used to build their dialog config here. They now open
// `DotAssetPickerComponent` through `buildAssetPickerDialogConfig` from `@dotcms/ui`, which owns
// those flags because the picker depends on them — see `EditorModalService.openAssetPicker`.
