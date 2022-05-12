import { Component, Injector, Input, OnInit, ViewContainerRef } from '@angular/core';
import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';

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

// Marks Extensions
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';

function toTitleCase(str) {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
}

@Component({
    selector: 'dotcms-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss']
})
export class DotBlockEditorComponent implements OnInit {
    @Input() lang = DEFAULT_LANG_ID;

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
                Link.configure({ openOnClick: true }),
                Placeholder.configure({
                    placeholder: ({ node }) => {
                        if (node.type.name === 'heading') {
                            return `${toTitleCase(node.type.name)} ${node.attrs.level}`;
                        }

                        return 'Type "/" for commmands';
                    }
                })
            ]
        });

        this.setEditorStorageData();
    }

    private setEditorStorageData() {
        this.editor.storage.dotConfig = { lang: this.lang };
    }
}
