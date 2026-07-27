import { fromEvent, Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    input,
    output,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { filter, take, takeUntil } from 'rxjs/operators';

import { DotSeoMetaTagsService, DotSeoMetaTagsUtilService } from '@dotcms/data-access';
import { SafeUrlPipe } from '@dotcms/ui';

import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { addEditorPageScript } from '../../../utils/ema-legacy-script-injection';

/**
 * Renders the UVE (Universal Visual Editor) page preview inside an iframe.
 *
 * Handles both traditional (VTL) and headless page types: injects rendered content,
 * SEO meta tags, inline-edit scripts, and filters click events for internal navigation
 * and inline editing targets.
 */
@Component({
    selector: 'dot-uve-iframe',
    standalone: true,
    templateUrl: './dot-uve-iframe.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [SafeUrlPipe],
    host: { class: 'block relative w-full h-full' }
})
export class DotUveIframeComponent {
    /**
     * Reference to the iframe element.
     * @type {ElementRef<HTMLIFrameElement>}
     */
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    /** URL to load in the iframe. */
    src = input.required<string>();
    /** Accessible title for the iframe. */
    title = input.required<string>();
    /** CSS pointer-events value for the iframe overlay. */
    pointerEvents = input.required<string | null>();
    /** Opacity of the iframe overlay (0–1). */
    opacity = input.required<number | null>();
    /** Host origin for postMessage communication. */
    host = input<string>('*');

    /** Emitted when the iframe has finished loading. */
    load = output<void>();
    /** Emitted when a click targets an internal link or inline-edit element. */
    internalNav = output<MouseEvent>();
    /** Emitted when a click targets an inline-edit element. */
    inlineEditing = output<MouseEvent>();

    protected readonly uveStore = inject(UVEStore);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * Emits on every iframe load to cancel the previous click listener subscription.
     */
    private readonly iframeClickListener$ = new Subject<void>();

    /**
     * Tracks the last content written into the iframe, keyed by `src + content`.
     * Prevents destructive re-writes when the reactive effect, the (load) handler,
     * and the synthetic load fired by doc.close() all converge on insertPageContent
     * for the same render — which re-executes customer top-level const/let and throws
     * "Identifier '…' has already been declared".
     */
    private lastWrittenKey: string | null = null;

    /**
     * Rendered HTML for traditional pages.
     * @type {Signal<string>}
     */
    readonly $pageRender = this.uveStore.$pageRender;
    /**
     * Whether inline editing is enabled in the editor.
     * @type {Signal<boolean>}
     */
    readonly $enableInlineEdit = this.uveStore.editorEnableInlineEdit;
    /**
     * Whether the legacy UVE script injection is enabled via feature flag.
     * @type {Signal<boolean>}
     */
    readonly $isEmaLegacyScriptInjectionEnabled = this.uveStore.$isEmaLegacyScriptInjectionEnabled;
    /**
     * Effect that injects rendered content into traditional pages when ready.
     * @type {EffectRef}
     */
    readonly $isTraditionalPageEffect = effect(() => {
        const isTraditional = this.uveStore.pageType() === PageType.TRADITIONAL;
        const pageRender = this.$pageRender();
        const enableInlineEdit = this.$enableInlineEdit();

        if (isTraditional && pageRender && this.iframe?.nativeElement?.contentDocument) {
            this.insertPageContent(pageRender, enableInlineEdit);
        }
    });

    /**
     * The content window of the iframe, or null if not available.
     * @returns {Window | null}
     */
    get contentWindow(): Window | null {
        return this.iframe?.nativeElement?.contentWindow || null;
    }

    /**
     * The underlying iframe DOM element, or null if not available.
     * @returns {HTMLIFrameElement | null}
     */
    get iframeElement(): HTMLIFrameElement | null {
        return this.iframe?.nativeElement || null;
    }

    /**
     * Handles the iframe load event.
     *
     * For headless pages, emits load without rewriting the document. For traditional
     * pages, injects content and SEO data before emitting load. Local iframe height
     * tracking is started for any locally accessible iframe regardless of page type.
     * @returns {void}
     */
    onIframeLoad(): void {
        if (this.uveStore.pageType() !== PageType.HEADLESS) {
            this.insertPageContent(this.$pageRender(), this.$enableInlineEdit());
            this.setSeoData();
        }

        this.load.emit();
    }

    /**
     * Injects rendered HTML into the iframe document and wires up inline scripts.
     * @param {string} pageRender - Rendered HTML to inject.
     * @param {boolean} enableInlineEdit - Whether to enable inline edit scripts.
     * @returns {void}
     */
    private insertPageContent(pageRender: string, enableInlineEdit: boolean): void {
        const iframeElement = this.iframe?.nativeElement;

        if (!iframeElement) {
            return;
        }

        const content = this.$isEmaLegacyScriptInjectionEnabled()
            ? addEditorPageScript(pageRender)
            : pageRender;

        const writeKey = `${this.src()}::${content}`;

        if (writeKey !== this.lastWrittenKey) {
            this.lastWrittenKey = writeKey;
            // srcdoc navigates the iframe to a fresh browsing context on every
            // unique render, clearing the window's global lexical scope.
            // document.open()/write()/close() reuses the same window object, so
            // top-level let/const from any prior render remain in scope and throw
            // "Identifier '…' has already been declared" when the same scripts
            // run again — even across legitimate re-renders (e.g. preview → edit
            // mode returns different server-rendered HTML that still contains the
            // same let/const declarations).
            iframeElement.srcdoc = content;
        }

        this.handleInlineScripts(enableInlineEdit);
    }

    /**
     * Subscribes to filtered click events and injects or removes inline-edit scripts.
     * @param {boolean} enableInlineEdit - Whether to inject inline-edit scripts.
     * @returns {void}
     */
    private handleInlineScripts(enableInlineEdit: boolean): void {
        const win = this.contentWindow;

        if (!win) {
            return;
        }

        this.iframeClickListener$.next();

        fromEvent<MouseEvent>(win, 'click')
            .pipe(
                filter((e) => {
                    const target = e.target as HTMLElement;
                    const linkElement = target.closest('a');
                    const href = linkElement?.getAttribute('href');

                    // Hash-only anchors (#section) are same-page scrolls — let
                    // the browser handle them. Skip both internalNav and
                    // inlineEditing emits even when the anchor is nested
                    // inside an editable [data-mode] region.
                    if (href?.startsWith('#')) {
                        return false;
                    }

                    const hasLink = !!href;
                    const hasInlineEditTarget =
                        !!target.closest('[data-mode]') || !!target.dataset?.mode;
                    return hasLink || hasInlineEditTarget;
                }),
                takeUntil(this.iframeClickListener$),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe((e) => {
                this.internalNav.emit(e);
                this.inlineEditing.emit(e);
            });

        if (enableInlineEdit) {
            this.inlineEditingService.injectInlineEdit(this.iframe);
        } else {
            this.inlineEditingService.removeInlineEdit(this.iframe);
        }
    }

    /**
     * Fetches SEO meta tags from the iframe document and updates the store.
     * @returns {void}
     */
    private setSeoData(): void {
        const iframeElement = this.iframe?.nativeElement;

        if (!iframeElement) {
            return;
        }

        const doc = iframeElement.contentDocument;

        if (!doc) {
            return;
        }

        this.dotSeoMetaTagsService
            .getMetaTagsResults(doc)
            .pipe(take(1))
            .subscribe((results) => {
                const ogTags = this.dotSeoMetaTagsUtilService.getMetaTags(doc);
                this.uveStore.setSeoData({ ogTags, ogTagsResults: results });
            });
    }
}
