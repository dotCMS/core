import { Directive, ElementRef, OnInit, Input } from '@angular/core';

@Directive({
    selector: '[dotAutofocus]'
})
export class DotAutofocusDirective implements OnInit {
    @Input() condition: string;

    private _autofocus;
    constructor(private el: ElementRef) {}

    ngOnInit() {
        if (this._autofocus || typeof this._autofocus === 'undefined') {
            setTimeout(() => {
                this.el.nativeElement.focus();
            }, 100);
        }
    }


}
