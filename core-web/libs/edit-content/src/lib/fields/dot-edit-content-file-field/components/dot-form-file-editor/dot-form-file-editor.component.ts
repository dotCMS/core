import { MonacoEditorComponent, MonacoEditorConstructionOptions, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';

import { ChangeDetectionStrategy, Component, effect, inject, input, output, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';

import { FormFileEditorStore } from './store/form-file-editor.store';

import { INPUT_TYPE } from '../../../dot-edit-content-text-field/utils';
import { UploadedFile } from '../../models';

@Component({
    selector: 'dot-form-file-editor',
    standalone: true,
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule,
        MonacoEditorModule,
    ],
    templateUrl: './dot-form-file-editor.component.html',
    styleUrls: ['./dot-form-file-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [FormFileEditorStore]
})
export class DotFormFileEditorComponent{
    readonly store = inject(FormFileEditorStore);
    readonly #formBuilder = inject(FormBuilder);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(
        DynamicDialogConfig<{ inputType: INPUT_TYPE; acceptedFiles: string[] }>
    );

    readonly form = this.#formBuilder.nonNullable.group({
        name: ['', [Validators.required, Validators.pattern(/^[^.]+\.[^.]+$/)]],
        content: ['']
    });
    
    $fileName = input.required<string>({ alias: 'fileName' });
    $fileContent = input.required<string>({ alias: 'fileContent' });
    $allowFileNameEdit = input(false, { alias: 'allowFileNameEdit' });
    $userMonacoOptions = input<MonacoEditorConstructionOptions>({});

    tempFileUploaded = output<UploadedFile>();
    cancel = output<void>();

    $editorRef = viewChild.required(MonacoEditorComponent);

    constructor() {

        effect(() => {
            const name = this.$fileName();
            const content = this.$fileContent();

            this.form.patchValue({
                name,
                content
            });
        });

        effect(() => {
            this.store.setMonacoOptions(this.$userMonacoOptions());
        });

        effect(() => {
            const isUploading = this.store.isUploading();

            if (isUploading) {
                this.#disableEditor();
            } else {
                this.#enableEditor();
            }
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
        editor.updateOptions({readOnly: true});
    }

    #enableEditor() {
        this.form.enable();
        const editor = this.$editorRef().editor;
        editor.updateOptions({readOnly: true});
    }
}
