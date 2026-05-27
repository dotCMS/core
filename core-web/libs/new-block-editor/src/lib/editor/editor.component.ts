import { TiptapEditorDirective } from 'ngx-tiptap';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Injector,
    OnDestroy,
    booleanAttribute,
    computed,
    effect,
    forwardRef,
    inject,
    input,
    numberAttribute,
    output,
    signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';

import { type AnyExtension, Editor, type JSONContent } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { AssetByUrlPopoverComponent } from './components/asset-by-url-popover/asset-by-url-popover.component';
import { EmojiPickerComponent } from './components/emoji-picker/emoji-picker.component';
import { ImagePropertiesPopoverComponent } from './components/image-popover/image-popover.component';
import { LinkPopoverComponent } from './components/link-popover/link-popover.component';
import { SlashMenuComponent } from './components/slash-menu/slash-menu.component';
import { SlashMenuService } from './components/slash-menu/slash-menu.service';
import { TableColumnPopoverComponent } from './components/table-popover/table-column-popover.component';
import { TablePopoverComponent } from './components/table-popover/table-popover.component';
import { TableRowPopoverComponent } from './components/table-popover/table-row-popover.component';
import { TableSelectionPopoverComponent } from './components/table-popover/table-selection-popover.component';
import { TablePropertiesPopoverComponent } from './components/table-properties-popover/table-properties-popover.component';
import { EditorToolbarStore } from './components/toolbar/editor-toolbar.store';
import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { syncCharacterStatsFromEditor } from './editor-character-stats';
import { handleEditorProseMirrorClick } from './editor-chrome-click';
import { handleMediaDrop } from './editor.utils';
import { createEditorExtensions } from './extensions/editor-extensions';
import { type ContentletEditEvent } from './extensions/nodes/contentlet/contentlet.extension';
import { SELECTION_PRESERVE_KEY } from './extensions/selection-preserve.extension';
import { ContentletEditUrlService } from './services/contentlet-edit-url.service';
import { DotUploadService } from './services/dot-upload.service';
import { EditorModalService } from './services/editor-modal.service';
import { EditorPopoverService } from './services/editor-popover.service';
import { EditorStore } from './store/editor.store';
import { loadRemoteExtensions, parseCustomBlocksField } from './utils/remote-extensions.loader';

/** Stringifies the editor document for form output (plain ProseMirror JSON, no extra attrs). */
function editorDocumentJsonText(editor: Editor): string {
    return JSON.stringify(editor.getJSON());
}

/**
 * Reads the `allowedBlocks` field variable as a list of block names.
 * Returns undefined for "no restriction" so {@link createEditorExtensions} short-circuits
 * its `!allowedBlocks` branch and loads every extension.
 */
function parseAllowedBlocks(field: DotCMSContentTypeField | undefined): string[] | undefined {
    const variable = field?.fieldVariables?.find((v) => v.key === 'allowedBlocks');
    const blocks = variable?.value
        ?.split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    return blocks && blocks.length > 0 ? blocks : undefined;
}

/**
 * Reads the legacy `contentTypes` field variable verbatim. The store normalises whitespace
 * before storing, so we hand the raw string straight through. Empty string ⇒ no restriction.
 */
function parseAllowedContentTypes(field: DotCMSContentTypeField | undefined): string {
    return field?.fieldVariables?.find((v) => v.key === 'contentTypes')?.value ?? '';
}

/** True when {@link parsed} represents the same document already in {@link editor}. */
function editorContentMatchesParsed(editor: Editor, parsed: string | JSONContent): boolean {
    const currentJson = editorDocumentJsonText(editor);
    if (typeof parsed === 'string') {
        const trimmed = parsed.trimStart();
        if (trimmed.startsWith('{')) {
            try {
                return JSON.stringify(JSON.parse(parsed)) === currentJson;
            } catch {
                return false;
            }
        }
        return parsed === editor.getHTML();
    }
    return JSON.stringify(parsed) === currentJson;
}

