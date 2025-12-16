import { throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, forwardRef, inject, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
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
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotTemplateThumbnailFieldComponent implements ControlValueAccessor {
    private dotTempFileUploadService = inject(DotTempFileUploadService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private dotCrudService = inject(DotCrudService);
    private dotMessageService = inject(DotMessageService);

    asset: DotCMSTemplateThumbnail;
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
                    switchMap(([{ id, image }]: DotCMSTempFile[]) => {
                        if (!image) {
                            return throwError(
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
                    })
                )
                .subscribe(
                    (asset: DotCMSTemplateThumbnail) => {
                        this.asset = asset;
                        this.propagateChange(this.asset.identifier);
                    },
                    (err: HttpErrorResponse | string) => {
                        const defaultError = this.dotMessageService.get(
                            'templates.properties.form.thumbnail.error'
                        );
                        this.error = typeof err === 'string' ? err : defaultError;
                    }
                );
        } else if (this.asset) {
            this.asset = null;
            this.propagateChange('');
        } else {
            this.error = this.dotMessageService.get(
                'templates.properties.form.thumbnail.error.invalid.image'
            );
        }
    }

    propagateChange = (_: unknown) => {
        // do nothing
    };

    writeValue(id: string): void {
        this.loading = true;

        this.dotCrudService
            .getDataById<DotCMSTemplateThumbnail[]>('/api/content', id, 'contentlets')
            .pipe(
                finalize(() => {
                    this.loading = false;
                }),
                take(1)
            )
            .subscribe(
                ([contentlet]: DotCMSTemplateThumbnail[]) => {
                    this.asset = contentlet;
                },
                () => {
                    // do nothing, failing silently like any html input select that get pass an invalid value
                }
            );
    }

    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        //
    }
}
