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

/**
 * Drives a PrimeNG dialog in and out of full screen.
 *
 * Extracted from the AssetPicker, which is where the behaviour and every one of its gotchas were
 * worked out. It lives here so a second dialog that renders its own header — the relationship
 * picker — gets the same behaviour instead of a second, subtly different copy of it.
 *
 * Three things are load-bearing and none of them are obvious:
 *
 * - The dialog goes full screen by taking {@link MAXIMIZED_DIALOG_CLASS}, not by having its inline
 *   size rewritten: the theme sizes that class with `!important`, which is the only thing that beats
 *   the width/height `DialogService` writes inline. Dropping the class hands the windowed size back.
 * - `maximize()` is called so PrimeNG's own `maximized` flag stays in step with us; the class is
 *   applied here as well because `Dialog` is `OnPush` and a toggle coming from its projected content
 *   never marks it dirty.
 * - `Boolean(dialog.maximized)` — PrimeNG leaves that flag **unset** until its own button is
 *   clicked, and `undefined !== false` would fire `maximize()` on the first (windowed) run, opening
 *   the dialog full screen the moment it appears.
 *
 * Requires `maximizable: true` in the dialog config; without it PrimeNG ignores `maximize()`.
 *
 * @param options.dialog The PrimeNG dialog instance, injected `{ optional: true }` by the host.
 * @param options.renderer The host's renderer.
 * @param options.on Whether full screen should be on.
 * @param options.prefersReducedMotion Skips the resize animation when the user asked for less motion.
 */
export function applyDialogFullscreen({
    dialog,
    renderer,
    on,
    prefersReducedMotion
}: {
    dialog: { container: () => unknown; maximized?: boolean; maximize: () => void } | null;
    renderer: {
        setStyle: (el: unknown, style: string, value: string) => void;
        addClass: (el: unknown, name: string) => void;
        removeClass: (el: unknown, name: string) => void;
    };
    on: boolean;
    prefersReducedMotion: boolean;
}): void {
    const container = dialog?.container() as HTMLElement | undefined;

    if (!dialog || !container) {
        return;
    }

    // Idempotent, and set before any toggle so the first real toggle already animates.
    renderer.setStyle(container, 'transition', prefersReducedMotion ? '' : DIALOG_SIZE_TRANSITION);

    if (Boolean(dialog.maximized) !== on) {
        dialog.maximize();
    }

    if (on) {
        renderer.addClass(container, MAXIMIZED_DIALOG_CLASS);
    } else {
        renderer.removeClass(container, MAXIMIZED_DIALOG_CLASS);
    }
}

/**
 * Whether the user has asked for reduced motion.
 *
 * @param doc The host's document.
 * @return True when the resize animation should be skipped.
 */
export function prefersReducedMotion(doc: Document): boolean {
    return doc.defaultView?.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}
