import { ChangeDetectionStrategy, Component, ElementRef, signal, viewChild } from '@angular/core';

import { Tooltip } from 'primeng/tooltip';

/** Attribute the folder tree uses to find this element when forwarding row focus (see below). */
export const TRUNCATED_LABEL_ATTR = 'data-dot-truncated-label';

/** Pointer dwell before the tooltip opens. Matches the tooltips already used in these panels. */
const TOOLTIP_SHOW_DELAY = 800;

/**
 * One-line label: clips whatever is projected into it with an ellipsis and reveals the full text
 * on hover or keyboard focus — but only when the text is actually clipped.
 *
 * This is the single definition of that behavior (#37363). Consumers contribute content; they do
 * not style the overflow or configure a tooltip of their own. There are deliberately no inputs.
 *
 * Two details are load-bearing:
 *
 * - `showOnEllipsis` makes PrimeNG compare `offsetWidth` against `scrollWidth` inside its own
 *   `activate()`, i.e. at the moment the pointer or focus arrives, on this very element. That is
 *   what suppresses the tooltip on names that fit, keeps the decision correct after the panel is
 *   resized, and costs nothing per row while the tree renders. It also means the tooltip has to
 *   sit on the element that clips — an ancestor never reports an ellipsis.
 * - The tooltip text is read from the rendered content rather than passed in, so it cannot
 *   disagree with the row. A consumer that displays different wording (the Asset Picker labels
 *   the tree root with a localized string while the node carries the hostname) gets a tooltip
 *   that matches what is on screen.
 */
@Component({
    selector: 'dot-truncated-label',
    imports: [Tooltip],
    template: `
        <span
            #label
            class="block min-w-0 truncate"
            data-testid="tree-node-label-clip"
            data-dot-truncated-label
            [pTooltip]="$tooltip()"
            [showOnEllipsis]="true"
            [showDelay]="showDelay"
            [pTooltipPT]="tooltipPt"
            tooltipEvent="both"
            tooltipPosition="right"
            (mouseenter)="syncTooltip()"
            (focusin)="syncTooltip()">
            <ng-content />
        </span>
    `,
    host: { class: 'block min-w-0' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTruncatedLabelComponent {
    /**
     * Keeps a long name on one line inside the tooltip instead of letting it wrap into a block.
     * A stable reference on purpose: PrimeNG re-applies pass-through options whenever the object
     * identity changes.
     */
    protected readonly tooltipPt = {
        root: { style: { maxWidth: 'none' } },
        text: { style: { whiteSpace: 'nowrap', wordBreak: 'normal' } }
    };

    protected readonly showDelay = TOOLTIP_SHOW_DELAY;

    /**
     * Text the tooltip reveals. Filled when the pointer or focus arrives, not on every change
     * detection pass: reading `textContent` in the binding itself would re-read it for every row
     * on every pass, and would be empty on the first one — projected content is not rendered yet
     * — which trips the dev-mode "changed after it was checked" check.
     */
    protected readonly $tooltip = signal('');

    // TS-private rather than `#private`: Angular rejects signal queries on ES private members.
    private readonly label = viewChild.required<ElementRef<HTMLElement>>('label');

    protected syncTooltip(): void {
        this.$tooltip.set(this.label().nativeElement.textContent?.trim() ?? '');
    }
}
