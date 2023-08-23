import { Directive, Host, OnInit, Optional, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotDropZoneComponent } from '../../dot-drop-zone.component';

@Directive({
    selector: '[dotDropZoneValueAccessor]',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotDropZoneValueAccessorDirective),
            multi: true
        }
    ]
})
export class DotDropZoneValueAccessorDirective implements ControlValueAccessor, OnInit {
    private onChange: (value: File[]) => void;
    private onTouched: () => void;

    constructor(@Optional() @Host() private _dotDropZone: DotDropZoneComponent) {}

    ngOnInit() {
        this._dotDropZone.fileDrop.subscribe((files: File[]) => {
            this.onChange(files);
            this.onTouched();
        });
    }

    writeValue(_value: unknown) {
        /* noop */
    }

    registerOnChange(fn: (value: unknown) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }
}
