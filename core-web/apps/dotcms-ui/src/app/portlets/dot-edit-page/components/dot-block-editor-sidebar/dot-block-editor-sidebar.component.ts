import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { switchMap, take, takeUntil } from 'rxjs/operators';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { Observable, of, Subject } from 'rxjs';
import { DotBlockEditorComponent } from '@dotcms/block-editor';
import { HttpErrorResponse } from '@angular/common/http';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotContentTypeService } from '@services/dot-content-type';
import { DotCMSContentTypeField, DotCMSContentTypeFieldVariable } from '@dotcms/dotcms-models';

export interface BlockEditorInput {
    content: { [key: string]: string };
    fieldName: string;
    language: number;
    inode: string;
    fieldVariables?: {
        allowedBlocks?: string;
        allowedContentTypes?: string;
        styles: string;
    };
}

@Component({
    selector: 'dot-block-editor-sidebar',
    templateUrl: './dot-block-editor-sidebar.component.html',
    styleUrls: ['./dot-block-editor-sidebar.component.scss']
})
export class DotBlockEditorSidebarComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    data: BlockEditorInput;
    saving = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotEventsService: DotEventsService,
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotContentTypeService: DotContentTypeService
    ) {}

    ngOnInit(): void {
        this.dotEventsService
            .listen<HTMLDivElement>('edit-block-editor')
            .pipe(
                takeUntil(this.destroy$),
                switchMap((event) => this.extractBlockEditorData(event.data.dataset))
            )
            .subscribe((eventData: BlockEditorInput) => {
                this.data = eventData;
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
                [this.data.fieldName]: JSON.stringify(this.blockEditor.editor.getJSON()),
                inode: this.data.inode,
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
                    this.data = null;
                },
                (e: HttpErrorResponse) => {
                    const message =
                        e.error.errors[0].message ||
                        this.dotMessageService.get('editpage.inline.error');
                    this.dotGlobalMessageService.error(message);
                }
            );
    }

    /**
     *  Clear the date to close the sideber.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    closeSidebar(): void {
        this.data = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private extractBlockEditorData(dataSet: {
        [key: string]: string;
    }): Observable<BlockEditorInput> {
        return this.dotContentTypeService.getContentType(dataSet.contentType).pipe(
            switchMap((contentType) =>
                contentType?.fields.filter((field) => field.variable == dataSet.fieldName)
            ),
            switchMap((field: DotCMSContentTypeField) => {
                return of({
                    fieldVariables: this.parseFieldVariables(field.fieldVariables),
                    fieldName: dataSet.fieldName,
                    language: parseInt(dataSet.language),
                    inode: dataSet.inode,
                    content: JSON.parse(dataSet.blockEditorContent)
                });
            })
        );
    }

    private parseFieldVariables(
        fieldVariables: DotCMSContentTypeFieldVariable[]
    ): BlockEditorInput['fieldVariables'] {
        return {
            allowedBlocks: fieldVariables.find((variable) => variable.key === 'allowedBlocks')
                ?.value,
            allowedContentTypes: fieldVariables.find(
                (variable) => variable.key === 'allowedContentTypes'
            )?.value,
            styles: fieldVariables.find((variable) => variable.key === 'styles')?.value
        };
    }
}
