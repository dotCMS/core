import { booleanAttribute, ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Body slot of {@link DotDialogComponent}: the region between the header and the footer.
 *
 * `min-h-0 flex-1` is what makes the shell work — it lets an inner region scroll instead of
 * stretching the dialog past its height. The clipping (`overflow-hidden`) lives on the shell host,
 * so this component adds no clipping context of its own and PrimeNG overlays opened inside the body
 * still escape it.
 */
@Component({
    selector: 'dot-dialog-content',
    templateUrl: './dot-dialog-content.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'block min-h-0 min-w-0 flex-1',
        '[class.overflow-y-auto]': '$scroll()',
        '[class.p-4]': '$padded()'
    }
})
export class DotDialogContentComponent {
    /**
     * Whether the body itself scrolls. Dialogs whose *inner* regions scroll (a list, a side panel)
     * leave this off, so the scrollbar stays on the region that owns it.
     *
     * @alias scroll
     */
    readonly $scroll = input(false, { alias: 'scroll', transform: booleanAttribute });

    /**
     * Adds the canonical body padding. Dialogs that lay out their own columns pad those instead.
     *
     * @alias padded
     */
    readonly $padded = input(false, { alias: 'padded', transform: booleanAttribute });
}
