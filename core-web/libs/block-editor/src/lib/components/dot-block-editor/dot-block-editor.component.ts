import {
    Component,
    EventEmitter,
    Injector,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewContainerRef
} from '@angular/core';

import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';

import { AnyExtension, Content, Editor, JSONContent } from '@tiptap/core';
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
    DotTableHeaderExtension,
    BubbleAssetFormExtension,
    removeInvalidNodes,
    VideoNode
} from '@dotcms/block-editor';

// Marks Extensions
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import CharacterCount, { CharacterCountStorage } from '@tiptap/extension-character-count';
import { TableRow } from '@tiptap/extension-table-row';

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
    @Input() content: Content = '';

    @Input() set allowedBlocks(blocks: string) {
        const allowedBlocks = blocks ? blocks.replace(/ /g, '').split(',').filter(Boolean) : [];

        this._allowedBlocks = [...this._allowedBlocks, ...allowedBlocks];
    }

    @Input() set value(content: Content) {
        if (typeof content === 'string') {
            this.content = formatHTML(content);

            return;
        }

        this.setEditorJSONContent(content);
    }

    @Output() valueChange = new EventEmitter<JSONContent>();

    editor: Editor;

    private _allowedBlocks: string[] = ['paragraph']; //paragraph should be always.
    private _customNodes: Map<string, AnyExtension> = new Map([
        ['dotContent', ContentletBlock(this.injector)],
        ['image', ImageNode],
        ['video', VideoNode],
        ['table', DotTableExtension()]
    ]);
    private destroy$: Subject<boolean> = new Subject<boolean>();

    onChange(value: JSONContent) {
        this.valueChange.emit(value);
    }

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
            extensions: [
                ...this.getEditorExtensions(),
                ...this.getEditorMarks(),
                ...this.getEditorNodes()
            ]
        });

        this.valueChange
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

    private getEditorNodes(): AnyExtension[] {
        // If you have more than one allow block (other than the parragrph),
        // we customize the starterkit.
        const starterkit =
            this._allowedBlocks?.length > 1
                ? StarterKit.configure(this.starterConfig())
                : StarterKit;

        const customNodes = this.getAllowedCustomNodes();

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
            .filter((heading) => this._allowedBlocks?.includes(heading))
            .map((heading) => +heading.slice(-1) as Level);

        const starterKit = staterKitOptions
            .filter((option) => !this._allowedBlocks?.includes(option))
            .reduce((options, option) => ({ ...options, [option]: false }), {});

        return {
            heading: levels?.length ? { levels, HTMLAttributes: {} } : false,
            ...starterKit
        };
    }

    /**
     * Filter the dot Nodes that are allowed by the user
     *
     * @private
     * @return {*}  {AnyExtension[]}
     * @memberof DotBlockEditorComponent
     */
    private getAllowedCustomNodes(): AnyExtension[] {
        const whiteList = [];

        // If only paragraph is included
        // We do not need to filter
        if (this._allowedBlocks.length <= 1) {
            return [...this._customNodes.values()];
        }

        for (const block of this._allowedBlocks) {
            const node = this._customNodes.get(block);
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
    private getEditorExtensions() {
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
            BubbleAssetFormExtension(this.viewContainerRef),
            DotTableHeaderExtension(),
            TableRow,
            CharacterCount
        ];
    }

    /**
     * Editor Marks
     *
     * @private
     * @return {*}
     * @memberof DotBlockEditorComponent
     */
    private getEditorMarks() {
        return [
            Underline,
            TextAlign.configure({ types: ['heading', 'paragraph', 'listItem', 'dotImage'] }),
            Highlight.configure({ HTMLAttributes: { style: 'background: #accef7;' } }),
            Link.configure({ autolink: false, openOnClick: false })
        ];
    }

    /**
     * Placeholder function
     *
     * @private
     * @param {*} { node }
     * @return {*}
     * @memberof DotBlockEditorComponent
     */
    private placeholder({ node }) {
        if (node.type.name === 'heading') {
            return `${toTitleCase(node.type.name)} ${node.attrs.level}`;
        }

        return 'Type "/" for commmands';
    }

    private setEditorJSONContent(content: Content) {
        this.content =
            this._allowedBlocks?.length > 1
                ? removeInvalidNodes(content, this._allowedBlocks)
                : content;
    }
}
