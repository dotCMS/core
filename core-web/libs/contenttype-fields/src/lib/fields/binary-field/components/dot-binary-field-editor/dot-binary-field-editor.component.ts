import {
    MonacoEditorComponent,
    MonacoEditorConstructionOptions,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';
import { from } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import {
    FormControl,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { debounceTime } from 'rxjs/operators';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotFieldValidationMessageComponent, DotMessagePipe } from '@dotcms/ui';

const EDITOR_CONFIG: MonacoEditorConstructionOptions = {
    theme: 'vs',
    minimap: {
        enabled: false
    },
    cursorBlinking: 'solid',
    overviewRulerBorder: false,
    mouseWheelZoom: false,
    lineNumbers: 'on',
    roundedSelection: false,
    automaticLayout: true,
    language: 'text'
};

@Component({
    selector: 'dot-dot-binary-field-editor',
    standalone: true,
    imports: [
        CommonModule,
        MonacoEditorModule,
        FormsModule,
        ReactiveFormsModule,
        InputTextModule,
        ButtonModule,
        DotMessagePipe,
        DotFieldValidationMessageComponent
    ],
    templateUrl: './dot-binary-field-editor.component.html',
    styleUrls: ['./dot-binary-field-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldEditorComponent implements OnInit, AfterViewInit {
    @Input() accept: string[];

    @Output() readonly tempFileUploaded = new EventEmitter<DotCMSTempFile>();
    @Output() readonly cancel = new EventEmitter<void>();

    @ViewChild('editorRef', { static: true }) editorRef!: MonacoEditorComponent;

    private readonly cd: ChangeDetectorRef = inject(ChangeDetectorRef);
    private readonly dotUploadService: DotUploadService = inject(DotUploadService);
    private readonly dotMessageService: DotMessageService = inject(DotMessageService);

    private extension = '';
    private invalidFileMessage = '';
    private editor: monaco.editor.IStandaloneCodeEditor;
    readonly form = new FormGroup({
        name: new FormControl('', [Validators.required, Validators.pattern(/^[^.]+\.[^.]+$/)]),
        content: new FormControl('')
    });

    editorOptions = EDITOR_CONFIG;
    mimeType = '';

    get name(): FormControl {
        return this.form.get('name') as FormControl;
    }

    get content(): FormControl {
        return this.form.get('content') as FormControl;
    }

    ngOnInit(): void {
        this.name.valueChanges
            .pipe(debounceTime(350))
            .subscribe((name) => this.setEditorLanguage(name));
        this.invalidFileMessage = this.dotMessageService.get(
            'dot.binary.field.error.type.file.not.supported.message',
            this.accept.join(', ')
        );
    }

    ngAfterViewInit(): void {
        this.editor = this.editorRef.editor;
    }

    onSubmit(): void {
        if (this.form.invalid) {
            if (!this.name.dirty) {
                this.markControlInvalid(this.name);
            }

            return;
        }

        const file = new File([this.content.value], this.name.value, {
            type: this.mimeType
        });
        this.uploadFile(file);
    }

    private markControlInvalid(control: FormControl): void {
        control.markAsDirty();
        control.updateValueAndValidity();
        this.cd.detectChanges();
    }

    private uploadFile(file: File) {
        const obs$ = from(this.dotUploadService.uploadFile({ file }));
        this.disableEditor();
        obs$.subscribe((tempFile) => {
            this.enableEditor();
            this.tempFileUploaded.emit(tempFile);
        });
    }

    private setEditorLanguage(fileName: string = '') {
        const fileExtension = fileName?.includes('.') ? fileName.split('.').pop() : '';
        const { id, mimetypes, extensions } = this.getLanguage(fileExtension) || {};
        this.mimeType = mimetypes?.[0];
        this.extension = extensions?.[0];

        if (fileExtension && !this.isValidType()) {
            this.name.setErrors({ invalidExtension: this.invalidFileMessage });
        }

        this.updateEditorLanguage(id);
        this.cd.detectChanges();
    }

    private getLanguage(fileExtension: string) {
        // Global Object Defined by Monaco Editor
        return monaco.languages
            .getLanguages()
            .find((language) => language.extensions?.includes(`.${fileExtension}`));
    }

    private updateEditorLanguage(languageId: string = 'text') {
        this.editorOptions = {
            ...this.editorOptions,
            language: languageId
        };
    }

    private disableEditor() {
        this.form.disable();
        this.editor.updateOptions({
            readOnly: true
        });
    }

    private enableEditor() {
        this.form.enable();
        this.editor.updateOptions({
            readOnly: false
        });
    }

    private isValidType(): boolean {
        if (this.accept?.length === 0) {
            return true;
        }

        return this.accept?.includes(this.extension) || this.accept?.includes(this.mimeType);
    }
}
