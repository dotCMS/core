import { AfterViewInit, Directive, ElementRef, HostListener, Optional, Self } from '@angular/core';
import { NgControl } from '@angular/forms';

/**
 * Directive for trimming the input value on blur.
 */
@Directive({
    selector: '[dotTrimInput]',
    standalone: true
})
export class DotTrimInputDirective implements AfterViewInit {
    constructor(
        @Optional() @Self() private readonly ngControl: NgControl,
        private readonly el: ElementRef
    ) {}

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
