import { ZIndexUtils } from 'primeng/utils';

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
 */
export function hasOverlayAbove(container?: Element | null): boolean {
    return ZIndexUtils.getCurrent() > ZIndexUtils.get(container);
}
