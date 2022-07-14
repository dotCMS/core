import { Component, Injector, Input, OnInit, ViewContainerRef } from '@angular/core';
import { Editor, Extensions } from '@tiptap/core';
import { HeadingOptions } from '@tiptap/extension-heading';
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

    _allowedBlocks: string[];
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
        ];
        if (this.allowedBlocks) {
            const allowedArray = this.allowedBlocks.split(',');
            console.log(allowedArray);
            const headingOptions: HeadingOptions = { levels: [], HTMLAttributes: {} };
            const customExtension: Extensions = [...defaultExtensions];
            const starterKitOptions: Partial<StarterKitOptions> = {
                heading: false,
                orderedList: false,
                bulletList: false,
                blockquote: false,
                codeBlock: false,
                horizontalRule: false
            };
            allowedArray.forEach((block: string) => {
                switch (block) {
                    case 'heading1':
                        headingOptions.levels.push(1);
                        break;
                    case 'heading2':
                        headingOptions.levels.push(2);
                        break;
                    case 'heading3':
                        headingOptions.levels.push(3);
                        break;
                    case 'orderedList':
                        delete starterKitOptions.orderedList;
                        break;
                    case 'bulletList':
                        delete starterKitOptions.bulletList;
                        break;
                    case 'blockquote':
                        delete starterKitOptions.blockquote;
                        break;
                    case 'codeBlock':
                        delete starterKitOptions.codeBlock;
                        break;
                    case 'horizontalRule':
                        delete starterKitOptions.horizontalRule;
                        break;
                    case 'contentlets':
                        customExtension.push(ContentletBlock(this.injector));
                }
            });
            starterKitOptions.heading = headingOptions.levels.length ? headingOptions : false;
            return [...customExtension, StarterKit.configure(starterKitOptions)];
        }

        return [StarterKit, ContentletBlock(this.injector), ...defaultExtensions];
    }

}
