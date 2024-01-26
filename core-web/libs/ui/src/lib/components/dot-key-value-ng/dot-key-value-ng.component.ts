import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    effect,
    forwardRef,
    signal
} from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';

import { TableModule } from 'primeng/table';

import { DotKeyValueTableInputRowComponent } from './dot-key-value-table-input-row/dot-key-value-table-input-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotKeyValue {
    key: string;
    hidden?: boolean;
    value: string;
}

@Component({
    selector: 'dot-key-value-ng',
    styleUrls: ['./dot-key-value-ng.component.scss'],
    templateUrl: './dot-key-value-ng.component.html',
    standalone: true,
    imports: [
        CommonModule,
        TableModule,
        DotKeyValueTableInputRowComponent,
        DotKeyValueTableRowComponent,
        DotMessagePipe
    ],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotKeyValueComponent),
            multi: true
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueComponent implements OnInit {
    @Input() autoFocus = true;
    @Input() showHiddenField: boolean;
    @Input() data: Record<string, string>;

    @Output() delete: EventEmitter<DotKeyValue> = new EventEmitter(false);
    @Output() save: EventEmitter<DotKeyValue> = new EventEmitter(false);

    value = signal<DotKeyValue[]>([]);

    private onChange: (value: DotKeyValue[]) => void;
    private onTouched: () => void;

    constructor() {
        effect(() => {
            this.onChange(this.value());
        });
    }

    ngOnInit() {
        if (this.data) {
            this.value.set(this.convertToKeyValue(this.data));
        }
    }

    writeValue(value: Record<string, string>): void {
        this.value.set(this.convertToKeyValue(value));
    }

    registerOnChange(fn: (value: DotKeyValue[]) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    /**
     * Handle Delete event, deleting the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    deleteVariable(index: number): void {
        this.value.update((variables) => {
            variables.splice(index, 1);

            return [...variables];
        });
    }

    /**
     * Handle Save event, saving the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    saveVariable(variable: DotKeyValue): void {
        this.value.update((variables) => {
            return [variable, ...variables];
        });
    }

    /**
     * Handle Save event, saving the variable locally and emitting
     * the variable to be handled by a parent smart component
     * @param {DotKeyValue} variable
     * @memberof DotKeyValueComponent
     */
    updateKeyValue(variable: DotKeyValue, index: number): void {
        this.value.update((variables) => {
            variables[index] = variable;

            return [...variables];
        });
    }

    private convertToKeyValue(data: Record<string, string>): DotKeyValue[] {
        if (!data) {
            return [];
        }

        return Object.keys(data).map((key: string) => {
            return {
                key: key,
                value: data[key]
            };
        });
    }
}
