import { Directive, ElementRef, OnInit, inject } from '@angular/core';

@Directive({
    selector: '[dotAutofocus]',
    standalone: false
})
export class DotAutofocusDirective implements OnInit {
    private el = inject(ElementRef);

    ngOnInit() {
        if (!this.el.nativeElement.disabled) {
            setTimeout(() => {
                this.el.nativeElement.focus();
            }, 100);
        }
    }
}
