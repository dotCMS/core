import { combineLatest, from, Observable, Subject } from 'rxjs';
import { array, assert, object, optional, string } from 'superstruct';

import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    forwardRef,
    inject,
    Injector,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { debounceTime, map, take, takeUntil } from 'rxjs/operators';

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

import { DotPropertiesService, DotAiService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    EDITOR_MARKETING_KEYS,
    IMPORT_RESULTS,
    RemoteCustomExtensions
} from '@dotcms/dotcms-models';

import {
    ActionsMenu,
    AIContentPromptExtension,
    AIImagePromptExtension,
    AssetUploader,
    BubbleAssetFormExtension,
    BubbleFormExtension,
    BubbleLinkFormExtension,
    DEFAULT_LANG_ID,
    DotBubbleMenuExtension,
    DotComands,
    DotConfigExtension,
    DotFloatingButton,
    DotTableCellExtension,
    DotTableExtension,
    DotTableHeaderExtension,
    DragHandler,
    FREEZE_SCROLL_KEY,
    FreezeScroll
} from '../../extensions';
import { DotPlaceholder } from '../../extensions/dot-placeholder/dot-placeholder-plugin';
import { AIContentNode, ContentletBlock, ImageNode, LoaderNode, VideoNode } from '../../nodes';
import {
    DotMarketingConfigService,
    formatHTML,
    removeInvalidNodes,
    RestoreDefaultDOMAttrs,
    SetDocAttrStep
} from '../../shared';

@Component({
    selector: 'dot-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotBlockEditorComponent),
            multi: true
        }
    ]
})
export class DotBlockEditorComponent implements OnInit, OnDestroy, ControlValueAccessor {
    readonly #injector = inject(Injector);

    @Input() field: DotCMSContentTypeField;
    @Input() contentlet: DotCMSContentlet;

