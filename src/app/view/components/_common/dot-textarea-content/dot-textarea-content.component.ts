import { Component, Input, OnInit, forwardRef, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SelectItem } from 'primeng/primeng';
import { AceEditorComponent } from 'ng2-ace-editor';

@Component({
    selector: 'dot-textarea-content',
    templateUrl: './dot-textarea-content.component.html',
    styleUrls: ['./dot-textarea-content.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTextareaContentComponent)
        }
    ]
})
export class DotTextareaContentComponent implements OnInit, ControlValueAccessor {
    @ViewChild('ace') ace: AceEditorComponent;
    @Input()
    code: any = {
        mode: 'text',
        options: {}
    };
    @Input() height: string;
    @Input() show;
    @Input() value = '';
    @Input() width: string;

    private DEFAULT_OPTIONS: SelectItem[] = [
        { label: 'Plain', value: 'plain' },
        { label: 'Code', value: 'code' },
        { label: 'WYSIWYG', value: 'wysiwyg' }
    ];

    selectOptions: SelectItem[] = [];
    selected: string;
    styles: any;

    propagateChange = (_: any) => {};

    constructor() {}

    ngOnInit() {
        this.selectOptions = this.show
            ? this.show
                .map(item => {
                    return this.DEFAULT_OPTIONS.find(option => option.value === item);
                })
                .filter(item => item) // Remove undefined values in the array
            : this.DEFAULT_OPTIONS;

        this.selected = this.selectOptions[0].value;

        this.propagateChange(this.value);

        this.styles = {
            width: this.width || '100%',
            height: this.height || '300px'
        };
    }

    /**
     * Update the value and form control
     *
     * @param {any} value
     * @memberof DotTextareaContentComponent
     */
    onModelChange(value) {
        this.value = value;
        this.propagateChange(value);
    }

    /**
     * Update model with external value
     *
     * @param {string} value
     * @memberof DotTextareaContentComponent
     */
    writeValue(value: string): void {
        if (value) {
            this.value = value || '';
        }
    }

    /**
     * Set the call callback to update value on model change
     *
     * @param {any} fn
     * @memberof DotTextareaContentComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}
}