/**
 * Normalizes incoming editor content to either an HTML string or a JSONContent object.
 * dotCMS stores block editor fields as ProseMirror JSON (object or stringified).
 * TipTap's setContent accepts both formats natively.
 */
function normalizeEditorContent(
    content: string | JSONContent | null | undefined
): string | JSONContent {
    if (!content) return '';
    if (typeof content !== 'string') return content;
    const trimmed = content.trimStart();
    if (trimmed.startsWith('{')) {
        try {
            return JSON.parse(content) as JSONContent;
        } catch {
            // fall through to HTML
        }
    }
    return content;
}

/**
 * DotCMS block editor shell: TipTap surface, toolbar, slash menu, floating dialogs
 * (table, image, video, link, emoji), media drag-and-drop, optional fullscreen overlay,
 * live document stats, and Angular {@link ControlValueAccessor} for two-way JSON-text binding
 * ({@link Editor.getJSON} stringified, same storage shape as the legacy block editor).
 *
 * Registers {@link EditorStore} and {@link SlashMenuService} at component scope so each
 * editor instance has isolated menu and shared UI state.
 */
@Component({
    selector: 'dot-block-editor',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrls: ['./editor.component.css'],
    providers: [
        EditorStore,
        SlashMenuService,
        EditorPopoverService,
        EditorModalService,
        EditorToolbarStore,
        ContentletEditUrlService,
        // Component-scoped DialogService so each editor instance has its own PrimeNG
        // dynamic-dialog factory; prevents the AI image prompt opened from one editor
        // from accidentally being closed by another editor on the same page.
        DialogService,
        // Component-scoped ConfirmationService pairs 1:1 with the local <p-confirmdialog>
        // below — keeps two editors on the same page from sharing confirmation state.
        ConfirmationService,
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotCMSEditorComponent),
            multi: true
        }
    ],
    imports: [
        TiptapEditorDirective,
        SlashMenuComponent,
        EmojiPickerComponent,
        TablePopoverComponent,
        TableColumnPopoverComponent,
        TableRowPopoverComponent,
        TableSelectionPopoverComponent,
        TablePropertiesPopoverComponent,
        ImagePropertiesPopoverComponent,
        LinkPopoverComponent,
        AssetByUrlPopoverComponent,
        ToolbarComponent,
        ConfirmDialog,
        DotMessagePipe
    ],
    template: `
        <div [class]="wrapperClass()">
            <div [class]="panelClass()">
                @if (editor(); as ed) {
                    <dot-toolbar
                        [editor]="ed"
                        [isFullscreen]="isFullscreen()"
                        (fullscreenToggle)="toggleFullscreen()"
                        (contentletEdit)="contentletEdit.emit($event)" />
                    <div
                        class="relative overflow-y-auto overscroll-contain editor-scroll-container"
                        [class.editor-scroll-container--locked]="anyOverlayOpen()"
                        [style]="
                            isFullscreen()
                                ? 'flex: 1; min-height: 0;'
                                : 'height: 500px; resize: vertical; min-height: 200px;'
                        ">
                        <div
                            tiptap
                            [editor]="ed"
                            class="prose max-w-none"
                            role="textbox"
                            aria-multiline="true"
                            [attr.aria-label]="'dot.block.editor.editor.aria-label' | dm"
                            aria-haspopup="listbox"
                            aria-controls="slash-command-menu"
                            [attr.aria-expanded]="menuService.isOpen()"
                            [attr.aria-activedescendant]="menuService.activeOptionId()"
                            (click)="onClick($event)"></div>
                    </div>

                    <div
                        class="flex items-center gap-4 border-t border-gray-100 px-8 py-3 text-sm text-gray-500"
                        aria-live="polite"
                        [attr.aria-label]="'dot.block.editor.editor.stats.aria-label' | dm">
                        <span>
                            {{ wordCount() }}
                            {{
                                (wordCount() === 1
                                    ? 'dot.block.editor.editor.stats.word'
                                    : 'dot.block.editor.editor.stats.words'
                                ) | dm
                            }}
                        </span>
                        <span>
                            {{ charCount() }}
                            {{
                                (charCount() === 1
                                    ? 'dot.block.editor.editor.stats.character'
                                    : 'dot.block.editor.editor.stats.characters'
                                ) | dm
                            }}
                        </span>
                        <span>{{ readingTimeLabel() }}</span>
                    </div>

                    <dot-slash-menu />
                    <dot-emoji-picker [editor]="ed" />
                    <dot-table-popover [editor]="ed" />
                    <dot-table-column-popover [editor]="ed" />
                    <dot-table-row-popover [editor]="ed" />
                    <dot-table-selection-popover [editor]="ed" />
                    <dot-table-properties-popover [editor]="ed" />
                    <dot-image-popover [editor]="ed" />
                    <dot-link-popover [editor]="ed" />
                    <dot-asset-by-url-popover [editor]="ed" />
                    <p-confirmdialog [style]="{ width: '32rem', maxWidth: '90vw' }" />
                }
            </div>
        </div>
    `
})
export class DotCMSEditorComponent implements OnDestroy, ControlValueAccessor {
    /** Slash menu state; used by the template for ARIA on the ProseMirror surface. */
    protected readonly menuService = inject(SlashMenuService);

