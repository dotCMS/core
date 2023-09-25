import { Directive, HostListener } from '@angular/core';
import { NgControl } from '@angular/forms';

/**
 * Directive to trim input value on blur event.
 */
@Directive({
    selector: '[dotTrimInput]',
    standalone: true
})
export class DotExperimentsTrimInputDirective {
    constructor(private control: NgControl) {}

    @HostListener('blur')
    onBlur() {
        const value: string = this.control.value;
        this.control.control.setValue(value.trim());
    }
}
