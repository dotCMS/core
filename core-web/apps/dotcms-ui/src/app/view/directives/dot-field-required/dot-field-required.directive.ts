import { Directive, ElementRef, Input, Renderer2 } from '@angular/core';
import { FormGroupDirective } from '@angular/forms';

@Directive({
    selector: '[dotFieldRequired]'
})
export class DotFieldRequiredDirective {
    @Input() for: string;

    constructor(
        private el: ElementRef,
        private fg: FormGroupDirective,
        private renderer: Renderer2
    ) {}
    ngOnInit() {
        if (this.fg?.form.controls[this.for]?.errors?.required) {
            this.renderer.addClass(this.el.nativeElement, 'p-label-input-required');
        }
    }
}