    /** Field-scoped UI state (e.g. allowed blocks, language for API calls). */
    protected readonly store = inject(EditorStore);

    /** Opens/closes caret-anchored popovers and supplies payloads (e.g. link edit context). */
    private readonly popovers = inject(EditorPopoverService);

    /** Uploads user-dropped image and video files to dotCMS. */
    private readonly dotUpload = inject(DotUploadService);

    /** Document root for fullscreen scroll lock and global key listeners. */
    private readonly document = inject(DOCUMENT);

    /** Passed into TipTap extensions that mount Angular node views (e.g. contentlet). */
    private readonly injector = inject(Injector);

    /** Used by extensions (gutter, upload placeholder, Tiptap Placeholder) for i18n. */
    private readonly dotMessageService = inject(DotMessageService);

    /**
     * Initial editor content: JSON text (ProseMirror / TipTap doc) or HTML.
     * Required for Web Component usage where Angular's ControlValueAccessor is not available
     * and content must be set via attribute or property binding from outside Angular.
     * Inside Angular, prefer binding through ngModel or a reactive form control instead.
     */
    readonly value = input<string>('');

    /**
     * DotCMS content type field definition.
     * Drives editor configuration via fieldVariables: allowed blocks, allowed content types,
     * character limit, custom styles, count bar visibility, and custom remote extensions.
     */
    readonly field = input<DotCMSContentTypeField | undefined>(undefined);

    /**
     * The DotCMS contentlet currently being edited.
     * Used to resolve the active language ID and contentlet identifier
     * passed to the editor config extension for API-scoped queries.
     * When provided, its languageId takes precedence over the `languageId` input.
     */
    readonly contentlet = input<DotCMSContentlet | undefined>(undefined);

    /**
     * Language ID used for dotCMS API queries (content type search, contentlet insertion).
     * Falls back to this value when no `contentlet` input is provided.
     * Defaults to language 1 (English). Coerced via {@link numberAttribute} so the JSP
     * Web-Component host can set it as a string property without a NaN downstream.
     */
    readonly languageId = input(1, { transform: numberAttribute });

    /**
     * Identifier of the contentlet whose Story Block field is being edited.
     * Set by the JSP Web-Component host; redundant with `contentlet()?.identifier`
     * for Angular consumers. Reserved for asset-scoped APIs that need it directly.
     */
    readonly contentletIdentifier = input<string | undefined>(undefined);

    /**
     * When true, applies error styling to the editor wrapper.
     * Set by the parent when the field's form control has validation errors
     * originating outside the editor (e.g. required, custom validators).
     */
    readonly hasError = input(false, { transform: booleanAttribute });

    /**
     * Emits the editor document as a ProseMirror JSON object on every change.
     * The JSP Web-Component host stringifies this value and writes it to a hidden form input;
     * Angular consumers should bind through ngModel/formControl instead, which receives the
     * stringified shape via {@link ControlValueAccessor}.
     */
    readonly valueChange = output<JSONContent>();

