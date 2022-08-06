import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DotcmsBlockEditorComponent } from '@dotcms/block-editor';
import { take, takeUntil } from 'rxjs/operators';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { Subject } from 'rxjs';

export interface BlockEditorData {
    content: { [key: string]: string };
    allowedContentTypes: string;
    allowedBlocks: string;
    fieldName: string;
    language: number;
    inode: string;
}

@Component({
    selector: 'dot-edit-block-editor',
    templateUrl: './dot-edit-block-editor.component.html',
    styleUrls: ['./dot-edit-block-editor.component.scss']
})
export class DotEditBlockEditorComponent implements OnInit, OnDestroy {
    @ViewChild('blockEditor') blockEditor: DotcmsBlockEditorComponent;

    data: BlockEditorData;
    saving = false;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private dotEventsService: DotEventsService
    ) {}

    ngOnInit(): void {
        this.dotEventsService
            .listen<HTMLDivElement>('edit-block-editor')
            .pipe(takeUntil(this.destroy$))
            .subscribe((event) => {
                this.data = {
                    fieldName: event.data.dataset.fieldName,
                    language: parseInt(event.data.dataset.language),
                    allowedBlocks: event.data.dataset.allowedBlocks,
                    allowedContentTypes: event.data.dataset.allowedContentTypes,
                    inode: event.data.dataset.inode,
                    content: JSON.parse(event.data.dataset.content)
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
            .subscribe(() => {
                this.saving = false;
                const customEvent = new CustomEvent('ng-event', { detail: { name: 'in-iframe' } });
                window.top.document.dispatchEvent(customEvent);
                this.data = null;
            });
    }

    closeSidebar(): void {
        this.data = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
