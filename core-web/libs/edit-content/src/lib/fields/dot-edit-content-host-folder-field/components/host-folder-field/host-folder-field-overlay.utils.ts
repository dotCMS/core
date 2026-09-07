/**
 * Left-aligns a popover overlay to the trigger's left edge, clamping to the viewport when
 * the overlay is wider than the trigger (e.g. minWidth exceeds trigger width).
 *
 * Coordinates match PrimeNG's body-appended overlays: viewport rects plus horizontal scroll.
 */
export function alignOverlayLeftToTrigger(trigger: HTMLElement, container: HTMLElement): void {
    const triggerRect = trigger.getBoundingClientRect();
    const containerWidth = container.offsetWidth;
    const viewportWidth = window.innerWidth;
    const scrollX = window.scrollX;
    const minLeft = scrollX;
    const maxLeft = scrollX + Math.max(0, viewportWidth - containerWidth);
    const left = Math.min(Math.max(triggerRect.left + scrollX, minLeft), maxLeft);

    container.style.left = `${left}px`;
}
