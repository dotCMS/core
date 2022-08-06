import { Component, Injector, Input, OnInit, ViewContainerRef } from '@angular/core';
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
import { AnyExtension, Editor } from '@tiptap/core';
import { Underline } from '@tiptap/extension-underline';
import { TextAlign } from '@tiptap/extension-text-align';
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';
import { HeadingOptions, Level } from '@tiptap/extension-heading';

function toTitleCase(str) {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
}

@Component({
    selector: 'dotcms-block-editor',
    templateUrl: './dotcms-block-editor.component.html',
    styleUrls: ['./dotcms-block-editor.component.scss']
})
export class DotcmsBlockEditorComponent implements OnInit {
    @Input() lang = DEFAULT_LANG_ID;
    @Input() allowedContentTypes = '';
    @Input() value: { [key: string]: string } | string = ''; // can be HTML or JSON, see https://www.tiptap.dev/api/editor#content

    @Input() set allowedBlocks(blocks: string) {
        this._allowedBlocks = [
            ...this._allowedBlocks,
            ...(blocks ? blocks.replace(/ /g, '').split(',').filter(Boolean) : [])
        ];
    }

    _allowedBlocks = ['paragraph']; //paragraph should be always.
    editor: Editor;

    constructor(private injector: Injector, public viewContainerRef: ViewContainerRef) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: this.setEditorExtensions()
        });
    }
    private setEditorExtensions(): AnyExtension[] {
        const defaultExtensions: AnyExtension[] = [
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
        const customExtensions: Map<string, AnyExtension> = new Map([
            ['contentlets', ContentletBlock(this.injector)],
            ['dotImage', ImageBlock(this.injector)]
        ]);

        return [
            ...defaultExtensions,
            ...(this._allowedBlocks.length > 1
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
                if (this._allowedBlocks[heading]) {
                    headingOptions.levels.push(+heading.slice(-1) as Level);
                }
            }
        );

        return {
            heading: headingOptions.levels.length ? headingOptions : false,
            ...staterKitOptions.reduce(
                (object, item) => ({
                    ...object,
                    ...(this._allowedBlocks[item] ? {} : { [item]: false })
                }),
                {}
            )
        };
    }

    private setCustomExtensions(customExtensions: Map<string, AnyExtension>): AnyExtension[] {
        return [
            ...(this._allowedBlocks['contentlets'] ? [customExtensions['contentlets']] : []),
            ...(this._allowedBlocks['dotImage'] ? [customExtensions['dotImage']] : [])
        ];
    }
}