    @Input() languageId = DEFAULT_LANG_ID;
    @Input() isFullscreen = false;
    @Input() value: Content = '';
    @Output() valueChange = new EventEmitter<JSONContent>();
    public allowedContentTypes: string;
    public customStyles: string;
    public displayCountBar: boolean | string = true;
    public charLimit: number;
    public customBlocks = '';
    public content: Content = '';
    public contentletIdentifier: string;
    editor: Editor;
    subject = new Subject();
    freezeScroll = true;
    private onChange: (value: string) => void;
    private onTouched: () => void;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private allowedBlocks: string[] = ['paragraph']; //paragraph should be always.
    private _customNodes: Map<string, AnyExtension> = new Map([
        ['dotContent', ContentletBlock(this.#injector)],
        ['image', ImageNode],
        ['video', VideoNode],
        ['table', DotTableExtension()],
        ['aiContent', AIContentNode],
        ['loader', LoaderNode]
    ]);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotPropertiesService = inject(DotPropertiesService);
    private isAIPluginInstalled$: Observable<boolean>;

    constructor(
        private readonly viewContainerRef: ViewContainerRef,
        private readonly dotMarketingConfigService: DotMarketingConfigService,
        private readonly dotAiService: DotAiService
    ) {
        this.isAIPluginInstalled$ = this.dotAiService.checkPluginInstallation();
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

    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    writeValue(content: JSONContent): void {
        this.value = content;
        this.setEditorContent(content);
    }

    async loadCustomBlocks(urls: string[]): Promise<PromiseSettledResult<AnyExtension>[]> {
        return Promise.allSettled(urls.map(async (url) => import(/* webpackIgnore: true */ url)));
    }

    ngOnInit() {
        this.setFieldVariable(); // Set the field variables - Before the editor is created
        combineLatest([
            this.showVideoThumbnail$(),
            from(this.getCustomRemoteExtensions()),
            this.isAIPluginInstalled$
        ])
            .pipe(take(1))
            .subscribe(([showVideoThumbnail, extensions, isInstalled]) => {
                this.editor = new Editor({
                    extensions: [
                        ...this.getEditorExtensions(isInstalled),
                        ...this.getEditorMarks(),
                        ...this.getEditorNodes(),
                        ...extensions
                    ]
                });

                this.dotMarketingConfigService.setProperty(
                    EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL,
                    showVideoThumbnail
                );

                this.subscribeToEditorEvents();
            });
    }

    ngOnDestroy() {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onBlockEditorChange(value: JSONContent) {
        this.valueChange.emit(value);
        this.onChange?.(JSON.stringify(value));
        this.onTouched?.();
    }

    setAllowedBlocks(blocks: string) {
        const allowedBlocks = blocks ? blocks.replace(/ /g, '').split(',').filter(Boolean) : [];

        this.allowedBlocks = [...this.allowedBlocks, ...allowedBlocks];
    }

    /**
     * Subscribe to the editor events
     *
     * @private
     * @memberof DotBlockEditorComponent
     */
    private subscribeToEditorEvents() {
        this.editor.on('create', () => {
            this.setEditorContent(this.value);
            this.updateCharCount();
        });
        this.subject
            .pipe(takeUntil(this.destroy$), debounceTime(250))
            .subscribe(() => this.updateCharCount());

        this.editor.on('transaction', ({ editor }) => {
            this.freezeScroll = FREEZE_SCROLL_KEY.getState(editor.view.state)?.freezeScroll;
        });

        this.cd.detectChanges();
    }

    /**
     * Update the character count
     *
     * @private
     * @memberof DotBlockEditorComponent
     */
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

    private showVideoThumbnail$(): Observable<boolean> {
        return this.dotPropertiesService
            .getKey(EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL)
            .pipe(map((property = 'true') => property === 'true' || property === 'NOT_FOUND'));
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
            this.allowedBlocks?.length > 1
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
            .filter((heading) => this.allowedBlocks?.includes(heading))
            .map((heading) => +heading.slice(-1) as Level);

        const starterKit = staterKitOptions
            .filter((option) => !this.allowedBlocks?.includes(option))
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
        if (this.allowedBlocks.length <= 1) {
            return [...this._customNodes.values()];
        }

        for (const block of this.allowedBlocks) {
            const node = this._customNodes.get(block);
            if (node) {
                whiteList.push(node);
            }
        }

        return whiteList;
    }

    /**
     * Returns an array of editor extensions
     *
     * @private
     * @returns {Array} An array of editor extensions
     */
    private getEditorExtensions(isAIPluginInstalled: boolean) {
        const extensions = [
            DotConfigExtension({
                lang: this.languageId || this.contentlet?.languageId,
                allowedContentTypes: this.allowedContentTypes,
                allowedBlocks: this.allowedBlocks,
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
            ActionsMenu(this.viewContainerRef, this.getParsedCustomBlocks(), {
                shouldShowAIExtensions: isAIPluginInstalled
            }),
            DragHandler(this.viewContainerRef),
            BubbleLinkFormExtension(this.viewContainerRef, this.languageId),
            DotBubbleMenuExtension(this.viewContainerRef),
            BubbleFormExtension(this.viewContainerRef),
            DotFloatingButton(this.#injector, this.viewContainerRef),
            DotTableCellExtension(this.viewContainerRef),
            BubbleAssetFormExtension(this.viewContainerRef),
            DotTableHeaderExtension(),
            TableRow,
            FreezeScroll,
            CharacterCount,
            AssetUploader(this.#injector, this.viewContainerRef)
        ];

        if (isAIPluginInstalled) {
            extensions.push(
                AIContentPromptExtension(this.viewContainerRef),
                AIImagePromptExtension(this.viewContainerRef)
            );
        }

        return extensions;
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
            this.allowedBlocks?.length > 1
                ? removeInvalidNodes(content, this.allowedBlocks)
                : content;
    }

    private setEditorContent(content: Content) {
        if (typeof content === 'string') {
            this.content = formatHTML(content);

            return;
        }

        this.setEditorJSONContent(content);
    }

    private setFieldVariable() {
        const { contentTypes, styles, displayCountBar, charLimit, customBlocks, allowedBlocks } =
            this.getFieldVariables();

        this.allowedContentTypes = contentTypes;
        this.customStyles = styles;
        this.displayCountBar = displayCountBar;
        this.charLimit = Number(charLimit);
        this.customBlocks = customBlocks;
        this.setAllowedBlocks(allowedBlocks);
    }

    /**
     * Get field variables
     *
     * @private
     * @return {*}  {Record<string, string>}
     * @memberof DotBlockEditorComponent
     */
    private getFieldVariables(): Record<string, string> {
        return (
            this.field?.fieldVariables.reduce(
                (prev, { key, value }) => ({
                    ...prev,
                    [key]: value
                }),
                {}
            ) || {}
        );
    }
}
