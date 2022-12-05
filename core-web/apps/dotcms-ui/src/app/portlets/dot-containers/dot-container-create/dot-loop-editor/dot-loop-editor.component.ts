import { animate, style, transition, trigger } from '@angular/animations';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    animations: [
        trigger('enterAnimation', [
            transition(':enter', [style({ opacity: 0 }), animate(500, style({ opacity: 1 }))])
        ])
    ],
    selector: 'dot-loop-editor',
    templateUrl: './dot-loop-editor.component.html',
    styleUrls: ['./dot-loop-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLoopEditorComponent),
            multi: true
        }
    ]
})
export class DotLoopEditorComponent implements ControlValueAccessor, OnInit {
    @Input() isEditorVisible = false;
    @Input() label = '';
    @Output() buttonClick = new EventEmitter();

    public readonly loopControl = new FormControl('');

    constructor() {
        //
    }

    public ngOnInit(): void {
        this.loopControl.valueChanges.subscribe((fieldVal) => {
            this._onChange(fieldVal);
            this.onTouched();
        });
    }

    public writeValue(value: string | null): void {
        value = value ?? '';
        this.loopControl.setValue(value);
    }

    public setDisabledState(isDisabled: boolean): void {
        if (isDisabled) {
            this.loopControl.disable();
        } else {
            this.loopControl.enable();
        }
    }

    private _onChange = (_value: string | null) => undefined;

    public registerOnChange(fn: (value: string | null) => void): void {
        this._onChange = fn;
    }

    public onTouched = () => undefined;

    public registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * This method shows the Loop Editor Input
     *
     * @return void
     * @memberof DotLoopEditorComponent
     */
    handleClick(): void {
        this.buttonClick.emit();
    }
}
