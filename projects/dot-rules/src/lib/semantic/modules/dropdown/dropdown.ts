import { MenuItem } from 'primeng/primeng';

import {
    Component,
    EventEmitter,
    Optional,
    OnChanges,
    SimpleChanges
} from '@angular/core';
import { Output, Input, ChangeDetectionStrategy } from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';
import { of, Observable, from } from 'rxjs';
import * as _ from 'lodash';
import { map, mergeMap, toArray } from 'rxjs/operators';

/**
 * Angular wrapper around OLD Semantic UI Dropdown Module.
 *
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-input-dropdown',
    template: `
        <p-dropdown
            ng-valid
            class="ui fluid ng-valid"
            appendTo="body"
            [style]="{ width: '100%' }"
            [required]="minSelections > 0"
            [placeholder]="placeholder"
            [options]="dropdownOptions | async"
            [editable]="allowAdditions"
            [filter]="true"
            [(ngModel)]="modelValue"
            *ngIf="maxSelections <= 1"
            (onChange)="fireChange($event.value)"
        >
        </p-dropdown>
        <dot-autocomplete-tags
            [inputId]="name"
            *ngIf="maxSelections > 1"
            [value]="modelValue"
            [options]="dropdownOptions | async"
            [placeholder]="placeholder"
            (onChange)="fireChange($event)"
        >
        </dot-autocomplete-tags>
    `
})
export class Dropdown implements ControlValueAccessor, OnChanges {
    @Input()
    set value(value: string) {
        this.modelValue = value;
    }
    @Input() options: any;
    @Input() name: string;
    @Input() placeholder: string;
    @Input() allowAdditions: boolean;
    @Input() minSelections: number;
    @Input() maxSelections: number;

    @Output() onDropDownChange: EventEmitter<any> = new EventEmitter();
    @Output() touch: EventEmitter<any> = new EventEmitter();
    @Output() enter: EventEmitter<boolean> = new EventEmitter(false);

    modelValue: string;
    dropdownOptions: Observable<MenuItem[]>;

    constructor(@Optional() control: NgControl) {
        if (control && !control.valueAccessor) {
            control.valueAccessor = this;
        }
        this.placeholder = '';
        this.allowAdditions = false;
        this.minSelections = 0;
        this.maxSelections = 1;
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes.options && changes.options.currentValue) {
            this.dropdownOptions = from(this.options).pipe(
                mergeMap((item: {[key: string]: any}) => {
                    if (item.label.pipe) {
                        return item.label.pipe(
                            map((text: string) => {
                                return {
                                    label: text,
                                    value: item.value
                                };
                            })
                        );
                    }

                    return of({
                        label: item.label,
                        value: item.value
                    });
                }),
                toArray()
            );
        }
    }

    onChange: Function = () => {};
    onTouched: Function = () => {};

    writeValue(value: any): void {
        this.modelValue = _.isEmpty(value) ? '' : value;
    }

    registerOnChange(fn): void {
        this.onChange = fn;
    }

    registerOnTouched(fn): void {
        this.onTouched = fn;
    }

    fireChange(value: any): void {
        this.modelValue = value;
        if (this.onDropDownChange) {
            this.onDropDownChange.emit(value);
            this.onChange(value);
            this.fireTouch(value);
        }
    }

    fireTouch($event): void {
        this.onTouched();
        this.touch.emit($event);
    }
}
