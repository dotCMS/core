/**
 * Left-aligns a popover overlay to the trigger's left edge, clamping to the viewport when
 * the overlay is wider than the trigger (e.g. minWidth exceeds trigger width).
 */
export function alignOverlayLeftToTrigger(trigger: HTMLElement, container: HTMLElement): void {
    const triggerRect = trigger.getBoundingClientRect();
    const containerWidth = container.offsetWidth;
    const viewportWidth = window.innerWidth;
    let left = triggerRect.left;

    if (left + containerWidth > viewportWidth) {
        left = Math.max(0, viewportWidth - containerWidth);
    }

    container.style.left = `${left}px`;
}
