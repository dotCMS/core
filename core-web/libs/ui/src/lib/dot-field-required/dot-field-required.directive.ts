import { Directive, ElementRef, Input, Renderer2, inject } from '@angular/core';
import { FormGroupDirective, Validators } from '@angular/forms';

/**
 * The purpose of this directive is to add or remove a class p-label-input-required to a HTML element based on whether the input control is required or not.
 * The directive has a selector [dotFieldRequired] which can be used to apply this directive to any HTML element. It also has an input property checkIsRequiredControl which takes a string parameter controlName which is the name of the control to check if it is required or not.
 */

@Directive({
    selector: '[dotFieldRequired]',
    standalone: true
})
export class DotFieldRequiredDirective {
    private el = inject(ElementRef);
    private renderer = inject(Renderer2);
    private formGroupDirective = inject(FormGroupDirective);

    constructor() {
        this.renderer.addClass(this.el.nativeElement, 'p-label-input-required');
    }

    /**
     * Remove Required Class if it is not required
     * @memberof DotFieldRequiredDirective
     * @param {string} controlName
     */
    @Input()
    set checkIsRequiredControl(controlName: string) {
        if (!this.isRequiredControl(controlName)) {
            this.renderer.removeClass(this.el.nativeElement, 'p-label-input-required');
        }
    }

    /**
     * Helper function for check control is required or not
     * @private
     * @param {string} controlName
     * @return {*}  {boolean}
     * @memberof DotFieldRequiredDirective
     */
    private isRequiredControl(controlName: string): boolean {
        const formControl = this.formGroupDirective.control?.get(controlName);

        return formControl && formControl.hasValidator(Validators.required) ? true : false;
    }
}