    /**
     * Emits when the user chooses to edit an embedded DotCMS contentlet from the document.
     */
    readonly contentletEdit = output<ContentletEditEvent>();

    /** Current word count shown in the footer stats bar. */
    readonly wordCount = signal(0);

    /** Current character count shown in the footer stats bar. */
    readonly charCount = signal(0);

    /** Estimated whole-minute reading time for the footer stats bar. */
    readonly readingTime = signal(0);

    /** Localized "{n} min read" label. */
    protected readonly readingTimeLabel = computed(() =>
        this.dotMessageService.get(
            'dot.block.editor.editor.stats.read-time',
            String(this.readingTime())
        )
    );

    /** Signals updated by {@link syncCharacterStatsFromEditor} on create and update. */
    private readonly stats = {
        wordCount: this.wordCount,
        charCount: this.charCount,
        readingTime: this.readingTime
    };

    /**
     * Shared TipTap {@link Editor} for the host template and all child editor components.
     * Held as a signal so that — for fields that opt into the `customBlocks` field
     * variable — we can defer construction until the remote ES-module URLs resolve.
     * The template guards children with `@if (editor(); as ed)` so they only mount
     * once the editor is ready. Fields without `customBlocks` initialise synchronously
     * in the constructor and observe no behavioural change.
     */
    readonly editor = signal<Editor | null>(null);

    /**
     * Buffers content sent through {@link writeValue} before the editor mounts on the
     * slow path. Drained once {@link buildEditor} returns. Without this, Angular forms
     * that call `writeValue` during component construction would silently drop the
     * initial value when the editor finishes loading milliseconds later.
     */
    private pendingValue: string | JSONContent | null = null;

    /**
     * Buffers a {@link setDisabledState} call that arrives before the editor exists.
     * Applied right after {@link buildEditor} returns.
     */
    private pendingDisabled: boolean | null = null;

    /**
     * Constructs the underlying TipTap editor with dotCMS extensions, drop handling,
     * and stats sync. Returns the new instance instead of assigning it, so the caller
     * can pin it inside the {@link editor} signal in a single, atomic update.
     *
     * @param remoteExtensions Customer-supplied TipTap extensions resolved from the
     * `customBlocks` field variable. Empty for the fast path.
     */
    private buildEditor(remoteExtensions: AnyExtension[]): Editor {
        const editor: Editor = new Editor({
            onCreate: ({ editor }) => syncCharacterStatsFromEditor(editor, this.stats),
            onUpdate: ({ editor }) => {
                syncCharacterStatsFromEditor(editor, this.stats);
                const json = this.withDocStats(editor.getJSON());
                this.onChange(JSON.stringify(json));
                this.valueChange.emit(json);
            },
            onBlur: () => {
                this.onTouched();
            },
            editorProps: {
                handleDrop: (view, event, slice, moved) =>
                    handleMediaDrop(
                        editor,
                        view,
                        event as DragEvent,
                        slice,
                        moved,
                        (file) => this.dotUpload.uploadImage(file),
                        (file) => this.dotUpload.uploadVideo(file)
                    )
            },
            extensions: createEditorExtensions(
                this.menuService,
                parseAllowedBlocks(this.field()),
                this.injector,
                this.dotMessageService,
                remoteExtensions
            ),
            content: ''
        });
        return editor;
    }

    /**
     * Pins a freshly built editor inside the {@link editor} signal and drains any
     * value / disabled state that arrived through {@link ControlValueAccessor} while
     * the editor was still mounting on the slow path.
     */
    private commitEditor(editor: Editor): void {
        this.editor.set(editor);

        if (this.pendingDisabled !== null) {
            editor.setEditable(!this.pendingDisabled);
            this.pendingDisabled = null;
        }

        if (this.pendingValue !== null) {
            const parsed = normalizeEditorContent(this.pendingValue);
            if (!editorContentMatchesParsed(editor, parsed)) {
                editor.commands.setContent(parsed, { emitUpdate: false });
            }
            this.pendingValue = null;
        }
    }

