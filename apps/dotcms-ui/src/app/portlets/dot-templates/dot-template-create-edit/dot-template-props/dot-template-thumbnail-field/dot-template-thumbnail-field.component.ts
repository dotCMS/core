import { HttpErrorResponse } from '@angular/common/http';
import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { throwError } from 'rxjs';
import { finalize, switchMap, take } from 'rxjs/operators';

import { DotCrudService } from '@services/dot-crud';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    DotCMSTempFile,
    DotTempFileUploadService
} from '@services/dot-temp-file-upload/dot-temp-file-upload.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

export interface DotCMSTemplateThumbnail extends DotCMSContentlet {
    assetVersion: string;
    name: string;
}

@Component({
    selector: 'dot-template-thumbnail-field',
    templateUrl: './dot-template-thumbnail-field.component.html',
    styleUrls: ['./dot-template-thumbnail-field.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotTemplateThumbnailFieldComponent)
        }
    ]
})
export class DotTemplateThumbnailFieldComponent implements ControlValueAccessor {
    asset: DotCMSTemplateThumbnail;
    error = '';
    loading = false;

    constructor(
        private dotTempFileUploadService: DotTempFileUploadService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotCrudService: DotCrudService,
        private dotMessageService: DotMessageService
    ) {}

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

    propagateChange = (_: any) => {};

    writeValue(id: string): void {
        this.loading = true;

        this.dotCrudService
            .getDataById('/api/content', id, 'contentlets')
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

    registerOnTouched(): void {}
}
