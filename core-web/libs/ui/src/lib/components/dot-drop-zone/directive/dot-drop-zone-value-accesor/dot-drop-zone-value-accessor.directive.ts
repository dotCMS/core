import { Directive, OnInit, forwardRef, inject } from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    Validator,
    NG_VALIDATORS,
    ValidationErrors
} from '@angular/forms';

import { DotDropZoneComponent, DropZoneFileEvent } from '../../dot-drop-zone.component';

@Directive({
    selector: '[dotDropZoneValueAccessor]',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotDropZoneValueAccessorDirective),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DotDropZoneValueAccessorDirective),
            multi: true
        }
    ]
})
export class DotDropZoneValueAccessorDirective implements ControlValueAccessor, Validator, OnInit {
    private _dotDropZone = inject(DotDropZoneComponent, { optional: true, host: true });

    private onChange: (value: File) => void;
    private onTouched: () => void;

    constructor() {
        if (!this._dotDropZone) {
            throw new Error(
                'dot-drop-zone-value-accessor can only be used inside of a dot-drop-zone'
            );
        }
    }

    ngOnInit() {
        this._dotDropZone.fileDropped.subscribe(({ file }: DropZoneFileEvent) => {
            this.onChange(file); // Only File
            this.onTouched();
        });
    }

    writeValue(_value: unknown) {
        /*
            We can set a value here by doing this._dotDropZone.setFile(value), if needed
        */
    }

    registerOnChange(fn: (value: unknown) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    validate(_control: AbstractControl): ValidationErrors | null {
        const validity = this._dotDropZone.validity;

        if (validity.valid) {
            return null;
        }

        const errors = Object.entries(validity).reduce((acc, [key, value]) => {
            if (value === true) {
                acc[key] = value;
            }

            return acc;
        }, {});

        return errors;
    }
}
