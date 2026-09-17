import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { DotFieldRequiredDirective } from '@dotcms/ui';

/**
 * The label of a field in the new Edit Contentlet.
 *
 * The selector is an **attribute on a `<label>`**, not an element of its own, and that is the
 * whole point. `.form .field > label` in `apps/dotcms-ui/src/style.css` is a direct-child
 * selector; while this component rendered its own `<label>` inside a `<dot-card-field-label>`
 * host, that host was the label's DOM parent and the rule never matched — so every label in the
 * editor kept the inherited 14px/400 instead of the 12.25px/500 every other admin form uses.
 *
 * `display: contents` on a wrapping host does NOT fix that: `display` governs box generation,
 * while selectors match on the DOM tree. It would have collapsed the layout correctly and left
 * the typography untouched — a failure that looks like success. Making the host *be* the label
 * is what makes the global rule apply, by structure rather than by trick.
 *
 * @example
 * ```html
 * <label dotCardFieldLabel [variableName]="field.variable" [isRequired]="isRequired">
 *     {{ field.name }}
 * </label>
 * ```
 */
@Component({
    // The lib's component-selector rule expects an element selector prefixed `dot`. This one has
    // to be an attribute on a native `<label>`: `.form .field > label` is a direct-child selector,
    // and a `<dot-card-field-label>` host between `.field` and the label is precisely what stops
    // it matching. The attribute itself carries the `dot` prefix.
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'label[dotCardFieldLabel]',
    templateUrl: './dot-card-field-label.component.html',
    // The asterisk belongs to the directive, never to a hand-written `p-label-input-required`:
    // that class is the directive's private output. A component cannot apply a directive to its
    // own host from its template, so it is declared here and `isRequired` is forwarded to it as
    // the directive's boolean mode.
    hostDirectives: [
        {
            directive: DotFieldRequiredDirective,
            inputs: ['dotFieldRequired: isRequired']
        }
    ],
    host: {
        '[attr.for]': '$variableName()',
        // A composite widget — a radio or checkbox group — is a <div>, which `<label for>` cannot
        // associate with, since `for` only reaches labelable elements. Those groups name themselves
        // with aria-labelledby pointing here, so the label needs a stable id of its own.
        '[attr.id]': '$testId()',
        '[attr.data-testid]': '$testId()'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldLabelComponent {
    /**
     * The variable name of the content type field. Doubles as the id of the control this label is
     * for, which is what the host `for` binding points at.
     *
     * @required
     */
    $variableName = input.required<string>({ alias: 'variableName' });

    /**
     * Kept as `label-<variable>`: specs across the library select on that exact format.
     */
    protected $testId = computed(() => `label-${this.$variableName()}`);
}