    /**
     * Public input that seeds the fullscreen overlay state from the JSP / Angular host.
     * Aliased to `isFullscreen` so the custom element exposes `el.isFullscreen` as the property.
     * After seeding, the in-toolbar expand button and the Escape handler mutate {@link isFullscreen}
     * directly via {@link _isFullscreen}; subsequent input changes are merged via {@link constructor}.
     */
    readonly isFullscreenInitial = input(false, {
        alias: 'isFullscreen',
        transform: booleanAttribute
    });

    /** Internal mutable source of truth for the fullscreen overlay. Set by toolbar / Escape. */
    private readonly _isFullscreen = signal(false);

    /**
     * Read-only fullscreen state for the template and child components.
     * Reflects the union of the input seed and any in-editor toggles.
     */
    readonly isFullscreen = this._isFullscreen.asReadonly();

    /**
     * True while any managed dialog or the slash menu is open. Drives both
     * selection-preservation meta (so the user still sees their editing range)
     * and the editor's scroll-lock — freezing the scroll container while an
     * overlay is open keeps the cursor anchored beneath its popover.
     */
    protected readonly anyOverlayOpen = computed(
        () => this.popovers.activePopover() !== null || this.menuService.isOpen()
    );

    /** Toggles {@link isFullscreen} from the toolbar control. */
    protected toggleFullscreen(): void {
        this._isFullscreen.update((v) => !v);
    }

    /** Backdrop/layout classes for the outer wrapper (fullscreen dimmer vs inline). */
    protected readonly wrapperClass = computed(() =>
        this.isFullscreen()
            ? 'fixed inset-0 z-[9998] flex items-center justify-center bg-black/50'
            : ''
    );

    /** Inner panel sizing and chrome classes (fullscreen vs default card layout). */
    protected readonly panelClass = computed(() =>
        this.isFullscreen()
            ? 'relative flex flex-col w-[90vw] max-w-7xl h-[90vh] rounded-lg border border-gray-200 bg-white overflow-hidden'
            : 'relative rounded-lg border border-gray-200'
    );

    /**
     * Subscribes inputs to {@link EditorStore} and the TipTap document, applies selection
     * preservation while overlays are open, and locks document scroll + Escape-to-exit while
     * {@link isFullscreen} is active.
     */
    constructor() {
        // Seed fullscreen state from the host input. Subsequent toolbar/Escape mutations live
        // on the internal signal; if the host pushes a new input value later, it wins.
        effect(() => {
            this._isFullscreen.set(this.isFullscreenInitial());
        });

        // Sync allowedBlocks (from field.fieldVariables) → store
        effect(() => {
            this.store.setAllowedBlocks(parseAllowedBlocks(this.field()) ?? []);
        });

        // Sync allowedContentTypes (from field.fieldVariables.contentTypes) → store.
        // Forwarded to the slash sub-menu's content-type fetch so customers can
        // restrict which content types are embeddable in this Story Block field.
        effect(() => {
            this.store.setAllowedContentTypes(parseAllowedContentTypes(this.field()));
        });

        // Sync languageId input → store (contentlet.languageId takes precedence).
        // Defensive coerce: numberAttribute returns NaN for malformed strings; fall back to 1
        // so downstream API queries don't fail with `?languageId=NaN`.
        effect(() => {
            const fromContentlet = this.contentlet()?.languageId;
            const fromInput = this.languageId();
            const id = fromContentlet ?? (Number.isFinite(fromInput) ? fromInput : 1);
            this.store.setLanguageId(id);
        });

        // Sync value input → editor (for web component / non-CVA usage).
        // Guard: skip when value is empty to avoid overriding CVA-set content on init;
        // skip when unchanged so two-way [value] + (valueChange) does not reset the cursor.
        // Also tracks `editor()` so the effect re-fires once the slow-path editor mounts.
        effect(() => {
            const v = this.value();
            if (!v) return;
            const ed = this.editor();
            if (!ed) return;
            const parsed = normalizeEditorContent(v);
            if (editorContentMatchesParsed(ed, parsed)) return;
            ed.commands.setContent(parsed, { emitUpdate: false });
        });

        // Preserve selection highlight while any popover or slash menu is open
        effect(() => {
            const open = this.anyOverlayOpen();
            const ed = this.editor();
            if (!ed) return;
            ed.view.dispatch(ed.state.tr.setMeta(SELECTION_PRESERVE_KEY, { active: open }));
        });

        // Fullscreen: body scroll lock + Escape exits when no popover/menu is open
        effect((onCleanup) => {
            if (!this.isFullscreen()) return;
            this.document.body.style.overflow = 'hidden';

            const onKey = (e: KeyboardEvent) => {
                if (e.key !== 'Escape') return;
                const overlayOpen =
                    this.popovers.activePopover() !== null || this.menuService.isOpen();
                if (!overlayOpen) this._isFullscreen.set(false);
            };

            this.document.addEventListener('keydown', onKey);
            onCleanup(() => {
                this.document.removeEventListener('keydown', onKey);
                this.document.body.style.overflow = '';
            });
        });

        // Editor init: fast path (no customBlocks) is synchronous so existing tests
        // and consumers see no behaviour change. Slow path (customBlocks set) defers
        // construction until the remote ES-module URLs resolve — TipTap's schema is
        // frozen at construction time, so adding extensions later is impossible
        // without destroying the editor and losing ProseMirror state.
        const parsedCustomBlocks = parseCustomBlocksField(this.field());
        if (parsedCustomBlocks.extensions.length === 0) {
            this.commitEditor(this.buildEditor([]));
        } else {
            void loadRemoteExtensions(parsedCustomBlocks).then(({ extensions, actions }) => {
                this.menuService.setRemoteBlockItems(actions);
                this.commitEditor(this.buildEditor(extensions));
            });
        }
    }

