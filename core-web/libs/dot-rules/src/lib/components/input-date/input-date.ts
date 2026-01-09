import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    inject
} from '@angular/core';
import { NgControl, ControlValueAccessor } from '@angular/forms';

import { isEmpty } from '@dotcms/utils';

// @dynamic
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    // host: { role: 'text' },
    selector: 'cw-input-date',
    template: `
        <p-calendar
            (onBlur)="onBlur($event)"
            (onSelect)="updateValue($event)"
            [(ngModel)]="modelValue"
            [showTime]="true"
            [placeholder]="placeholder"
            [disabled]="disabled"
            [tabindex]="tabIndex || ''"
            hourFormat="12"
            showButtonBar="true" />
    `,
    standalone: false
})
export class InputDate implements ControlValueAccessor {
    private _elementRef = inject(ElementRef);

    private static DEFAULT_VALUE: Date;
    @Input() placeholder = '';
    @Input() type = '';
    @Input() value = '';
    @Input() icon: string;
    @Input() disabled = false;
    @Input() focused = false;
    @Input() tabIndex: number = null;
    @Input() required = false;

    @Output() blur: EventEmitter<any> = new EventEmitter();

    errorMessage: string;
    onChange: Function;
    onTouched: Function;
    modelValue: Date;

    private static _defaultValue(): Date {
        const d = new Date();

        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        d.setMilliseconds(0);
        d.setMonth(d.getMonth() + 1);
        d.setDate(1);

        return d;
    }

    constructor() {
        const control = inject(NgControl, { optional: true });

        if (control) {
            control.valueAccessor = this;
        }

        if (!InputDate.DEFAULT_VALUE) {
            InputDate.DEFAULT_VALUE = InputDate._defaultValue();
        }
    }

    ngOnChanges(change): void {
        if (change.focused) {
            const f =
                change.focused.currentValue === true || change.focused.currentValue === 'true';
            if (f) {
                const el = this._elementRef.nativeElement;
                el.children[0].children[0].focus();
            }
        }
    }

    onBlur(_value): void {
        // this.onTouched();
        // this.blur.emit(value);
    }

    updateValue(value): void {
        this.value = this.convertToISOFormat(value);
        this.modelValue = value;
        this.onChange(this.value);
        this.onTouched();
        this.blur.emit(value);
    }

    writeValue(value: any): void {
        this.modelValue = isEmpty(value) ? InputDate.DEFAULT_VALUE : new Date(value);
    }

    registerOnChange(fn): void {
        this.onChange = fn;
    }

    registerOnTouched(fn): void {
        this.onTouched = fn;
    }

    private convertToISOFormat(value: Date): string {
        const offset = new Date().getTimezoneOffset() * 60000;

        return new Date(value.getTime() - offset).toISOString().slice(0, -5);
    }
}
