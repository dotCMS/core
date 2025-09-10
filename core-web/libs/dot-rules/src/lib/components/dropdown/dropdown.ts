import { of, Observable, from } from 'rxjs';

import {
    Component,
    EventEmitter,
    OnChanges,
    SimpleChanges,
    ViewChild,
    Output,
    Input,
    ChangeDetectionStrategy,
    inject
} from '@angular/core';
import { ControlValueAccessor, NgControl } from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { Dropdown as PDropdown } from 'primeng/dropdown';

import { map, mergeMap, toArray } from 'rxjs/operators';

import { isEmpty } from '@dotcms/utils';

/**
 * Angular wrapper around OLD Semantic UI Dropdown Module.
 *
 */
@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'cw-input-dropdown',
    template: `
        <p-dropdown
            *ngIf="maxSelections <= 1"
            (onChange)="fireChange($event.value)"
            [(ngModel)]="modelValue"
            [style]="{ width: '100%' }"
            [required]="minSelections > 0"
            [placeholder]="placeholder"
            [options]="dropdownOptions | async"
            [editable]="allowAdditions"
            [filter]="true"
            #inputDropdown
            ng-valid
            class="ui fluid ng-valid"
            appendTo="body"></p-dropdown>
        <dot-autocomplete-tags
            *ngIf="maxSelections > 1"
            (onChange)="fireChange($event)"
            [inputId]="name"
            [value]="modelValue"
            [options]="dropdownOptions | async"
            [placeholder]="placeholder"></dot-autocomplete-tags>
    `,
    standalone: false
})
export class Dropdown implements ControlValueAccessor, OnChanges {
    @Input()
    set value(value: string) {
        this.modelValue = value;
    }
    @Input() focus: boolean;
    @Input() options: any;
    @Input() name: string;
    @Input() placeholder: string;
    @Input() allowAdditions: boolean;
    @Input() minSelections: number;
    @Input() maxSelections: number;

    @Output() onDropDownChange: EventEmitter<any> = new EventEmitter();
    @Output() touch: EventEmitter<any> = new EventEmitter();
    @Output() enter: EventEmitter<boolean> = new EventEmitter(false);

    @ViewChild('inputDropdown') inputDropdown: PDropdown;

    modelValue: string;
    dropdownOptions: Observable<SelectItem[]>;

    constructor() {
        const control = inject(NgControl, { optional: true });

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
            this.dropdownOptions = <Observable<SelectItem[]>>from(this.options).pipe(
                mergeMap((item: { [key: string]: any }) => {
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

        this.isFocusSet(changes);
    }

    isFocusSet: Function = (changes: SimpleChanges) => {
        if (changes.focus && changes.focus.currentValue) {
            setTimeout(() => {
                this.inputDropdown.focus();
            }, 0);
        }
    };

    onChange: Function = () => {};
    onTouched: Function = () => {};

    writeValue(value: any): void {
        this.modelValue = isEmpty(value) ? '' : value;
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