    /**
     * Delegates ProseMirror clicks to shared chrome logic (e.g. opening dialogs for nodes).
     *
     * @param event - Native click from the TipTap host element.
     */
    onClick(event: MouseEvent): void {
        const ed = this.editor();
        if (!ed) return;
        handleEditorProseMirrorClick(event, ed, this.popovers);
    }

    /** Restores body scroll and destroys the TipTap instance. */
    ngOnDestroy(): void {
        this.document.body.style.overflow = '';
        this.editor()?.destroy();
    }

    /**
     * Stamps the document's character / word / reading-time stats onto the JSON's root `attrs`
     * so the emitted shape matches the legacy block editor. The legacy editor wrote these on
     * every change; downstream consumers (server-side reporting, headless renderers) read them
     * straight off the doc attrs. Skip the stamp when the editor is empty so a brand-new doc
     * doesn't ship inflated zeros.
     */
    private withDocStats(json: JSONContent): JSONContent {
        const chars = this.charCount();
        if (chars <= 0) return json;
        return {
            ...json,
            attrs: {
                ...(json.attrs ?? {}),
                charCount: chars,
                wordCount: this.wordCount(),
                readingTime: this.readingTime()
            }
        };
    }

    /** Bound in {@link registerOnChange}; forwards stringified {@link Editor.getJSON} to the form control. */
    private onChange: (value: string) => void = (_value: string) => {
        // Implementation provided by registerOnChange
    };

    /** Bound in {@link registerOnTouched}; marks the control touched on editor blur. */
    private onTouched: () => void = () => {
        // Implementation provided by registerOnTouched
    };

    /** @inheritdoc */
    writeValue(content: string | null): void {
        const ed = this.editor();
        if (!ed) {
            // Slow path: buffer until commitEditor drains it.
            this.pendingValue = content ?? '';
            return;
        }
        const parsed = normalizeEditorContent(content);
        if (editorContentMatchesParsed(ed, parsed)) return;
        ed.commands.setContent(parsed, { emitUpdate: false });
    }

    /** @inheritdoc */
    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    /** @inheritdoc */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /** @inheritdoc */
    setDisabledState(isDisabled: boolean): void {
        const ed = this.editor();
        if (!ed) {
            this.pendingDisabled = isDisabled;
            return;
        }
        ed.setEditable(!isDisabled);
    }
}
