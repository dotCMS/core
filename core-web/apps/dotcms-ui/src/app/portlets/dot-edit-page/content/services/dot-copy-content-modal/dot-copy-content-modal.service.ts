import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map, take, filter } from 'rxjs/operators';

import {
    DotBinaryOptionSelectorComponent,
    BINARY_OPTION
} from '@dotcms/app/portlets/shared/dot-binary-option-selector/dot-binary-option-selector.component';
import { DotMessageService } from '@dotcms/data-access';

export interface ModelCopyContentResponse {
    shouldCopy: boolean;
}

export const OPTIONS = {
    option1: {
        value: 'Copy',
        message: 'editpage.content.edit.content.in.this.page.message',
        icon: 'article',
        label: 'editpage.content.edit.content.in.this.page',
        buttonLabel: 'editpage.content.edit.content.in.this.page.button.label'
    },
    option2: {
        value: 'NotCopy',
        message: 'editpage.content.edit.content.in.all.pages.message',
        icon: 'dynamic_feed',
        label: 'editpage.content.edit.content.in.all.pages',
        buttonLabel: 'editpage.content.edit.content.in.all.pages.button.label'
    }
};

@Injectable()
export class DotCopyContentModalService {
    private readonly CONTENT_EDIT_OPTIONS: BINARY_OPTION = OPTIONS;
    private _dialogRef: DynamicDialogRef;

    get dialogRef() {
        return this._dialogRef;
    }

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {}

    /**
     *
     * @private
     * @param {DotCopyContent} copyContent
     * @memberof DotEditContentComponent
     */
    open(): Observable<ModelCopyContentResponse> {
        this._dialogRef = this.dialogService.open(DotBinaryOptionSelectorComponent, {
            header: this.dotMessageService.get('Edit-Content'),
            width: '37rem',
            data: { options: this.CONTENT_EDIT_OPTIONS },
            contentStyle: { padding: '0px' }
        });

        return this._dialogRef.onClose.pipe(
            take(1),
            // If the user close the modal without select an option, we return false
            // This will complete the observable
            filter((value) => !!value),
            map((value) => {
                return { shouldCopy: this.CONTENT_EDIT_OPTIONS.option1.value === value };
            })
        );
    }
}
