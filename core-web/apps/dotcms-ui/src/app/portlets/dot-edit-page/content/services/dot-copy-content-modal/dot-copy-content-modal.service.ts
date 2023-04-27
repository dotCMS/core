import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { map, take } from 'rxjs/operators';

import {
    DotBinaryOptionSelectorComponent,
    BINARY_OPTION
} from '@dotcms/app/portlets/shared/dot-binary-option-selector/dot-binary-option-selector.component';
import { DotMessageService } from '@dotcms/data-access';
import { DotLoadingIndicatorService } from '@dotcms/utils';

export interface ModelCopyContentResponse {
    shouldCopie?: boolean;
    closed?: boolean;
}

@Injectable()
export class DotCopyContentModalService {
    private readonly CONTENT_EDIT_OPTIONS: BINARY_OPTION = {
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

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService,
        public dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {}

    /**
     *
     * @private
     * @param {DotCopyContent} copyContent
     * @memberof DotEditContentComponent
     */
    open(): Observable<ModelCopyContentResponse> {
        const ref = this.dialogService.open(DotBinaryOptionSelectorComponent, {
            header: this.dotMessageService.get('Edit-Content'),
            width: '37rem',
            data: { options: this.CONTENT_EDIT_OPTIONS },
            contentStyle: { padding: '0px' }
        });

        return ref.onClose.pipe(
            take(1),
            map((value) => {
                if (!value) {
                    return { closed: true };
                }

                return { shouldCopie: this.CONTENT_EDIT_OPTIONS.option1.value === value };
            })
        );
    }
}
