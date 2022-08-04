import { Component, Injector, OnDestroy, OnInit, ViewContainerRef } from '@angular/core';

import { AnyExtension, Editor } from '@tiptap/core';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';
import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DEFAULT_LANG_ID,
    DotBubbleMenuExtension,
    DotConfigExtension,
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
import { HeadingOptions, Level } from '@tiptap/extension-heading';

export interface BlockEditorData {
    content: { [key: string]: string };
    allowedContentTypes: string;
    allowedBlocks: string[];
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
    saving = false;
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
                    language: parseInt(event.data.dataset.language) || DEFAULT_LANG_ID,
                    allowedBlocks: event.data.dataset.allowedBlocks
                        ? [
                              'paragraph',
                              ...event.data.dataset.allowedBlocks
                                  .replace(/ /g, '')
                                  .split(',')
                                  .filter(Boolean)
                          ]
                        : [],
                    allowedContentTypes: event.data.dataset.allowedContentTypes,
                    inode: event.data.dataset.inode,
                    content: JSON.parse(event.data.dataset.content)
                };
                this.editor = new Editor({
                    extensions: this.setEditorExtensions()
                });
            });
    }

    saveEditorChanges(): void {
        this.saving = true;
        this.dotWorkflowActionsFireService
            .saveContentlet({
                [this.data.fieldName]: JSON.stringify(this.editor.getJSON()),
                inode: this.data.inode,
                indexPolicy: 'WAIT_FOR'
            })
            .pipe(take(1))
            .subscribe(() => {
                this.saving = false;
                const customEvent = new CustomEvent('ng-event', { detail: { name: 'in-iframe' } });
                window.top.document.dispatchEvent(customEvent);
                this.editor.destroy();
                this.editor = null;
            });
    }

    closeSidebar(): void {
        this.editor = null;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private setEditorExtensions(): AnyExtension[] {
        const defaultExtensions: AnyExtension[] = [
            DotConfigExtension({
                lang: this.data.language,
                allowedContentTypes: this.data.allowedContentTypes,
                allowedBlocks: this.data.allowedBlocks
            }),
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
        ];
        const customExtensions: Map<string, AnyExtension> = new Map([
            ['contentlets', ContentletBlock(this.injector)],
            ['dotImage', ImageBlock(this.injector)]
        ]);

        return [
            ...defaultExtensions,
            ...(this.data.allowedBlocks.length
                ? [
                      StarterKit.configure(this.setStarterKitOptions()),
                      ...this.setCustomExtensions(customExtensions)
                  ]
                : [StarterKit, ...customExtensions.values()])
        ];
    }

    /**
     *
     * Check if the starter kit keys are part of the _allowedBlocks,
     * ONLY if is not present will add an attribute with false to disable it. ex. {orderedList: false}.
     * Exception, headings fill the HeadingOptions or false.
     */
    private setStarterKitOptions(): Partial<StarterKitOptions> {
        // These are the keys that meter for the starter kit.
        const staterKitOptions = [
            'orderedList',
            'bulletList',
            'blockquote',
            'codeBlock',
            'horizontalRule'
        ];
        const headingOptions: HeadingOptions = { levels: [], HTMLAttributes: {} };

        //Heading types supported by default in the editor.
        ['heading1', 'heading2', 'heading3', 'heading4', 'heading5', 'heading6'].forEach(
            (heading) => {
                if (this.data.allowedBlocks[heading]) {
                    headingOptions.levels.push(+heading.slice(-1) as Level);
                }
            }
        );

        return {
            heading: headingOptions.levels.length ? headingOptions : false,
            ...staterKitOptions.reduce(
                (object, item) => ({
                    ...object,
                    ...(this.data.allowedBlocks[item] ? {} : { [item]: false })
                }),
                {}
            )
        };
    }

    private setCustomExtensions(customExtensions: Map<string, AnyExtension>): AnyExtension[] {
        return [
            ...(this.data.allowedBlocks['contentlets'] ? [customExtensions['contentlets']] : []),
            ...(this.data.allowedBlocks['dotImage'] ? [customExtensions['dotImage']] : [])
        ];
    }

    private toTitleCase(str): void {
        return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
            return txt.charAt(0).toUpperCase() + txt.slice(1);
        });
    }
}
