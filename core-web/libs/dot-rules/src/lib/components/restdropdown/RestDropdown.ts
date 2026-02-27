import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import {
    Component,
    EventEmitter,
    OnChanges,
    AfterViewInit,
    Output,
    Input,
    ChangeDetectionStrategy,
    inject
} from '@angular/core';
import { NgControl, ControlValueAccessor } from '@angular/forms';

import { map } from 'rxjs/operators';

import { isEmpty } from '@dotcms/utils';

import { Verify } from '../../services/validation/Verify';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-input-rest-dropdown',
    template: `
        <cw-input-dropdown
            (onDropDownChange)="fireChange($event)"
            (touch)="fireTouch($event)"
            [value]="modelValue"
            [maxSelections]="maxSelections"
            [minSelections]="minSelections"
            [allowAdditions]="allowAdditions"
            [options]="options | async"
            placeholder="{{ placeholder }}" />
    `,
    standalone: false
})
export class RestDropdown implements AfterViewInit, OnChanges, ControlValueAccessor {
    private http = inject(HttpClient);
    control = inject(NgControl, { optional: true });

    @Input() placeholder: string;
    @Input() allowAdditions: boolean;
    @Input() minSelections: number;
    @Input() maxSelections: number;
    @Input() optionUrl: string;
    @Input() optionValueField: string;
    @Input() optionLabelField: string;
    @Input() value: string;

    @Output() change: EventEmitter<any> = new EventEmitter();
    @Output() touch: EventEmitter<any> = new EventEmitter();

    private _modelValue: string[] | string;
    private _options: Observable<any[]>;

    constructor() {
        const control = this.control;

        if (control) {
            control.valueAccessor = this;
        }

        this.placeholder = '';
        this.optionValueField = 'key';
        this.optionLabelField = 'value';
        this.allowAdditions = false;
        this.minSelections = 0;
        this.maxSelections = 1;
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unsafe-function-type
    onChange: Function = () => {};
    // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unsafe-function-type
    onTouched: Function = () => {};

    // Required by AfterViewInit interface but not used in this component
    // eslint-disable-next-line @angular-eslint/no-empty-lifecycle-method
    ngAfterViewInit(): void {
        // No implementation needed
    }

    writeValue(value: any): void {
        if (value && value.indexOf(',') > -1) {
            this._modelValue = value.split(',');
        } else {
            this._modelValue = isEmpty(value) ? null : value;
        }
    }

    get modelValue() {
        return this._modelValue;
    }

    get options() {
        return this._options;
    }

    registerOnChange(fn): void {
        this.onChange = fn;
    }

    registerOnTouched(fn): void {
        this.onTouched = fn;
    }

    fireChange($event): void {
        $event = Array.isArray($event) ? $event.join() : $event;
        this.change.emit($event);
        this.onChange($event);
    }

    fireTouch($event): void {
        this.onTouched($event);
        this.touch.emit($event);
    }

    ngOnChanges(change): void {
        if (change.optionUrl) {
            this._options = this.http
                .get<any>(change.optionUrl.currentValue)
                .pipe(map((res: any) => this.jsonEntriesToOptions(res)));
        }

        if (
            change.value &&
            typeof change.value.currentValue === 'string' &&
            this.maxSelections > 1
        ) {
            this._modelValue = change.value.currentValue.split(',');
        }
    }

    private jsonEntriesToOptions(res: any): any {
        const valuesJson = res;
        let ary = [];
        if (Verify.isArray(valuesJson)) {
            ary = valuesJson.map((valueJson) => this.jsonEntryToOption(valueJson));
        } else {
            ary = Object.keys(valuesJson).map((key) => {
                return this.jsonEntryToOption(valuesJson[key], key);
            });
        }

        return ary;
    }

    private jsonEntryToOption(json: any, key: string = null): { value: string; label: string } {
        const opt = { value: null, label: null };
        if (!json[this.optionValueField] && this.optionValueField === 'key' && key != null) {
            opt.value = key;
        } else {
            opt.value = json[this.optionValueField];
        }

        opt.label = json[this.optionLabelField];

        return opt;
    }
}
