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
        const target: HTMLInputElement = event.target as unknown as HTMLInputElement;

        if (event.key === 'Enter' && !!target.value) {
            // TODO: find a way to get the selected item from the autocomplete selectItem method was removed since v.16
            // this.autoComplete.selectItem(target.value);
        }
    }
}
