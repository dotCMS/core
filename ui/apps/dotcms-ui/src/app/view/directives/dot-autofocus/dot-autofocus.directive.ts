import { Directive, ElementRef, OnInit } from '@angular/core';

@Directive({
    selector: '[dotAutofocus]'
})
export class DotAutofocusDirective implements OnInit {
    constructor(private el: ElementRef) {}

    ngOnInit() {
        if (!this.el.nativeElement.disabled) {
            setTimeout(() => {
                this.el.nativeElement.focus();
            }, 100);
        }
    }
}
