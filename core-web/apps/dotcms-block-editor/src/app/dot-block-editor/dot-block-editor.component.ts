import { Component, Injector, Input, OnInit, ViewContainerRef } from '@angular/core';
import {Editor, Extensions} from '@tiptap/core';
import { HeadingOptions, Level } from '@tiptap/extension-heading';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';

import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DEFAULT_LANG_ID,
    DotBubbleMenuExtension,
    DragHandler,
    ImageBlock,
    ImageUpload,
    DotConfigExtension
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
    @Input() allowedContentTypes = '';
    @Input() set allowedBlocks(blocks: string) {
        this._allowedBlocks = ['paragraph', ...blocks.replace(/ /g, '').split(',').filter(Boolean)];
    }

    _allowedBlocks = [];
    editor: Editor;

    value = ''; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    constructor(private injector: Injector, public viewContainerRef: ViewContainerRef) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: this.setEditorExtensions()
        });
    }



    private setEditorExtensions(): Extensions {
        const defaultExtensions: Extensions = [
            DotConfigExtension({
                lang: this.lang,
                allowedContentTypes: this.allowedContentTypes,
                allowedBlocks: this._allowedBlocks
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
                        return `${toTitleCase(node.type.name)} ${node.attrs.level}`;
                    }

                    return 'Type "/" for commmands';
                }
            })
        ];
        if (this.allowedBlocks) {
            const customExtension: Extensions = [...defaultExtensions];
            this._allowedBlocks.forEach((block: string) => {
                switch (block) {
                    case 'contentlets':
                        customExtension.push(ContentletBlock(this.injector));
                        break
                    case 'dotImage':
                        customExtension.push(ImageBlock(this.injector));
                        break
                }
            });

            return [...customExtension, StarterKit.configure(this.setStarterKitOptions())];
        }

        return [StarterKit, ...defaultExtensions, ImageBlock(this.injector), ContentletBlock(this.injector)];
    }

    private setStarterKitOptions() : Partial<StarterKitOptions> {
        const headingOptions: HeadingOptions = { levels: [], HTMLAttributes: {} };
        ['heading1', 'heading2', 'heading3',  'heading4',  'heading5',  'heading6'].forEach((heading) => {
            if (this._allowedBlocks[heading]){
                headingOptions.levels.push(+heading.slice(-1) as Level)
            }
        });

        return { heading: headingOptions.levels.length ? headingOptions : false,
            ...(this._allowedBlocks['orderedList'] ? {} : {orderedList: false}),
            ...(this._allowedBlocks['bulletList'] ? {} : {bulletList: false}),
            ...(this._allowedBlocks['blockquote'] ? {} : {blockquote: false}),
            ...(this._allowedBlocks['codeBlock'] ? {} : {codeBlock: false}),
            ...(this._allowedBlocks['horizontalRule'] ? {} : {horizontalRule: false})
        };
    }


}
