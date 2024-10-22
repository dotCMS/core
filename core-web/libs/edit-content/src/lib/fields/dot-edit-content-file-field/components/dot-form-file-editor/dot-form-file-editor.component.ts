import {
    MonacoEditorComponent,
    MonacoEditorConstructionOptions,
    MonacoEditorModule
} from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    output,
    viewChild, OnInit
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';

import { FormFileEditorStore } from './store/form-file-editor.store';

import { UploadedFile } from '../../models';

type DialogProps = {
    allowFileNameEdit: boolean;
    userMonacoOptions: Partial<MonacoEditorConstructionOptions>;
    uploadedFile: UploadedFile | null;
}

@Component({
    selector: 'dot-form-file-editor',
    standalone: true,
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule,
        MonacoEditorModule
    ],
    templateUrl: './dot-form-file-editor.component.html',
    styleUrls: ['./dot-form-file-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [FormFileEditorStore]
})
export class DotFormFileEditorComponent implements OnInit {
    readonly store = inject(FormFileEditorStore);
    readonly #formBuilder = inject(FormBuilder);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig<DialogProps>);

    readonly form = this.#formBuilder.nonNullable.group({
        name: ['', [Validators.required, Validators.pattern(/^[^.]+\.[^.]+$/)]],
        content: ['']
    });


    tempFileUploaded = output<UploadedFile>();
    cancel = output<void>();

    $editorRef = viewChild.required(MonacoEditorComponent);

    constructor() {

        effect(() => {
            const isUploading = this.store.isUploading();

            if (isUploading) {
                this.#disableEditor();
            } else {
                this.#enableEditor();
            }
        });
    }

    ngOnInit(): void {
        const data = this.#dialogConfig?.data as DialogProps;
        if (!data) {
            return;
        }

        const { uploadedFile, userMonacoOptions, allowFileNameEdit } = data;

        if (uploadedFile) {
            this.#initValuesForm(uploadedFile);
        }

        this.store.initLoad({
            monacoOptions: userMonacoOptions || {},
            allowFileNameEdit: allowFileNameEdit || true
        });
    }

    onSubmit(): void {
        if (this.form.invalid) {
            this.form.markAsDirty();
            this.form.updateValueAndValidity();

            return;
        }

        this.store.uploadFile();
    }

    get nameField() {
        return this.form.get('name');
    }

    get contentField() {
        return this.form.get('content');
    }

    #disableEditor() {
        this.form.disable();
        const editor = this.$editorRef().editor;
        editor.updateOptions({ readOnly: true });
    }

    #enableEditor() {
        this.form.enable();
        const editor = this.$editorRef().editor;
        editor.updateOptions({ readOnly: false });
    }

    #initValuesForm({ source, file }: UploadedFile): void {
        this.form.patchValue({
            name: source === 'temp' ? file.fileName : file.title,
            content: file.content
        });
    }
}
