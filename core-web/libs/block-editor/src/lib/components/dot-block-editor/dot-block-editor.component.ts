import { combineLatest, from, Observable, Subject } from 'rxjs';
import { array, assert, optional, string, type as structType } from 'superstruct';
import tippy from 'tippy.js';

import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    forwardRef,
    inject,
    Injector,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewContainerRef,
    ChangeDetectionStrategy
} from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    NG_VALUE_ACCESSOR,
    NgControl
} from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { debounceTime, map, take, takeUntil } from 'rxjs/operators';

import { AnyExtension, Content, Editor, JSONContent } from '@tiptap/core';
import CharacterCount from '@tiptap/extension-character-count';
import { Level } from '@tiptap/extension-heading';
import { Highlight } from '@tiptap/extension-highlight';
import { Link } from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { Subscript } from '@tiptap/extension-subscript';
import { Superscript } from '@tiptap/extension-superscript';
import { TextAlign } from '@tiptap/extension-text-align';
import { Underline } from '@tiptap/extension-underline';
import { Youtube } from '@tiptap/extension-youtube';
import StarterKit, { StarterKitOptions } from '@tiptap/starter-kit';

import { DotAiService, DotMessageService, DotPropertiesService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    EDITOR_MARKETING_KEYS,
    getDeclaredRemoteBlockNames,
    IMPORT_RESULTS,
    RemoteCustomExtensions,
    warnOnUnmatchedRemoteBlockNames
} from '@dotcms/dotcms-models';

import {
    ActionsMenu,
    AIContentPromptExtension,
    AIImagePromptExtension,
    AssetUploader,
    BubbleAssetFormExtension,
    BubbleFormExtension,
    DotCMSTableExtensions,
    DotComands,
    DotConfigExtension,
    DotFloatingButton,
    DotTableCellContextMenu,
    FREEZE_SCROLL_KEY,
    FreezeScroll,
    IndentExtension
} from '../../extensions';
import {
    AIContentNode,
    AudioNode,
    ContentletBlock,
    createGridColumn,
    GridBlock,
    ImageNode,
    LoaderNode,
    UnsupportedBlockNode,
    VideoNode
} from '../../nodes';
import {
    DEFAULT_LANG_ID,
    DotMarketingConfigService,
    formatHTML,
    removeInvalidNodes,
    restoreUnknownBlockNodes,
    RestoreDefaultDOMAttrs,
    preserveUnknownBlockNodes,
    SetDocAttrStep
} from '../../shared';

