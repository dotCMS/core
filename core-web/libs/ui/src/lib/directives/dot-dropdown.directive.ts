import { Directive, Input, inject } from '@angular/core';

import { Dropdown } from 'primeng/dropdown';
import { MultiSelect } from 'primeng/multiselect';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropdownSelectOption } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../dot-message/dot-message.pipe';

const DEFAULT_LABEL_NAME_INDEX = 'label';
const DEFAULT_VALUE_NAME_INDEX = 'value';

/**
 * Directive to set a default configuration of Dropdown or MultiSelect (PrimeNG) and translate the label of the options
 *
 * @export
 * @class DotDropdownDirective
 */
@Directive({
    selector: '[dotDropdown]',
    providers: [DotMessagePipe]
})
export class DotDropdownDirective {
    private readonly primeDropdown = inject(Dropdown, { optional: true, self: true });
    private readonly primeMultiSelect = inject(MultiSelect, { optional: true, self: true });
    private readonly dotMessageService = inject(DotMessageService);

    control: Dropdown | MultiSelect;

    constructor() {
        this.control = this.primeDropdown ? this.primeDropdown : this.primeMultiSelect;

        if (this.control) {
            this.control.optionLabel = DEFAULT_LABEL_NAME_INDEX;
            this.control.optionValue = DEFAULT_VALUE_NAME_INDEX;
            this.control.showClear = this.control instanceof Dropdown ? true : false;
        } else {
            console.warn('DotDropdownDirective is for use with PrimeNg Dropdown or MultiSelect');
        }
    }

    /**
     *Array of options to translate LABEL_NAME and assign to Dropdown
     *
     * @param {Array<DotDropdownSelectOption<string>>} options - Options of Dropdown
     * @memberof DotDropdownDirective
     */
    @Input()
    set dotOptions(options: Array<DotDropdownSelectOption<string>>) {
        if (options) {
            this.setOptions(options);
        }
    }

    private setOptions(options: Array<DotDropdownSelectOption<string>>) {
        this.control.options = options.map((opt) => {
            return {
                ...opt,
                [DEFAULT_LABEL_NAME_INDEX]: this.dotMessageService.get(
                    opt[DEFAULT_LABEL_NAME_INDEX]
                )
            };
        });
    }
}
