import { AfterViewInit, Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';

/**
 * Directive for trimming the input value on blur.
 */
@Directive({
    selector: '[dotTrimInput]'
})
export class DotTrimInputDirective implements AfterViewInit {
    private readonly ngControl = inject(NgControl, { optional: true, self: true });
    private readonly el = inject(ElementRef);

    @HostListener('blur')
    onBlur() {
        this.ngControl.control.setValue(this.ngControl.value.trim());
    }

    ngAfterViewInit(): void {
        if (this.el.nativeElement.tagName.toLowerCase() !== 'input') {
            console.warn('DotTrimInputDirective is for use with Inputs');
        }
    }
}
