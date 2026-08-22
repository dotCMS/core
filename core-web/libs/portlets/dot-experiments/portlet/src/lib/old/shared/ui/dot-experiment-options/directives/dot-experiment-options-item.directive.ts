import { ContentChild, Directive, input, inject } from '@angular/core';

import { DotExperimentOptionContentDirective } from './dot-experiment-option-content.directive';

import { DotExperimentOptionsComponent } from '../dot-experiment-options.component';

/**
 * Options item of the control
 */
@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'dot-experiment-options-item',
    standalone: true
})
export class DotExperimentOptionsItemDirective {
    private _select = inject(DotExperimentOptionsComponent, { optional: true, host: true });

    $title = input.required<string>({ alias: 'title' });
    $value = input.required<string>({ alias: 'value' });
    $detail = input<string>('', { alias: 'detail' });
    $icon = input<string>('', { alias: 'icon' });

    @ContentChild(DotExperimentOptionContentDirective)
    content!: DotExperimentOptionContentDirective;

    /**
     * Select and Open content of the option
     * @param item
     * @param index
     */
    selectItem(item: DotExperimentOptionsItemDirective, index: number) {
        // `_select` is injected `{ optional: true }`, so it is null when this directive is used
        // outside a `dot-experiment-options` host — there is no selection to change then.
        this._select?.setOptionSelected(item.$value());
        this._select?.toggleOption(index);
    }
}
