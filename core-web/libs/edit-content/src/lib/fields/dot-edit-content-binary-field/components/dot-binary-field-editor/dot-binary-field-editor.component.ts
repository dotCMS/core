import {
    MonacoEditorComponent,
    MonacoEditorConstructionOptions,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';
import { from } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
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

import { DotBinaryFieldValidatorService } from '../../service/dot-binary-field-validator/dot-binary-field-validator.service';

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
export class DotBinaryFieldEditorComponent implements OnInit {
    @Input() fileName = '';
    @Input() fileContent = '';

    @Output() readonly tempFileUploaded = new EventEmitter<DotCMSTempFile>();
    @Output() readonly cancel = new EventEmitter<void>();

    @ViewChild('editorRef', { static: true }) editorRef!: MonacoEditorComponent;

    private readonly cd: ChangeDetectorRef = inject(ChangeDetectorRef);
    private readonly dotUploadService: DotUploadService = inject(DotUploadService);
    private readonly dotMessageService: DotMessageService = inject(DotMessageService);
    private readonly dotBinaryFieldValidatorService: DotBinaryFieldValidatorService = inject(
        DotBinaryFieldValidatorService
    );

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
        this.setFormValues();
        this.name.valueChanges
            .pipe(debounceTime(350))
            .subscribe((name) => this.setEditorLanguage(name));
        this.invalidFileMessage = this.dotMessageService.get(
            'dot.binary.field.error.type.file.not.supported.message',
            this.dotBinaryFieldValidatorService.accept.join(', ')
        );
    }

    onEditorInit() {
        this.editor = this.editorRef.editor;
        if (this.fileName) {
            this.setEditorLanguage(this.fileName);
        }
    }

    onSubmit(): void {
        if (this.name.invalid) {
            this.markControlInvalid(this.name);

            return;
        }

        const file = new File([this.content.value], this.name.value, {
            type: this.mimeType
        });
        this.uploadFile(file);
    }

    private setFormValues(): void {
        this.name.setValue(this.fileName);
        this.content.setValue(this.fileContent);
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
            this.tempFileUploaded.emit({
                ...tempFile,
                content: this.content.value
            });
        });
    }

    private setEditorLanguage(fileName: string = '') {
        const fileExtension = fileName?.includes('.') ? fileName.split('.').pop() : '';
        const { id, mimetypes, extensions } = this.getLanguage(fileExtension) || {};
        this.mimeType = mimetypes?.[0];
        this.extension = extensions?.[0];

        const isValidType = this.dotBinaryFieldValidatorService.isValidType({
            extension: this.extension,
            mimeType: this.mimeType
        });

        if (fileExtension && !isValidType) {
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
}
