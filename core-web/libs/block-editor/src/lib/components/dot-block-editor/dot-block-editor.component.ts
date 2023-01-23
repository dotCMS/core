import { Component, Injector, Input, OnInit, ViewContainerRef, OnDestroy } from '@angular/core';

import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';

import { AnyExtension, Content, Editor } from '@tiptap/core';
import { Level } from '@tiptap/extension-heading';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';
import Placeholder from '@tiptap/extension-placeholder';

import {
    ActionsMenu,
    BubbleLinkFormExtension,
    ContentletBlock,
    DEFAULT_LANG_ID,
    DotBubbleMenuExtension,
    DragHandler,
    ImageUpload,
    DotConfigExtension,
    BubbleFormExtension,
    DotFloatingButton,
    ImageNode,
    SetDocAttrStep,
    formatHTML,
    DotTableCellExtension,
    DotTableExtension,
    DotTableHeaderExtension
} from '@dotcms/block-editor';

// Marks Extensions
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import TextAlign from '@tiptap/extension-text-align';
import Underline from '@tiptap/extension-underline';
import CharacterCount, { CharacterCountStorage } from '@tiptap/extension-character-count';
import TableRow from '@tiptap/extension-table-row';

function toTitleCase(str) {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
}

@Component({
    selector: 'dot-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss']
})
export class DotBlockEditorComponent implements OnInit, OnDestroy {
    @Input() lang = DEFAULT_LANG_ID;
    @Input() allowedContentTypes: string;
    @Input() customStyles: string;
    @Input() displayCountBar: boolean | string = true;
    @Input() charLimit: number;

    @Input() set allowedBlocks(blocks: string) {
        this._allowedBlocks = [
            'paragraph', //paragraph should be always.
            ...(blocks ? blocks.replace(/ /g, '').split(',').filter(Boolean) : [])
        ];
    }

    @Input() set setValue(content: Content) {
        // https://www.tiptap.dev/api/editor#content
        this.editor.commands.setContent(
            typeof content === 'string' ? formatHTML(content) : content
        );
    }

    editor: Editor;
    subject = new Subject();

    private _allowedBlocks = ['paragraph', 'heading1', 'codeBlock'];
    private destroy$: Subject<boolean> = new Subject<boolean>();

    get characterCount(): CharacterCountStorage {
        return this.editor?.storage.characterCount;
    }

    get showCharData() {
        try {
            return JSON.parse(this.displayCountBar as string);
        } catch (e) {
            return true;
        }
    }

    get readingTime() {
        // The constant used by Medium for words an adult can read per minute is 265
        // More Information here: https://help.medium.com/hc/en-us/articles/214991667-Read-time
        return Math.ceil(this.characterCount.words() / 265);
    }

    constructor(private injector: Injector, public viewContainerRef: ViewContainerRef) {}

    ngOnInit() {
        this.editor = new Editor({
            extensions: [...this.dotExtensions(), ...this.editorMarks(), ...this.editorNode()]
        });

        this.editor.on('create', () => this.updateChartCount());
        this.subject
            .pipe(takeUntil(this.destroy$), debounceTime(250))
            .subscribe(() => this.updateChartCount());
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private updateChartCount(): void {
        const tr = this.editor.state.tr
            .step(new SetDocAttrStep('chartCount', this.characterCount.characters()))
            .step(new SetDocAttrStep('wordCount', this.characterCount.words()))
            .step(new SetDocAttrStep('readingTime', this.readingTime));
        this.editor.view.dispatch(tr);
    }

    private editorNode(): AnyExtension[] {
        const starterkit = this._allowedBlocks
            ? StarterKit.configure(this.starterConfig())
            : StarterKit;

        const customNodes = this.getAllowedCustomBlocks(this._allowedBlocks);

        return [starterkit, ...customNodes];
    }

    /**
     *
     * Check if the starter kit keys are part of the _allowedBlocks,
     * ONLY if is not present will add an attribute with false to disable it. ex. {orderedList: false}.
     * Exception, headings fill the HeadingOptions or false.
     */
    private starterConfig(): Partial<StarterKitOptions> {
        // These are the keys that meter for the starter kit.
        const staterKitOptions = [
            'orderedList',
            'bulletList',
            'blockquote',
            'codeBlock',
            'horizontalRule'
        ];

        //Heading types supported by default in the editor.
        const heading = ['heading1', 'heading2', 'heading3', 'heading4', 'heading5', 'heading6'];
        const levels = heading
            .filter((heading) => this._allowedBlocks.includes(heading))
            .map((heading) => +heading.slice(-1) as Level);

        const starterKit = staterKitOptions
            .filter((option) => this._allowedBlocks.includes(option))
            .reduce((options, option) => ({ ...options, [option]: false }), {});

        return {
            heading: levels?.length ? { levels, HTMLAttributes: {} } : false,
            ...starterKit
        };
    }

    private getAllowedCustomBlocks(list: string[] = []): AnyExtension[] {
        const whiteList = [];
        const customBlock: Map<string, AnyExtension> = new Map([
            ['contentlets', ContentletBlock(this.injector)],
            ['image', ImageNode]
        ]);

        if (!list.length) {
            return [...customBlock.values()];
        }

        for (const item of list) {
            const node = customBlock.get(item);
            if (node) {
                whiteList.push(node);
            }
        }

        return whiteList;
    }

    /**
     * Extensions that improve the user experience
     *
     * @private
     * @return {*}
     * @memberof DotBlockEditorComponent
     */
    private dotExtensions() {
        return [
            DotConfigExtension({
                lang: this.lang,
                allowedContentTypes: this.allowedContentTypes,
                allowedBlocks: this._allowedBlocks
            }),
            Placeholder.configure({ placeholder: this.placeholder }),
            ActionsMenu(this.viewContainerRef),
            DragHandler(this.viewContainerRef),
            ImageUpload(this.injector, this.viewContainerRef),
            BubbleLinkFormExtension(this.viewContainerRef),
            DotBubbleMenuExtension(this.viewContainerRef),
            BubbleFormExtension(this.viewContainerRef),
            DotFloatingButton(this.injector, this.viewContainerRef),
            DotTableCellExtension(this.viewContainerRef),
            DotTableHeaderExtension(),
            DotTableExtension(),
            TableRow,
            CharacterCount
        ];
    }

    /**
     * Styles that can be applied to Paragraph
     *
     * @private
     * @return {*}
     * @memberof DotBlockEditorComponent
     */
    private editorMarks() {
        return [
            Underline,
            TextAlign.configure({ types: ['heading', 'paragraph', 'listItem', 'dotImage'] }),
            Highlight.configure({ HTMLAttributes: { style: 'background: #accef7;' } }),
            Link.configure({ autolink: false, openOnClick: false })
        ];
    }

    private placeholder({ node }) {
        if (node.type.name === 'heading') {
            return `${toTitleCase(node.type.name)} ${node.attrs.level}`;
        }

        return 'Type "/" for commmands';
    }
}
