import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { map, take, filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';

import {
    BINARY_OPTION,
    DotBinaryOptionSelectorComponent
} from '../../components/dot-binary-option-selector/dot-binary-option-selector.component';

export interface ModelCopyContentResponse {
    shouldCopy: boolean;
}

@Injectable()
export class DotCopyContentModalService {
    private readonly CONTENT_EDIT_OPTIONS: BINARY_OPTION = {
        option1: {
            value: 'NotCopy',
            message: 'editpage.content.edit.content.in.all.pages.message',
            icon: 'dynamic_feed',
            label: 'editpage.content.edit.content.in.all.pages',
            buttonLabel: 'editpage.content.edit.content.in.all.pages.button.label'
        },
        option2: {
            value: 'Copy',
            message: 'editpage.content.edit.content.in.this.page.message',
            icon: 'article',
            label: 'editpage.content.edit.content.in.this.page',
            buttonLabel: 'editpage.content.edit.content.in.this.page.button.label'
        }
    };

    constructor(
        private dialogService: DialogService,
        private dotMessageService: DotMessageService
    ) {}

    /**
     * Open a modal with two options to copy or not the content
     *
     * @param {DotCopyContent} copyContent
     * @memberof DotEditContentComponent
     * @returns {Observable<ModelCopyContentResponse>}
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
            // If the user close the modal without select an option, we return false
            // This will complete the observable
            filter((value) => !!value),
            map((value) => {
                return { shouldCopy: this.CONTENT_EDIT_OPTIONS.option2.value === value };
            })
        );
    }
}
