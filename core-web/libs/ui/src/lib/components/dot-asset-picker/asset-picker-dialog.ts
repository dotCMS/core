import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotAssetPickerConfig } from './store/models';

/**
 * Centered-modal configuration for `DotAssetPickerComponent`.
 *
 * Every caller must go through this rather than assembling its own: the picker makes real
 * assumptions about the dialog hosting it — it renders its own header, and its full-screen toggle
 * drives PrimeNG's maximized state — so these flags are part of the component's contract, not a
 * per-caller preference. Two callers hand-rolling them is how the Story Block and the File field
 * end up looking like different features again.
 *
 * @param data The picker configuration, normally from `buildAssetPickerConfig`.
 * @param overrides The one thing a caller may legitimately differ on — see below.
 */
export function buildAssetPickerDialogConfig(
    data: DotAssetPickerConfig,
    /**
     * `baseZIndex` only. The Story Block's full-screen shell paints its backdrop at `z-[9998]`, so a
     * picker opened from there has to be lifted above it or it renders under the shell and is
     * unreachable. Nothing else here is a caller's business.
     */
    overrides?: Pick<DynamicDialogConfig, 'baseZIndex'>
): DynamicDialogConfig<DotAssetPickerConfig> {
    return {
        // The picker renders its own header (title + full screen + ✕), so PrimeNG's chrome header is
        // hidden to avoid a duplicate and the title travels in `data`.
        showHeader: false,
        appendTo: 'body',
        closeOnEscape: true,
        closable: true,
        dismissableMask: true,
        draggable: false,
        keepInViewport: false,
        maskStyleClass: 'p-dialog-mask-dynamic',
        resizable: false,
        modal: true,
        // The picker's own header drives full screen through PrimeNG's maximized state. No maximize
        // button is rendered — PrimeNG's lives in the header we just hid.
        maximizable: true,
        // Autofocus would land on the picker's search input and paint the theme's focus halo the
        // moment the dialog opens, which reads as an error state.
        focusOnShow: false,
        // Windowed size as a single `width`, not `90%` capped by a `max-width`: an inline max-width
        // would still clamp the dialog once it goes full screen.
        //
        // The viewport-relative halves are what normally apply — the picker fills most of a laptop
        // screen, so the folder tree and the asset table both breathe without reaching for full
        // screen. The caps only bite on large external monitors, where a dialog that wide would just
        // be hard to read. They are in `rem` so they track the content, which Tailwind sizes in
        // `rem` throughout; mind that `html { font-size: 14px }` here, so 114rem/68rem are
        // ~1596x952px, not the 16px-root figures you would expect.
        //
        // `.p-dialog` is capped at `max-height: 90%` by the theme, so asking for more than 90vh
        // would have no effect.
        width: 'min(90vw, 114rem)',
        height: 'min(90vh, 68rem)',
        // The picker fills the dialog so it can grow with the full-screen toggle.
        contentStyle: { height: '100%', overflow: 'hidden', padding: '0' },
        data,
        ...overrides
    };
}
