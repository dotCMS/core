import { Observable } from 'rxjs';

import {
    Component,
    EventEmitter,
    OnChanges,
    Optional,
    AfterViewInit,
    Output,
    Input,
    ChangeDetectionStrategy
} from '@angular/core';
import { NgControl, ControlValueAccessor } from '@angular/forms';

import { map } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
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
            placeholder="{{ placeholder }}"></cw-input-dropdown>
    `
})
export class RestDropdown implements AfterViewInit, OnChanges, ControlValueAccessor {
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

    constructor(
        private coreWebService: CoreWebService,
        @Optional() public control: NgControl
    ) {
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

    onChange: Function = () => {};
    onTouched: Function = () => {};

    ngAfterViewInit(): void {}

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
            this._options = this.coreWebService
                .request({
                    url: change.optionUrl.currentValue
                })
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
