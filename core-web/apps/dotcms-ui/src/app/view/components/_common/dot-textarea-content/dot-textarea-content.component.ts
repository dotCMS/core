import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    forwardRef,
    HostBinding,
    Input,
    OnInit,
    Output,
    inject
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';

import { SelectItem } from 'primeng/api';

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
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotTextareaContentComponent implements OnInit, ControlValueAccessor {
    private sanitizer = inject(DomSanitizer);

    @Input()
    code = {
        mode: 'text',
        options: {}
    };

    @Input()
    height: string;

    @Input()
    show;

    @Input()
    value = '';

    @Input()
    width: string;

    @Input()
    customStyles: Record<string, unknown>;

    @Input()
    editorName: string;

    @Output()
    monacoInit = new EventEmitter<unknown>();

    @Output()
    valueChange = new EventEmitter<string>();

    @Input() set language(value: string) {
        this.editorOptions = {
            ...this.editorOptions,
            language: value
        };
    }

    @HostBinding('style')
    get myStyle(): SafeStyle {
        return this.sanitizer.bypassSecurityTrustStyle(this.styles as string);
    }

    selectOptions: SelectItem[] = [];
    selected: string;
    styles: Record<string, unknown> | string;
    editorOptions: MonacoEditorConstructionOptions = {
        theme: 'vs-light',
        minimap: {
            enabled: false
        },
        cursorBlinking: 'solid',
        overviewRulerBorder: false,
        mouseWheelZoom: false,
        lineNumbers: 'on',
        selectionHighlight: false,
        roundedSelection: false,
        selectOnLineNumbers: false,
        columnSelection: false,
        language: 'text/plain'
    };

    private DEFAULT_OPTIONS: SelectItem[] = [
        { label: 'Plain', value: 'plain' },
        { label: 'Code', value: 'code' }
    ];

    propagateChange = (_: string) => {
        /**/
    };

    ngOnInit() {
        this.selectOptions = this.getSelectOptions();
        this.selected = this.selectOptions[0]?.value;

        this.propagateChange(this.value);

        this.styles = {
            width: this.width || '100%',
            height: this.height || '21.42rem'
        };

        if (this.customStyles) {
            this.styles = {
                ...this.styles,
                ...this.customStyles
            };
        }
    }

    /**
     * Prevent enter to propagate
     *
     * @param {KeyboardEvent} $event
     * @memberof DotTextareaContentComponent
     */
    onKeyEnter($event: KeyboardEvent): void {
        /*
            This field is use in dot-dialog.component when we hit enter it triggers the "Accept"
            action in the dialog, but for this case we don't want to do that because this fields
            are multilines, which mean that "enter" should do a "next line".
        */
        $event.stopPropagation();
    }
    /**
     * Initializes the Monaco Editor
     *
     * @param {unknown} editor
     * @memberof DotTextareaContentComponent
     */
    onInit(editor: unknown): void {
        this.monacoInit.emit({ name: this.editorName, editor });
    }

    /**
     * Update the value and form control
     * @param {string} value
     * @memberof DotTextareaContentComponent
     */
    onModelChange(value: string) {
        this.value = value;
        this.propagateChange(value ? value.replace(/\r/g, '').split('\n').join('\r\n') : value);
        if (this.valueChange.observers.length) {
            this.valueChange.emit(value);
        }
    }

    /**
     * Update model with external value
     *
     * @param string value
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
     * @param any fn
     * @memberof DotTextareaContentComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }

    private getSelectOptions() {
        return this.show
            ? this.show
                  .map((item) => {
                      return this.DEFAULT_OPTIONS.find((option) => option.value === item);
                  })
                  .filter((item) => item) // Remove undefined values in the array
            : this.DEFAULT_OPTIONS;
    }
}
