import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { take, takeUntil } from 'rxjs/operators';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { Subject } from 'rxjs';
import { DotBlockEditorComponent } from '@dotcms/block-editor';
import { HttpErrorResponse } from '@angular/common/http';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';

export interface BlockEditorData {
    content: { [key: string]: string };
    fieldName: string;
    language: number;
    inode: string;
    fieldVariables?: { [key: string]: string };
}

@Component({
    selector: 'dot-block-editor-sidebar',
    templateUrl: './dot-block-editor-sidebar.component.html',
    styleUrls: ['./dot-block-editor-sidebar.component.scss']
})
export class DotBlockEditorSidebarComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    data: BlockEditorData;
    saving = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotEventsService: DotEventsService,
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService
    ) {}

    ngOnInit(): void {
        this.dotEventsService
            .listen<HTMLDivElement>('edit-block-editor')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event) => {
                this.data = {
                    fieldName: event.data.dataset.fieldName,
                    language: parseInt(event.data.dataset.language),
                    inode: event.data.dataset.inode,
                    content: JSON.parse(event.data.dataset.content),
                    fieldVariables: this.parseFieldVariables(event.data.dataset.fieldVariables)
                };
            });
    }

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

    closeSidebar(): void {
        this.data = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private parseFieldVariables(fieldVariables: string): { [key: string]: string } {
        const { allowedBlocks, allowedContentTypes } = JSON.parse(fieldVariables);

        return {
            allowedBlocks: allowedBlocks?.value,
            allowedContentTypes: allowedContentTypes?.value
        };
    }
}
