import { Directive, ElementRef, Input, Renderer2 } from '@angular/core';
import { FormGroupDirective, Validators } from '@angular/forms';

@Directive({
    selector: '[dotFieldRequired]',
    standalone: true
})
export class DotFieldRequiredDirective {
    constructor(
        private el: ElementRef,
        private renderer: Renderer2,
        private formGroupDirective: FormGroupDirective
    ) {
        renderer.addClass(this.el.nativeElement, 'p-label-input-required');
    }

    @Input()
    set checkIsRequiredControl(controlName: string) {
        if (!this.isRequiredControl(controlName)) {
            this.renderer.removeClass(this.el.nativeElement, 'p-label-input-required');
        }
    }

    private isRequiredControl(controlName: string): boolean {
        const formControl = this.formGroupDirective.control?.get(controlName);

        return formControl && formControl.hasValidator(Validators.required) ? true : false;
    }
}
