import { Observable, of, Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { switchMap, take, takeUntil } from 'rxjs/operators';

import { DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

export interface BlockEditorInput {
    content: { [key: string]: string };
    fieldName: string;
    field: DotCMSContentTypeField;
    inode: string;
    contentletIdentifier: string;
    languageId: number;
}

@Component({
    selector: 'dot-block-editor-sidebar',
    templateUrl: './dot-block-editor-sidebar.component.html',
    styleUrls: ['./dot-block-editor-sidebar.component.scss']
})
export class DotBlockEditorSidebarComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    blockEditorInput: BlockEditorInput;
    showVideoThumbnail: boolean;
    saving = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotEventsService: DotEventsService,
        private dotMessageService: DotMessageService,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotContentTypeService: DotContentTypeService
    ) {}

    ngOnInit(): void {
        const content$ = this.dotEventsService.listen<HTMLDivElement>('edit-block-editor').pipe(
            takeUntil(this.destroy$),
            switchMap((event) => this.extractBlockEditorData(event.data.dataset))
        );

        content$.subscribe((data) => {
            this.blockEditorInput = data;
        });
    }

    /**
     *  Execute the workflow to save the editor changes and then close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    saveEditorChanges(): void {
        this.saving = true;
        this.dotWorkflowActionsFireService
            .saveContentlet({
                [this.blockEditorInput.fieldName]: JSON.stringify(
                    this.blockEditor.editor.getJSON()
                ),
                inode: this.blockEditorInput.inode,
                indexPolicy: 'WAIT_FOR'
            })
            .pipe(take(1))
            .subscribe(
                () => {
                    this.saving = false;
                    const customEvent = new CustomEvent('ng-event', {
                        detail: { name: 'in-iframe' }
                    });
                    window.top.document.dispatchEvent(customEvent);
                    this.closeSidebar();
                },
                (e: HttpErrorResponse) => {
                    this.saving = false;
                    this.dotAlertConfirmService.alert({
                        accept: () => {
                            this.closeSidebar();
                        },
                        header: this.dotMessageService.get('error'),
                        message:
                            e.error?.message || this.dotMessageService.get('editpage.inline.error')
                    });
                }
            );
    }

    /**
     *  Clear the date to close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    closeSidebar(): void {
        this.blockEditorInput = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private extractBlockEditorData({
        contentletIdentifier,
        blockEditorContent,
        inode,
        language,
        fieldName,
        contentType
    }: DOMStringMap): Observable<BlockEditorInput> {
        return this.dotContentTypeService.getContentType(contentType).pipe(
            switchMap(({ fields }) => fields?.filter(({ variable }) => variable == fieldName)),
            switchMap((field: DotCMSContentTypeField) => {
                return of({
                    field,
                    inode,
                    contentletIdentifier,
                    fieldName: fieldName,
                    languageId: parseInt(language),
                    content: JSON.parse(blockEditorContent)
                });
            })
        );
    }
}
