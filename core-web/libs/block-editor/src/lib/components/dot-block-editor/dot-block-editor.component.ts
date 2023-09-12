import { Subject, from } from 'rxjs';
import { assert, object, string, array, optional } from 'superstruct';

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

import { debounceTime, take, takeUntil } from 'rxjs/operators';

import { AnyExtension, Content, Editor, JSONContent } from '@tiptap/core';
import CharacterCount, { CharacterCountStorage } from '@tiptap/extension-character-count';
import { Level } from '@tiptap/extension-heading';
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import { Subscript } from '@tiptap/extension-subscript';
import { Superscript } from '@tiptap/extension-superscript';
import { TableRow } from '@tiptap/extension-table-row';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import { Youtube } from '@tiptap/extension-youtube';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';

import {
    RemoteCustomExtensions,
    EDITOR_MARKETING_KEYS,
    IMPORT_RESULTS
} from '@dotcms/dotcms-models';

import {
    ActionsMenu,
    BubbleFormExtension,
    BubbleLinkFormExtension,
    DotBubbleMenuExtension,
    DEFAULT_LANG_ID,
    DotConfigExtension,
    DotTableCellExtension,
    DotTableHeaderExtension,
    DotTableExtension,
    DragHandler,
    DotFloatingButton,
    BubbleAssetFormExtension,
    FreezeScroll,
    FREEZE_SCROLL_KEY,
    AssetUploader,
    DotComands,
    AIContentPromptExtension
} from '../../extensions';
import { DotPlaceholder } from '../../extensions/dot-placeholder/dot-placeholder-plugin';
import { ContentletBlock, ImageNode, VideoNode } from '../../nodes';
import {
    formatHTML,
    removeInvalidNodes,
    SetDocAttrStep,
    DotMarketingConfigService,
    RestoreDefaultDOMAttrs
} from '../../shared';
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
    @Input() customBlocks = '';
    @Input() content: Content = '';
    @Input() contentletIdentifier: string;
    @Input() set showVideoThumbnail(value) {
        this.dotMarketingConfigService.setProperty(
            EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL,
            value
        );
    }

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
    subject = new Subject();
    freezeScroll = true;

    private _allowedBlocks: string[] = ['paragraph']; //paragraph should be always.
    private _customNodes: Map<string, AnyExtension> = new Map([
        ['dotContent', ContentletBlock(this.injector)],
        ['image', ImageNode],
        ['video', VideoNode],
        ['table', DotTableExtension()]
    ]);

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

    constructor(
        private injector: Injector,
        public viewContainerRef: ViewContainerRef,
        private dotMarketingConfigService: DotMarketingConfigService
    ) {}

    async loadCustomBlocks(urls: string[]): Promise<PromiseSettledResult<AnyExtension>[]> {
        return Promise.allSettled(urls.map(async (url) => import(/* webpackIgnore: true */ url)));
    }

    ngOnInit() {
        from(this.getCustomRemoteExtensions())
            .pipe(take(1))
            .subscribe((extensions) => {
                this.editor = new Editor({
                    extensions: [
                        ...this.getEditorExtensions(),
                        ...this.getEditorMarks(),
                        ...this.getEditorNodes(),
                        ...extensions
                    ]
                });

                this.editor.on('create', () => this.updateCharCount());
                this.subject
                    .pipe(takeUntil(this.destroy$), debounceTime(250))
                    .subscribe(() => this.updateCharCount());

                this.editor.on('transaction', ({ editor }) => {
                    this.freezeScroll = FREEZE_SCROLL_KEY.getState(editor.view.state)?.freezeScroll;
                });
            });
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onChange(value: JSONContent) {
        this.valueChange.emit(value);
    }

    private updateCharCount(): void {
        const tr = this.editor.state.tr.setMeta('addToHistory', false);

        if (this.characterCount.characters() != 0) {
            tr.step(new SetDocAttrStep('charCount', this.characterCount.characters()))
                .step(new SetDocAttrStep('wordCount', this.characterCount.words()))
                .step(new SetDocAttrStep('readingTime', this.readingTime));
        } else {
            // If the content is empty, we need to remove the attributes
            tr.step(new RestoreDefaultDOMAttrs());
        }

        this.editor.view.dispatch(tr);
    }

    /**
     * assert call throws a detailed error
     * @param data
     * @throws if the schema is not valid to use
     *
     */
    private isValidSchema(data: RemoteCustomExtensions): void {
        const RemoteExtensionsSchema = object({
            extensions: array(
                object({
                    url: string(),
                    actions: optional(
                        array(
                            object({
                                command: string(),
                                menuLabel: string(),
                                icon: string()
                            })
                        )
                    )
                })
            )
        });

        assert(data, RemoteExtensionsSchema);
    }

    private getParsedCustomBlocks(): RemoteCustomExtensions {
        const emptyExtentions = {
            extensions: []
        };

        if (!this.customBlocks?.length) {
            return emptyExtentions;
        }

        try {
            const data = JSON.parse(this.customBlocks);
            this.isValidSchema(data);

            return data;
        } catch (e) {
            console.warn('JSON parse fails, please check the JSON format.', e);

            return {
                extensions: []
            };
        }
    }

    private parsedCustomModules(
        prevModule,
        module: PromiseFulfilledResult<AnyExtension> | PromiseRejectedResult
    ) {
        if (module.status === IMPORT_RESULTS.REJECTED) {
            console.warn('Failed to load the module', module.reason);
        }

        return module.status === IMPORT_RESULTS.FULFILLED
            ? {
                  ...prevModule,
                  ...module?.value
              }
            : { ...prevModule };
    }

    /**
     * This methods get the customBlocks variable to retrieve the custom modules as Objects.
     * Validates that there is customBlocks defined.
     * @private
     * @return {*}  {Promise<AnyExtension[]>}
     * @memberof DotBlockEditorComponent
     */
    private async getCustomRemoteExtensions(): Promise<AnyExtension[]> {
        const data: RemoteCustomExtensions = this.getParsedCustomBlocks();
        const extensionUrls = data?.extensions?.map((extension) => extension.url);
        const customModules = await this.loadCustomBlocks(extensionUrls);
        const blockNames = [];

        data.extensions.forEach((extension) => {
            blockNames.push(...(extension.actions?.map((item) => item.name) || []));
        });

        const moduleObj = customModules.reduce(this.parsedCustomModules, {});

        return Object.values(moduleObj);
    }

    private getEditorNodes(): AnyExtension[] {
        // If you have more than one allow block (other than the paragraph),
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
                allowedBlocks: this._allowedBlocks,
                contentletIdentifier: this.contentletIdentifier
            }),
            DotComands,
            DotPlaceholder.configure({ placeholder: 'Type "/" for commands' }),
            Youtube.configure({
                height: 300,
                width: 400,
                interfaceLanguage: 'us',
                nocookie: true,
                modestBranding: true
            }),
            Subscript,
            Superscript,
            ActionsMenu(this.viewContainerRef, this.getParsedCustomBlocks()),
            DragHandler(this.viewContainerRef),
            BubbleLinkFormExtension(this.viewContainerRef, this.lang),
            DotBubbleMenuExtension(this.viewContainerRef),
            BubbleFormExtension(this.viewContainerRef),
            AIContentPromptExtension(this.viewContainerRef),
            DotFloatingButton(this.injector, this.viewContainerRef),
            DotTableCellExtension(this.viewContainerRef),
            BubbleAssetFormExtension(this.viewContainerRef),
            DotTableHeaderExtension(),
            TableRow,
            FreezeScroll,
            CharacterCount,
            AssetUploader(this.injector, this.viewContainerRef)
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

    private setEditorJSONContent(content: Content) {
        this.content =
            this._allowedBlocks?.length > 1
                ? removeInvalidNodes(content, this._allowedBlocks)
                : content;
    }
}
