import { ContentChild, Directive, Host, Input, Optional } from '@angular/core';

import { DotExperimentOptionContentDirective } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/directives/dot-experiment-option-content.directive';
import { DotExperimentOptionsComponent } from '@portlets/dot-experiments/shared/ui/dot-experiment-options/dot-experiment-options.component';

/**
 * Options item of the control
 */
@Directive({
    // eslint-disable-next-line @angular-eslint/directive-selector
    selector: 'dot-experiment-options-item'
})
export class DotExperimentOptionsItemDirective {
    val!: string;

    @Input()
    title: string;
    @Input()
    detail: string;

    @ContentChild(DotExperimentOptionContentDirective)
    content: DotExperimentOptionContentDirective;

    constructor(@Optional() @Host() private _select: DotExperimentOptionsComponent) {}

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
