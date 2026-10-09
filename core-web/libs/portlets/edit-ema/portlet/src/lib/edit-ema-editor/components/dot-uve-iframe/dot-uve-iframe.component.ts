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
import { scrollIframeToFragment } from '../../../utils';
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
     * Tracks the last content + src reference written into the iframe. Prevents
     * destructive re-writes when the reactive effect, the (load) handler, and the
     * synthetic load fired by doc.close() all converge on insertPageContent for
     * the same render — which re-executes customer top-level const/let and throws
     * "Identifier '…' has already been declared".
     *
     * `src` must be compared by reference, not by its stringified value: for
     * traditional pages `$iframeURL` (withEditor.ts) deliberately returns a new
     * `String('')` object on every load — including a re-navigation to the same
     * page with byte-identical rendered HTML — to force the native iframe `src`
     * to reset the browsing context. That reset silently discards whatever
     * `srcdoc` previously held. Comparing `src` by its stringified value (always
     * `''`) hid this: content-only dedup skipped rewriting `srcdoc` into the
     * freshly reset, now-empty document, leaving the canvas permanently blank.
     * Comparing the `src` signal's own reference re-triggers the write exactly
     * when (and only when) that native reset has actually happened.
     */
    private lastWrittenContent: string | null = null;
    private lastWrittenSrcRef: unknown = null;

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

        const srcRef = this.src();

        if (content !== this.lastWrittenContent || srcRef !== this.lastWrittenSrcRef) {
            this.lastWrittenContent = content;
            this.lastWrittenSrcRef = srcRef;
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

        // Bound on the capture phase so this fires before ANY bubble-phase
        // listener the page's own content registered (e.g. a docs-theme's
        // client-side router delegating clicks on `document`), and before any
        // capture-phase listener on a node closer to the click target. Capture
        // traverses window → document → … → target, so a listener on `window`
        // is always first; bubble traverses the reverse, so a page-owned
        // listener on `document` would otherwise fire *before* a same-phase
        // listener on `window` ever could, regardless of registration order.
        // stopPropagation() here prevents the page's own handlers from ever
        // seeing the click — closing the gap where a page script reacts to a
        // click dotCMS didn't recognize as a link (e.g. the click landed on a
        // padded/delegate wrapper, not the anchor itself) and performs its own
        // `location.href` navigation, which silently bypasses the SPA entirely
        // and blanks the canvas (dotCMS/core#37961).
        fromEvent<MouseEvent>(win, 'click', { capture: true })
            .pipe(
                filter((e) => {
                    const target = e.target as HTMLElement;

                    // dotCMS's own in-iframe editing machinery needs the real
                    // click to reach it unobstructed, same problem as the
                    // block-editor case below in each case:
                    //  - [data-mode] (WYSIWYG inline edit): TinyMCE (configured
                    //    `inline: true` in inline-edit.service.ts) binds its
                    //    content click dispatch directly on this node — it's
                    //    `editor.getBody()` — to drive selection/image-select
                    //    and other click-reactive behavior.
                    //  - [id^="mceu_"]: every control TinyMCE's UI framework
                    //    renders (toolbar, buttons, menus) gets this id prefix.
                    //    Its floating toolbar is appended to <body> as a
                    //    *sibling* of the editable node, not a descendant of
                    //    it, and wires its own native click listener directly
                    //    on each control's element.
                    const isInlineEditTarget =
                        !!target.closest('[data-mode]') || !!target.dataset?.mode;
                    const isTinyMceUiTarget = !!target.closest('[id^="mceu_"]');

                    // The `@dotcms/uve` SDK script injected into every rendered
                    // page (dot-uve.js) wires block-editor inline editing with a
                    // plain bubble-phase `click` listener registered directly on
                    // the [data-block-editor-content] node (see
                    // libs/sdk/uve/src/script/utils.ts).
                    const isBlockEditorTarget = !!target.closest('[data-block-editor-content]');

                    // Stopping propagation here — on `window`, during the
                    // capture phase, before the event ever reaches any of the
                    // nodes above — would prevent their own handlers from
                    // firing at all. Let those clicks through untouched.
                    if (!isInlineEditTarget && !isTinyMceUiTarget && !isBlockEditorTarget) {
                        e.stopPropagation();
                    }

                    const linkElement = target.closest('a');
                    const href = linkElement?.getAttribute('href');

                    // Hash-only anchors (#section) are same-page scrolls. The
                    // browser can't do it here: the srcdoc's base URL is the
                    // admin's, so it would load the admin inside the canvas.
                    // Scroll the iframe ourselves, and skip both internalNav and
                    // inlineEditing emits even when the anchor is nested inside
                    // an editable [data-mode] region.
                    if (href?.startsWith('#')) {
                        e.preventDefault();
                        scrollIframeToFragment(win, href);

                        return false;
                    }

                    const hasLink = !!href;
                    return hasLink || isInlineEditTarget;
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
