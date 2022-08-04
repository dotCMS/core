import { Component, Injector, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DEFAULT_LANG_ID,
    DotBubbleMenuExtension,
    DragHandler,
    ImageBlock,
    ImageUpload
} from '@dotcms/block-editor';
import { Underline } from '@tiptap/extension-underline';
import { TextAlign } from '@tiptap/extension-text-align';
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { take, takeUntil } from 'rxjs/operators';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { Subject } from 'rxjs';

export interface BlockEditorData {
    content: { [key: string]: string };
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
    editor: Editor;
    data: BlockEditorData;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private injector: Injector,
        public viewContainerRef: ViewContainerRef,
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
                    inode: event.data.dataset.inode,
                    content: JSON.parse(event.data.dataset.content)
                };
                this.initBlockEditor();
            });
    }

    saveEditorChanges(): void {
        this.dotWorkflowActionsFireService
            .saveContentlet({
                [this.data.fieldName]: JSON.stringify(this.editor.getJSON()),
                inode: this.data.inode,
                indexPolicy: 'WAIT_FOR'
            })
            .pipe(take(1))
            .subscribe(() => {
                const customEvent = new CustomEvent('ng-event', { detail: { name: 'in-iframe' } });
                window.top.document.dispatchEvent(customEvent);
                this.data = null;
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private initBlockEditor() {
        this.editor = new Editor({
            extensions: [
                StarterKit,
                ContentletBlock(this.injector),
                ImageBlock(this.injector),
                ActionsMenu(this.viewContainerRef),
                DragHandler(this.viewContainerRef),
                ImageUpload(this.injector, this.viewContainerRef),
                BubbleLinkFormExtension(this.injector, this.viewContainerRef),
                DotBubbleMenuExtension(this.viewContainerRef),
                // Marks Extensions
                Underline,
                TextAlign.configure({ types: ['heading', 'paragraph', 'listItem', 'dotImage'] }),
                Highlight.configure({ HTMLAttributes: { style: 'background: #accef7;' } }),
                Link.configure({ openOnClick: true }),
                Placeholder.configure({
                    placeholder: ({ node }) => {
                        if (node.type.name === 'heading') {
                            return `${this.toTitleCase(node.type.name)} ${node.attrs.level}`;
                        }

                        return 'Type "/" for commmands';
                    }
                })
            ]
        });
        this.setEditorStorageData();
    }

    // Here we create the dotConfig name space
    // to storage information in the editor.
    private setEditorStorageData() {
        console.log('TODO: setupt storage');
        this.editor.storage.dotConfig = {
            lang: this.data?.language | DEFAULT_LANG_ID,
            allowedContentTypes: ''
        };
        // this.editor.storage.dotConfig = {
        //   lang: this.lang,
        //   allowedContentTypes: this.allowedContentTypes
        // };
    }

    private toTitleCase(str): void {
        return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
            return txt.charAt(0).toUpperCase() + txt.slice(1);
        });
    }
}
