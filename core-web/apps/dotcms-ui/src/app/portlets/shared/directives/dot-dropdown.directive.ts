import { Directive, Input, Optional, Self } from '@angular/core';

import { Dropdown } from 'primeng/dropdown';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { ExperimentsSelectOption } from '@dotcms/dotcms-models';

const DEFAULT_LABEL_NAME_INDEX = 'label';
const DEFAULT_VALUE_NAME_INDEX = 'value';

/**
 * Directive to set a default configuration of Dropdown (PrimeNG) and translate the label of the options
 *
 * @export
 * @class DotDropdownDirective
 */
@Directive({
    standalone: true,
    selector: '[dotDropdown]',
    providers: [DotMessagePipe]
})
export class DotDropdownDirective {
    constructor(
        @Optional() @Self() private readonly primeDropdown: Dropdown,
        private readonly dotMessagePipe: DotMessagePipe
    ) {
        if (primeDropdown) {
            primeDropdown.optionLabel = DEFAULT_LABEL_NAME_INDEX;
            primeDropdown.optionValue = DEFAULT_VALUE_NAME_INDEX;
            primeDropdown.showClear = true;
        } else {
            console.warn('DotDropdownDirective is for use with PrimeNg Dropdown');
        }
    }

    /**
     *Array of option to translate LABEL_NAME and assign to Dropdown
     *
     * @param {Array<ExperimentsSelectOption<string>>} options - Options of Dropdown
     * @memberof DotDropdownDirective
     */
    @Input()
    set dotOptions(options: Array<ExperimentsSelectOption<string>>) {
        this.primeDropdown.options = options.map((opt) => {
            return {
                ...opt,
                [DEFAULT_LABEL_NAME_INDEX]: this.dotMessagePipe.transform(
                    opt[DEFAULT_LABEL_NAME_INDEX]
                )
            };
        });
    }
}
