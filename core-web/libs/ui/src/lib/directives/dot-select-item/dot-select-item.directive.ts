import { Directive, HostListener, Optional, Self } from '@angular/core';

import { AutoComplete } from 'primeng/autocomplete';

@Directive({
    selector: 'p-autoComplete[dotSelectItem]',
    standalone: true
})
export class DotSelectItemDirective {
    constructor(@Optional() @Self() private autoComplete: AutoComplete) {}

    /**
     * Listen to keyup event and select the item if the user press enter
     *
     * @param {KeyboardEvent} event
     * @memberof DotSelectItemDirective
     */
    @HostListener('onKeyUp', ['$event'])
    onKeyUp(event: KeyboardEvent) {
        const value = (event.target as HTMLInputElement).value;
        if (event.key === 'Enter' && value) {
            this.autoComplete.suggestions.push(value);
            const options = this.autoComplete.visibleOptions();
            const size = options.length;
            this.autoComplete.onOptionSelect(event, options[size - 1]);
        }
    }
}
