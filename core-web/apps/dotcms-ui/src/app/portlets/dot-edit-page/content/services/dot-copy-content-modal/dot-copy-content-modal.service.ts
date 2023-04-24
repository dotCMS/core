import { Observable, of } from 'rxjs';

import { Injectable } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

// import { map, pluck, switchMap, take } from 'rxjs/operators';
import { switchMap, take } from 'rxjs/operators';

import {
    DotBinaryOptionSelectorComponent,
    BINARY_OPTION
} from '@dotcms/app/portlets/shared/dot-binary-option-selector/dot-binary-option-selector.component';
import { DotCopyContentService, DotMessageService } from '@dotcms/data-access';
import { DotCopyContent } from '@dotcms/dotcms-models';

@Injectable()
export class DotCopyContentModalService {
    private readonly CONTENT_EDIT_OPTIONS: BINARY_OPTION = {
        option1: {
            value: 'current',
            message: 'editpage.content.edit.content.in.this.page.message',
            icon: 'article',
            label: 'editpage.content.edit.content.in.this.page',
            buttonLabel: 'editpage.content.edit.content.in.this.page.button.label'
        },
        option2: {
            value: 'all',
            message: 'editpage.content.edit.content.in.all.pages.message',
            icon: 'dynamic_feed',
            label: 'editpage.content.edit.content.in.all.pages',
            buttonLabel: 'editpage.content.edit.content.in.all.pages.button.label'
        }
    };

    constructor(
        private dialogService: DialogService,
        private dotCopyContentService: DotCopyContentService,
        private dotMessageService: DotMessageService
    ) {}

    /**
     *
     *
     * @private
     * @param {DotCopyContent} copyContent
     * @memberof DotEditContentComponent
     */
    openCopyContentModal({
        copyContent,
        inode
    }: {
        copyContent: DotCopyContent;
        inode: string;
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
    }): Observable<any> {
        const ref = this.dialogService.open(DotBinaryOptionSelectorComponent, {
            header: this.dotMessageService.get('Edit-Content'),
            width: '37rem',
            data: { options: this.CONTENT_EDIT_OPTIONS },
            contentStyle: { padding: '0px' }
        });

        return ref.onClose.pipe(
            take(1),
            switchMap((value) => {
                if (!value) {
                    return of({});
                }

                return this.CONTENT_EDIT_OPTIONS.option1.value === value
                    ? this.dotCopyContentService.copyContentInPage(copyContent)
                    : of({ inode });
            })
        );
    }
}
