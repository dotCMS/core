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
    computed,
    EventEmitter,
    HostListener,
    inject,
    Input,
    OnChanges,
    OnInit,
    Output,
    signal,
    ViewChild
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

import { dotVelocityLanguageDefinition } from '../../../../custom-languages/velocity-monaco-language';
import { DEFAULT_BINARY_FIELD_MONACO_CONFIG } from '../../dot-edit-content-binary-field.component';
import { DotBinaryFieldValidatorService } from '../../service/dot-binary-field-validator/dot-binary-field-validator.service';

const DEFAULT_FILE_TYPE = 'text';
@Component({
    selector: 'dot-binary-field-editor',
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
export class DotBinaryFieldEditorComponent implements OnInit, OnChanges {
    @Input() fileName = '';
    @Input() fileContent = '';
    @Input() allowFileNameEdit = true;

    @Output() readonly tempFileUploaded = new EventEmitter<DotCMSTempFile>();
    @Output() readonly cancel = new EventEmitter<void>();
    @ViewChild('editorRef', { static: true }) editorRef!: MonacoEditorComponent;
    readonly form = new FormGroup({
        name: new FormControl('', [Validators.required]),
        content: new FormControl('')
    });
    mimeType = '';
    private readonly languageType = signal(DEFAULT_FILE_TYPE);
    private readonly cd: ChangeDetectorRef = inject(ChangeDetectorRef);
    private readonly dotUploadService: DotUploadService = inject(DotUploadService);
    private readonly dotMessageService: DotMessageService = inject(DotMessageService);
    private readonly dotBinaryFieldValidatorService: DotBinaryFieldValidatorService = inject(
        DotBinaryFieldValidatorService
    );
    private extension = '';
    private invalidFileMessage = '';
    private editor: monaco.editor.IStandaloneCodeEditor;

    private _userMonacoOptions = signal<MonacoEditorConstructionOptions>({});

    monacoOptions = computed(() => {
        return {
            ...DEFAULT_BINARY_FIELD_MONACO_CONFIG,
            ...this._userMonacoOptions(),
            language: this.languageType()
        };
    });

    @Input()
    set userMonacoOptions(customMonacoOptions: MonacoEditorConstructionOptions) {
        this._userMonacoOptions.set(customMonacoOptions);
    }

    get name(): FormControl {
        return this.form.get('name') as FormControl;
    }

    get content(): FormControl {
        return this.form.get('content') as FormControl;
    }

    /**
     * Close the editor when the user press ESC
     * And prevent the default behavior of the edit conten iframe
     *
     * @param {*} event
     * @memberof DotBinaryFieldEditorComponent
     */
    @HostListener('document:keydown.escape', ['$event']) onEscape(event) {
        // TODO: The 'emit' function requires a mandatory void argument
        this.cancel.emit();
        event.preventDefault();
        event.stopPropagation();
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

    ngOnChanges(): void {
        this.setFormValues();
        if (window.monaco) {
            this.setEditorLanguage(this.fileName);
        }
    }

    onEditorInit() {
        this.editor = this.editorRef.editor;
        if (this.fileName) {
            this.setEditorLanguage(this.fileName);
        }

        window.monaco.languages.register({
            id: 'velocity',
            extensions: ['.vtl'],
            mimetypes: ['text/x-velocity']
        });

        window.monaco.languages.setMonarchTokensProvider('velocity', dotVelocityLanguageDefinition);
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

    private setEditorLanguage(fileName = '') {
        const fileExtension = this.extractFileExtension(fileName);

        if (fileExtension === 'vtl') {
            this.setVelocityLanguage();
        } else {
            this.updateLanguageForFileExtension(fileExtension);
        }

        this.validateFileType();
        this.cd.detectChanges();
    }

    private extractFileExtension(fileName: string) {
        return fileName?.includes('.') ? fileName.split('.').pop() : '';
    }

    private setVelocityLanguage() {
        this.mimeType = 'text/x-velocity';
        this.extension = 'vtl';
        this.updateEditorLanguage('velocity');
    }

    private updateLanguageForFileExtension(fileExtension: string) {
        const { id, mimetypes, extensions } = this.getLanguage(fileExtension) || {};
        this.mimeType = mimetypes?.[0] || 'plain/text';
        this.extension = extensions?.[0] || 'txt';
        this.updateEditorLanguage(id);
    }

    private validateFileType() {
        const isValidType = this.dotBinaryFieldValidatorService.isValidType({
            extension: this.extension,
            mimeType: this.mimeType
        });
        if (!isValidType) {
            this.name.setErrors({ invalidExtension: this.invalidFileMessage });
        }
    }

    private getLanguage(fileExtension: string) {
        // Global Object Defined by Monaco Editor
        return monaco.languages
            .getLanguages()
            .find((language) => language.extensions?.includes(`.${fileExtension}`));
    }

    private updateEditorLanguage(languageId = 'text') {
        this.languageType.set(languageId);
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
