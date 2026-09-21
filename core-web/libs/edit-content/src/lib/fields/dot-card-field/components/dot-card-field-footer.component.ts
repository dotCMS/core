import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The slot below a field's control, holding its required message and its hint.
 *
 * Both at once when both apply, error first: the hint says what to enter, which is exactly what
 * the author needs while the field is in error. Previously every template suppressed the hint the
 * moment an error appeared, so the author was told the field was required and simultaneously lost
 * the sentence explaining what to put in it.
 *
 * The stacking lives here rather than in each of the eighteen field templates — the same reason
 * `dot-card-field` exists. `:empty` still collapses the slot when a field has neither, so a
 * template must render genuinely nothing rather than whitespace.
 */
@Component({
    selector: 'dot-card-field-footer',
    template: `
        <ng-content />
    `,
    host: {
        class: 'flex flex-col gap-1'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldFooterComponent {}
