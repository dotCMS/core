import { Component, OnInit, Injector } from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DotBubbleMenuExtension,
    DragHandler,
    ImageBlock,
    ImageUpload
} from '@dotcms/block-editor';

// Marks Extensions
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import { ViewContainerRef } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss']
})
export class DotBlockEditorComponent implements OnInit {
    editor: Editor;

    value = ''; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    constructor(private injector: Injector, public viewContainerRef: ViewContainerRef) {}

    ngOnInit() {
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
                Link.configure({ openOnClick: true })
            ]
        });
    }
}