@Component({
    selector: 'dot-old-block-editor',
    templateUrl: './dot-block-editor.component.html',
    styleUrls: ['./dot-block-editor.component.css'],
    providers: [
        DialogService,
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotBlockEditorComponent),
            multi: true
        }
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
/**
 * @deprecated Legacy block editor — kept on the rollback path behind `FEATURE_FLAG_NEW_BLOCK_EDITOR`
 * so customers can opt out of the new TipTap-v3 editor (`DotCMSEditorComponent` in `@dotcms/new-block-editor`).
 * Slated for removal once the new editor exits QA. Do not extend this component — file new work against the new editor.
 */
export class DotBlockEditorComponent implements OnInit, OnChanges, OnDestroy, ControlValueAccessor {
    readonly #injector = inject(Injector);
    /** Schema node names captured as soon as the TipTap editor instance exists. */
    readonly #knownEditorNodeNames = new Set<string>();
    /** Buffers incoming form content until the editor create lifecycle can safely consume it. */
    #pendingValue: Content | null = null;
    /** Field-level allowed blocks, with paragraph forced in as the legacy default. */
    #allowedBlocks: string[] = ['paragraph']; //paragraph should be always.

    // Optional: both are read through `?.` already, so neither is required to be bound.
    @Input() field?: DotCMSContentTypeField;
    @Input() contentlet?: DotCMSContentlet;

    @Input() languageId = DEFAULT_LANG_ID;
    @Input() isFullscreen = false;
    @Input() hasFieldError = false;
    @Input() value: Content = '';
    @Output() valueChange = new EventEmitter<JSONContent>();
    public allowedContentTypes = '';
    public customStyles = '';
    public displayCountBar: boolean | string = true;
    /** NaN until `setFieldVariable` runs, and NaN whenever the field variable is absent —
     * `updateCharLimitValidity` treats any non-finite limit as "no limit". */
    public charLimit = NaN;
    public customBlocks = '';
    public content: Content = '';
    public contentletIdentifier = '';
    public disabled = false;
    /** Null until the async `ngOnInit` finishes building it; `writeValue` can arrive first. */
    editor: Editor | null = null;
    subject = new Subject();
    freezeScroll = true;
    // Assigned by Angular through `registerOnChange` / `registerOnTouched` before either is
    // called. Left without a no-op default so a call outside a form still fails loudly.
    private onChange!: (value: string) => void;
    private onTouched!: () => void;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private _customNodes = new Map([
        ['dotContent', ContentletBlock(this.#injector)],
        ['image', ImageNode],
        ['video', VideoNode],
        ['audio', AudioNode],
        ['aiContent', AIContentNode],
        ['loader', LoaderNode],
        ['gridBlock', GridBlock]
    ]);
    private readonly cd = inject(ChangeDetectorRef);
    private readonly dotPropertiesService = inject(DotPropertiesService);
    private isAIPluginInstalled$!: Observable<boolean>;
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly viewContainerRef = inject(ViewContainerRef);
    readonly dotMarketingConfigService = inject(DotMarketingConfigService);
    readonly dotAiService = inject(DotAiService);

    readonly dotDragHandleOptions = {
        duration: 250,
        zIndex: 5,
        placement: 'left'
    };

    // v3 stopped exporting CharacterCountStorage; mirror the shape locally. Undefined while
    // the editor is still being built — every caller already guards with `?.`.
    get characterCount(): { characters: () => number; words: () => number } | undefined {
        return this.editor?.storage.characterCount;
    }

    get showCharData() {
        try {
            return JSON.parse(this.displayCountBar as string);
        } catch {
            return true;
        }
    }

    get readingTime() {
        // The constant used by Medium for words an adult can read per minute is 265
        // More Information here: https://help.medium.com/hc/en-us/articles/214991667-Read-time
        return Math.ceil(this.characterCount.words() / 265);
    }

    /**
     * Returns the charLimitExceeded error if it exists on the control.
     * Used in the template to display the error message.
     */
    get charLimitError(): { max: number; actual: number } | null {
        const ngControl = this.#injector.get(NgControl, null);

        return ngControl?.control?.errors?.['charLimitExceeded'] ?? null;
    }

    /**
     * Returns true if the editor should show error styling (red border).
     * Combines the external error state (from parent) with internal charLimit validation.
     */
    get hasError(): boolean {
        return this.hasFieldError || !!this.charLimitError;
    }

    /**
     * Returns true if the control has a required error and has been touched.
     * Used to display the required error message in the footer.
     */
    get requiredError(): boolean {
        const ngControl = this.#injector.get(NgControl, null);
        const control = ngControl?.control;

        return !!(control?.errors?.['required'] && control?.touched);
    }

    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    writeValue(content: JSONContent): void {
        this.value = content;
        if (!this.editor) {
            this.#pendingValue = content;

            return;
        }

        this.setEditorContent(content);
    }

    setDisabledState(isDisabled: boolean): void {
        this.disabled = isDisabled;
        if (this.editor) {
            this.editor.setEditable(!isDisabled);
        }
    }

    async loadCustomBlocks(urls: string[]): Promise<PromiseSettledResult<AnyExtension>[]> {
        return Promise.allSettled(urls.map(async (url) => import(/* webpackIgnore: true */ url)));
    }

    ngOnInit() {
        this.isAIPluginInstalled$ = this.dotAiService.checkPluginInstallation();
        tippy.setDefaultProps({ zIndex: 10 });
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
                    ],
                    editable: true
                });
                this.#knownEditorNodeNames.clear();
                Object.keys(this.editor.schema.nodes).forEach((nodeName) =>
                    this.#knownEditorNodeNames.add(nodeName)
                );

                this.dotMarketingConfigService.setProperty(
                    EDITOR_MARKETING_KEYS.SHOW_VIDEO_THUMBNAIL,
                    showVideoThumbnail
                );

                this.subscribeToEditorEvents();
            });
    }

    ngOnChanges(changes: SimpleChanges) {
        // Update DotConfig extension when languageId changes
        if (changes['languageId'] && this.editor && !changes['languageId'].firstChange) {
            const newLanguageId = this.contentlet?.languageId || this.languageId;
            this.editor.storage.dotConfig.lang = newLanguageId;
        }
    }

    ngOnDestroy() {
        if (this.editor) {
            this.editor.destroy();
        }

        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onBlockEditorChange(value: JSONContent): void {
        if (this.disabled) {
            return;
        }

        const restoredValue = {
            ...value,
            content: restoreUnknownBlockNodes(value.content)
        };

        // Eagerly include charCount/wordCount/readingTime in the doc attrs so the
        // API response always contains this metadata. Without this patch the attrs
        // would only arrive after the 250 ms debounce fired by the (keyup) handler.
        // `characterCount` is derived from `this.editor?.storage`, so it can be
        // undefined when the editor hasn't finished initializing (async ngOnInit).
        const charCount = this.characterCount?.characters?.() ?? 0;
        const updatedValue: JSONContent =
            charCount > 0
                ? {
                      ...restoredValue,
                      attrs: {
                          ...(restoredValue.attrs || {}),
                          charCount,
                          wordCount: this.characterCount?.words?.() ?? 0,
                          readingTime: this.readingTime
                      }
                  }
                : restoredValue;

        this.valueChange.emit(updatedValue);
        this.onChange?.(JSON.stringify(updatedValue));
        this.updateCharLimitValidity();
    }

    /**
     * Updates the form control validity based on charLimit.
     * When character count exceeds charLimit, sets charLimitExceeded error
     * so the form cannot be saved.
     *
     * @private
     * @memberof DotBlockEditorComponent
     */
    private updateCharLimitValidity(): void {
        const ngControl = this.#injector.get(NgControl, null);
        const control = ngControl?.control;
        if (!control) {
            return;
        }

        const limit = this.charLimit;
        if (!Number.isFinite(limit) || limit <= 0) {
            this.clearCharLimitError(control);

            return;
        }

        const count = this.characterCount?.characters?.() ?? 0;
        if (count > limit) {
            control.setErrors({
                ...(control.errors || {}),
                charLimitExceeded: { max: limit, actual: count }
            });
            control.markAsTouched();
        } else {
            this.clearCharLimitError(control);
        }
    }

    /**
     * Removes the charLimitExceeded error from the control while preserving other errors.
     *
     * @private
     * @param {AbstractControl} control - The form control to clear the error from
     * @memberof DotBlockEditorComponent
     */
    private clearCharLimitError(control: AbstractControl): void {
        const errors = control.errors;
        if (!errors || !('charLimitExceeded' in errors)) {
            return;
        }

        // Remove charLimitExceeded while preserving other errors
        const rest = Object.keys(errors)
            .filter((key) => key !== 'charLimitExceeded')
            .reduce((acc, key) => ({ ...acc, [key]: errors[key] }), {});

        control.setErrors(Object.keys(rest).length > 0 ? rest : null);
    }

    setAllowedBlocks(blocks: string) {
        const allowedBlocks = blocks ? blocks.replace(/ /g, '').split(',').filter(Boolean) : [];

        this.#allowedBlocks = [...this.#allowedBlocks, ...allowedBlocks];
    }

    /**
     * Subscribe to the editor events
     *
     * @private
     * @memberof DotBlockEditorComponent
     */
    private subscribeToEditorEvents() {
        this.editor.on('create', () => {
            // A CVA write can arrive before TipTap finishes booting; replay that buffered
            // value first so the initial document is wrapped/filtered against the real schema.
            this.setEditorContent(this.#pendingValue ?? this.value);
            this.#pendingValue = null;
            this.updateCharCount();
            // Validate char limit on initial load (e.g., existing content over limit)
            this.updateCharLimitValidity();
        });

        // Validate char limit on every update (typing, paste, etc.)
        this.editor.on('update', () => {
            this.updateCharLimitValidity();
        });

        // Mark control as touched when user leaves the editor (proper ControlValueAccessor pattern)
        this.editor.on('blur', () => {
            this.onTouched?.();
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
        const RemoteExtensionsSchema = structType({
            extensions: array(
                structType({
                    url: string(),
                    actions: optional(
                        array(
                            structType({
                                command: string(),
                                menuLabel: string(),
                                icon: string(),
                                name: optional(string())
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
        const moduleObj = customModules.reduce(this.parsedCustomModules, {});
        const loadedExtensions = Object.values(moduleObj) as AnyExtension[];
        const registeredExtensionNames = loadedExtensions
            .map((extension) => extension?.name)
            .filter((name): name is string => typeof name === 'string' && name.length > 0);

        warnOnUnmatchedRemoteBlockNames(data, registeredExtensionNames);

        // Only register the remote blocks this field actually allows. A remote block
        // deselected in Allowed Blocks is never added to the schema, so it cannot be
        // inserted (slash menu included) — while any existing content using it still
        // round-trips as a `dotUnsupportedBlock` placeholder, since an unregistered
        // node is unknown to `#knownEditorNodeNames`.
        return loadedExtensions.filter((extension) => this.#isRemoteBlockAllowed(extension?.name));
    }

    /**
     * Whether a remote block may be registered on this field.
     *
     * Unrestricted fields (`allowedBlocks.length <= 1`, i.e. paragraph-only) register
     * everything, matching `getAllowedCustomNodes`. On a restricted field the block's
     * declared `action.name` must appear in `allowedBlocks`.
     */
    #isRemoteBlockAllowed(extensionName: string | undefined): boolean {
        if (this.#allowedBlocks.length <= 1) {
            return true;
        }

        return typeof extensionName === 'string' && this.#allowedBlocks.includes(extensionName);
    }

    private getEditorNodes(): AnyExtension[] {
        // StarterKit v3 bundles Link and Underline, but this editor registers its own
        // Link.extend(...) and Underline in getEditorMarks(). Always disable StarterKit's
        // bundled copies (in BOTH branches) so we don't double-register them and trigger
        // "Duplicate extension names found: ['link', 'underline']".
        const baseConfig: Partial<StarterKitOptions> = { link: false, underline: false };

        // If you have more than one allow block (other than the paragraph),
        // we customize the starterkit.
        const starterkit =
            this.#allowedBlocks?.length > 1
                ? StarterKit.configure({ ...baseConfig, ...this.starterConfig() })
                : StarterKit.configure(baseConfig);

        const customNodes = this.getAllowedCustomNodes();

        return [starterkit, UnsupportedBlockNode, ...customNodes];
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
            .filter((heading) => this.#allowedBlocks?.includes(heading))
            .map((heading) => +heading.slice(-1) as Level);

        const starterKit = staterKitOptions
            .filter((option) => !this.#allowedBlocks?.includes(option))
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
        if (this.#allowedBlocks.length <= 1) {
            return [...this._customNodes.values()];
        }

        for (const block of this.#allowedBlocks) {
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
                lang: this.contentlet?.languageId || this.languageId,
                allowedContentTypes: this.allowedContentTypes,
                allowedBlocks: this.#allowedBlocks,
                contentletIdentifier: this.contentletIdentifier
            }),
            DotComands,
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
            BubbleFormExtension(this.viewContainerRef),
            DotFloatingButton(this.#injector, this.viewContainerRef),
            BubbleAssetFormExtension(this.viewContainerRef),
            FreezeScroll,
            CharacterCount,
            AssetUploader(this.#injector, this.viewContainerRef),
            IndentExtension,
            Placeholder.configure({
                emptyEditorClass: 'is-editor-empty',
                emptyNodeClass: 'is-empty',
                placeholder: ({ node }) => {
                    if (node.type.name === 'bulletList' || node.type.name === 'orderedList') {
                        return this.#dotMessageService.get('block-editor.placeholder.list');
                    }

                    if (node.type.name === 'heading') {
                        const level = node.attrs['level'] ?? '';

                        return this.#dotMessageService.get(
                            'block-editor.placeholder.heading',
                            level
                        );
                    }

                    if (node.type.name === 'codeBlock') {
                        return this.#dotMessageService.get('block-editor.placeholder.code');
                    }

                    if (node.type.name === 'blockquote') {
                        return this.#dotMessageService.get('block-editor.placeholder.quote');
                    }

                    if (node.type.name === 'table' || node.type.name === 'gridBlock') {
                        return '';
                    }

                    return this.#dotMessageService.get('block-editor.placeholder.paragraph');
                }
            }),
            ...DotCMSTableExtensions,
            DotTableCellContextMenu(this.viewContainerRef),
            createGridColumn(this.#allowedBlocks.length > 1 ? this.#allowedBlocks : [])
        ];

        if (isAIPluginInstalled) {
            extensions.push(
                AIContentPromptExtension(this.viewContainerRef),
                AIImagePromptExtension(this.#dialogService, this.#dotMessageService)
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
            // Extends the default Link mark with accessibility attributes (title, aria-label)
            // and rel. These are persisted in the TipTap JSON and rendered in the editor DOM.
            Link.extend({
                addAttributes() {
                    return {
                        ...this.parent?.(),
                        title: {
                            default: null,
                            parseHTML: (el) => el.getAttribute('title'),
                            renderHTML: (attrs) => (attrs['title'] ? { title: attrs['title'] } : {})
                        },
                        'aria-label': {
                            default: null,
                            parseHTML: (el) => el.getAttribute('aria-label'),
                            renderHTML: (attrs) =>
                                attrs['aria-label'] ? { 'aria-label': attrs['aria-label'] } : {}
                        },
                        rel: {
                            default: null,
                            parseHTML: (el) => el.getAttribute('rel'),
                            renderHTML: (attrs) => (attrs['rel'] ? { rel: attrs['rel'] } : {})
                        }
                    };
                }
            }).configure({ autolink: false, openOnClick: false })
        ];
    }

    private setEditorJSONContent(content: Content) {
        if (!this.editor || typeof content === 'string') {
            this.content = content;

            return;
        }

        const preservedContent = Array.isArray(content)
            ? preserveUnknownBlockNodes(content, this.#knownEditorNodeNames)
            : {
                  ...content,
                  content: preserveUnknownBlockNodes(content.content, this.#knownEditorNodeNames)
              };

        this.content =
            this.#allowedBlocks?.length > 1
                ? removeInvalidNodes(
                      preservedContent,
                      this.#allowedBlocks,
                      this.#getDeclaredRemoteBlockNames()
                  )
                : preservedContent;
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

    /**
     * Declared remote block names that this field also allows.
     *
     * Passing these to `removeInvalidNodes` keeps a selected remote block's content intact
     * even when its bundle fails to load. Names the field does not allow are deliberately
     * excluded so deselecting a remote block actually restricts it — the node is then
     * wrapped as a `dotUnsupportedBlock` placeholder (never stripped, since that
     * placeholder is always allowed), so restricting still never destroys content.
     */
    #getDeclaredRemoteBlockNames(): string[] {
        const declaredNames = getDeclaredRemoteBlockNames(this.getParsedCustomBlocks());

        if (this.#allowedBlocks.length <= 1) {
            return declaredNames;
        }

        return declaredNames.filter((name) => this.#allowedBlocks.includes(name));
    }
}
