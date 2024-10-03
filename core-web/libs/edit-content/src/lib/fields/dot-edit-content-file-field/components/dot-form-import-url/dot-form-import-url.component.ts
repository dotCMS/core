import { ChangeDetectionStrategy, Component, effect, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef, DialogService } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';

import { FormImportUrlStore } from './store/form-import-url.store';

@Component({
    selector: 'dot-form-import-url',
    standalone: true,
    imports: [
        DotMessagePipe,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        ButtonModule,
        InputTextModule
    ],
    templateUrl: './dot-form-import-url.component.html',
    styleUrls: ['./dot-form-import-url.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [FormImportUrlStore]
})
export class DotFormImportUrlComponent implements OnInit {
    readonly #formBuilder = inject(FormBuilder);
    readonly store = inject(FormImportUrlStore);
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogService = inject(DialogService);
    readonly #instance = this.#dialogService.getInstance(this.#dialogRef);

    readonly form = this.#formBuilder.group({
        url: ['', [Validators.required, Validators.pattern(/^(ftp|http|https):\/\/[^ "]+$/)]]
    });

    constructor() {
        effect(
            () => {
                const file = this.store.file();
                const isDone = this.store.isDone();

                if (file && isDone) {
                    this.#dialogRef.close(file);
                }
            },
            {
                allowSignalWrites: true
            }
        );
    }

    ngOnInit(): void {
        const uploadType = this.#instance?.data?.inputType === 'Binary' ? 'temp' : 'dotasset';
        this.store.setUploadType(uploadType);
    }

    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        const { url } = this.form.getRawValue();
        this.store.uploadFileByUrl(url);
    }

    cancelUpload(): void {
        this.#dialogRef.close();
    }
}
