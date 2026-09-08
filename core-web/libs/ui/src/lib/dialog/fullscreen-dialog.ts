/**
 * PrimeNG's own "maximized dialog" class.
 *
 * The theme styles it with `width/height/top/left: … !important`, which is the only thing that beats
 * the width/height `DialogService` writes **inline** on `.p-dialog`. That is why a dialog goes
 * full-screen by taking this class rather than by having its inline styles rewritten.
 *
 * PrimeNG applies it itself when `maximizable` is on and its own maximize button is clicked. A dialog
 * that hides PrimeNG's header (`showHeader: false`) to render its own has no such button, so it
 * drives the class from its own control instead.
 */
export const MAXIMIZED_DIALOG_CLASS = 'p-dialog-maximized';

/**
 * Inline `.p-dialog` style props applied when a dialog goes full-screen and
 * restored on exit. Overrides PrimeNG's `DynamicDialog` size (set inline via
 * `[ngStyle]`), so it must be applied as inline styles to win — a stylesheet
 * rule can't beat inline without `!important`.
 *
 * @deprecated Prefer {@link MAXIMIZED_DIALOG_CLASS}: PrimeNG's theme already declares the same sizing
 * with `!important`, so the class needs no save/restore of the windowed values.
 */
export const FULLSCREEN_DIALOG_STYLE: Record<string, string> = {
    width: '100vw',
    height: '100vh',
    maxWidth: '100vw',
    maxHeight: '100vh',
    borderRadius: '0'
};

/** Eased transition so the dialog grows/shrinks smoothly instead of snapping. */
export const DIALOG_SIZE_TRANSITION =
    'width 250ms ease, height 250ms ease, border-radius 250ms ease';
