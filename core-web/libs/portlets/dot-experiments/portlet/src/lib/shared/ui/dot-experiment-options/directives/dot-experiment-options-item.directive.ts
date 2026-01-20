import { ContentChild, Directive, Input, inject } from '@angular/core';

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

    val!: string;

    @Input()
    title: string;
    @Input()
    detail: string;
    @Input()
    icon: string;

    @ContentChild(DotExperimentOptionContentDirective)
    content: DotExperimentOptionContentDirective;

    get value() {
        return this.val;
    }

    @Input()
    set value(value: string) {
        this.val = value;
    }

    /**
     * Select and Open content of the option
     * @param item
     * @param index
     */
    selectItem(item: DotExperimentOptionsItemDirective, index: number) {
        this._select.setOptionSelected(item.value);
        this._select.toggleOption(index);
    }
}
