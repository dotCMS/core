import { throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectorRef,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    forwardRef,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { finalize, switchMap, take } from 'rxjs/operators';

import {
    DotCrudService,
    DotMessageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface DotCMSTemplateThumbnail extends DotCMSContentlet {
    assetVersion: string;
    name: string;
}

@Component({
    selector: 'dot-template-thumbnail-field',
    templateUrl: './dot-template-thumbnail-field.component.html',
    styleUrls: ['./dot-template-thumbnail-field.component.scss'],
    imports: [DotMessagePipe],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTemplateThumbnailFieldComponent)
        }
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotTemplateThumbnailFieldComponent implements ControlValueAccessor {
    private readonly cdr = inject(ChangeDetectorRef);
    private dotTempFileUploadService = inject(DotTempFileUploadService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private dotCrudService = inject(DotCrudService);
    private dotMessageService = inject(DotMessageService);

    /** Null with no thumbnail set, which is what `writeValue('')` and clearing both do. */
    asset: DotCMSTemplateThumbnail | null = null;
    error = '';
    loading = false;

    /**
     * Handle thumbnail setup
     *
     * @param {(CustomEvent<{ name: string; value: File | string }>)} { detail: { value } }
     * @memberof DotTemplateThumbnailFieldComponent
     */
    onThumbnailChange({
        detail: { value }
    }: CustomEvent<{ name: string; value: File | string }>): void {
        if (value) {
            this.loading = true;
            this.error = '';

            this.dotTempFileUploadService
                .upload(value)
                .pipe(
                    switchMap((uploaded: DotCMSTempFile[] | string) => {
                        // A failed upload arrives as the HTTP status *string* —
                        // `DotTempFileUploadService.handleError` maps the error to
                        // `err.status.toString()` — so destructuring it as a temp-file list took
                        // the string's first character and carried on with nothing.
                        if (typeof uploaded === 'string' || !uploaded.length) {
                            return throwError(() =>
                                this.dotMessageService.get(
                                    'templates.properties.form.thumbnail.error'
                                )
                            );
                        }

                        const [{ id, image }] = uploaded;

                        if (!image) {
                            return throwError(() =>
                                this.dotMessageService.get(
                                    'templates.properties.form.thumbnail.error.invalid.url'
                                )
                            );
                        }

                        return this.dotWorkflowActionsFireService.publishContentletAndWaitForIndex<DotCMSTemplateThumbnail>(
                            'dotAsset',
                            {
                                asset: id
                            }
                        );
                    }),
                    take(1),
                    finalize(() => {
                        this.loading = false;
                        // DynamicDialog (appendTo="body"): force CD after async upload.
                        this.cdr.detectChanges();
                    })
                )
                .subscribe({
                    next: (asset: DotCMSTemplateThumbnail) => {
                        this.asset = asset;
                        this.propagateChange(this.asset.identifier);
                    },
                    error: (err: HttpErrorResponse | string) => {
                        const defaultError = this.dotMessageService.get(
                            'templates.properties.form.thumbnail.error'
                        );
                        this.error = typeof err === 'string' ? err : defaultError;
                    }
                });
        } else if (this.asset) {
            this.asset = null;
            this.propagateChange('');
            this.cdr.detectChanges();
        } else {
            this.error = this.dotMessageService.get(
                'templates.properties.form.thumbnail.error.invalid.image'
            );
            this.cdr.detectChanges();
        }
    }

    propagateChange = (_: unknown) => {
        // do nothing
    };

    writeValue(id: string | null): void {
        if (!id) {
            this.asset = null;
            this.loading = false;

            return;
        }

        this.loading = true;

        this.dotCrudService
            .getDataById<DotCMSTemplateThumbnail[]>('/api/content', id, 'contentlets')
            .pipe(
                finalize(() => {
                    this.loading = false;
                    // DynamicDialog (appendTo="body"): async asset load must force CD or
                    // preview bindings stay empty after Angular 21+/22.
                    this.cdr.detectChanges();
                }),
                take(1)
            )
            .subscribe({
                next: ([contentlet]: DotCMSTemplateThumbnail[]) => {
                    this.asset = contentlet;
                },
                error: () => {
                    // do nothing, failing silently like any html input select that get pass an invalid value
                }
            });
    }

    registerOnChange(fn: (value: unknown) => void): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        //
    }
}
