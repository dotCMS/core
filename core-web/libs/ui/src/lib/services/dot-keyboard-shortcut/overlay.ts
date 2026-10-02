import { ZIndexUtils } from 'primeng/utils';

/**
 * The lowest z-index PrimeNG stacks an overlay at: its default `overlay` and `menu` bases, which
 * dotCMS does not override. Below this an inline z-index is ordinary layout, such as the `2` PrimeNG
 * puts on an input's icon, and must not be read as something open above the page.
 */
const LOWEST_OVERLAY_Z_INDEX = 1000;

/**
 * Whether a PrimeNG overlay is stacked above the given element.
 *
 * The registry cannot arbitrate with an overlay's own key handling. A `<p-dialog closeOnEscape>`
 * binds its **own** document keydown listener and closes on a z-index comparison, never consulting
 * `defaultPrevented`, so it and a registry claim both fire on the same keystroke. It is third-party
 * code, so it cannot be brought into the registry. A surface underneath therefore has to stand down
 * on its own, and this is how it asks whether it should.
 *
 * Every PrimeNG overlay registers itself in the shared `ZIndexUtils` stack when it opens, so
 * comparing the top of that stack against an element's own z-index answers "is something above me?"
 * without matching on overlay class names, which differ per component and change across versions.
 * It therefore covers dialogs, confirm popups, select panels and anything added later, rather than a
 * list of visibility flags somebody has to remember to extend.
 *
 * @param container the caller's own overlay element. Omit it for a base-layer surface such as a
 * portlet shell, which has no element in the stack: `ZIndexUtils.get(undefined)` is `0`, so the
 * question becomes "is any overlay open at all", which is the right question for that caller.
 *
 * ⚠️ The stack is a module-level singleton, and an overlay that is torn down without closing leaves
 * its entry behind. That does not happen in the application, where overlays revert on close, but it
 * does across tests in one file: the Content Drive shell suite reads **1102** with nothing visible
 * once an earlier test has opened a dialog. **Tests that depend on this must mock
 * `ZIndexUtils.getCurrent`** rather than trusting the ambient value.
 *
 * Toasts and tooltips are the exception. They join the same stack, under the same `modal` key a
 * dialog uses, but they block nothing, so a shortcut must keep working while one is on screen
 * (#37884). The stack records only values, not which element holds them, so when it says something
 * is above, the stacked elements are looked up in the DOM: if every one of them is a passive layer,
 * nothing that should block is open. If none can be found (a mocked stack, or an entry left behind),
 * the stack's answer stands, so this can only ever relax the check, never fire behind a dialog.
 */
export function hasOverlayAbove(container?: Element | null): boolean {
    const floor = ZIndexUtils.get(container);

    if (ZIndexUtils.getCurrent() <= floor) {
        return false;
    }

    const stacked = Array.from(document.querySelectorAll<HTMLElement>('[style*="z-index"]')).filter(
        (element) => {
            const zIndex = ZIndexUtils.get(element);

            return zIndex > floor && zIndex >= LOWEST_OVERLAY_Z_INDEX;
        }
    );

    return stacked.length === 0 || stacked.some((element) => !isPassiveLayer(element));
}

/** A layer that sits above the page without taking it over: it informs and blocks nothing. */
function isPassiveLayer(element: Element): boolean {
    return element.matches('.p-toast, .p-tooltip');
}
