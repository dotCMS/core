import { fromEvent } from 'rxjs';

import { NgStyle } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    Output,
    ViewChild,
    signal,
    DestroyRef,
    effect
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { filter } from 'rxjs/operators';

import {
    DotMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import { SafeUrlPipe } from '@dotcms/ui';

import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../../../utils';

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
    styleUrls: ['./dot-uve-iframe.component.scss'],
    imports: [NgStyle, SafeUrlPipe]
})
export class DotUveIframeComponent {
    /**
     * Reference to the iframe element.
     * @type {ElementRef<HTMLIFrameElement>}
     */
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    /**
     * URL to load in the iframe.
     * @type {string | null}
     */
    @Input() src!: string | null;
    /**
     * Accessible title for the iframe.
     * @type {string}
     */
    @Input() title!: string;
    /**
     * CSS pointer-events value for the iframe overlay.
     * @type {string | null}
     */
    @Input() pointerEvents!: string | null;
    /**
     * Opacity of the iframe overlay (0–1).
     * @type {number | null}
     */
    @Input() opacity!: number | null;
    /**
     * Host origin for postMessage communication.
     * @type {string}
     * @default '*'
     */
    @Input() host = '*';

    /**
     * Emitted when the iframe has finished loading.
     * @type {EventEmitter<void>}
     */
    @Output() load = new EventEmitter<void>();
    /**
     * Emitted when a click targets an internal link or inline-edit element.
     * @type {EventEmitter<MouseEvent>}
     */
    @Output() internalNav = new EventEmitter<MouseEvent>();
    /**
     * Emitted when a click targets an inline-edit element.
     * @type {EventEmitter<MouseEvent>}
     */
    @Output() inlineEditing = new EventEmitter<MouseEvent>();
    /**
     * Emitted when the iframe document height changes.
     * @type {EventEmitter<number>}
     */
    @Output() iframeDocHeightChange = new EventEmitter<number>();

    protected readonly uveStore = inject(UVEStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * Current height of the iframe document content.
     * @type {Signal<number>}
     */
    readonly $iframeDocHeight = signal<number>(0);

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
     * For headless pages, emits load immediately. For traditional pages, injects
     * content, sets SEO data, then emits load.
     * @returns {void}
     */
    onIframeLoad(): void {
        if (this.uveStore.pageType() === PageType.HEADLESS) {
            this.load.emit();
            return;
        }

        this.insertPageContent(this.$pageRender(), this.$enableInlineEdit());
        this.setSeoData();
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

        const doc = iframeElement.contentDocument;
        const newDoc = this.injectCodeToVTL(pageRender);

        if (!doc) {
            return;
        }

        doc.open();
        doc.write(newDoc);
        doc.close();

        this.handleInlineScripts(enableInlineEdit);
    }

    /**
     * Injects the editor script and custom styles into the rendered HTML.
     * @param {string} rendered - Raw rendered HTML.
     * @returns {string} HTML with script and styles injected.
     */
    private injectCodeToVTL(rendered: string): string {
        const fileWithScript = this.addEditorPageScript(rendered);
        return this.addCustomStyles(fileWithScript);
    }

    /**
     * Appends the UVE editor SDK script before the closing body tag.
     * @param {string} [rendered=''] - HTML string to modify.
     * @returns {string} HTML with the editor script injected.
     */
    private addEditorPageScript(rendered = ''): string {
        const scriptString = `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`;
        const bodyExists = rendered.includes('</body>');

        if (!bodyExists) {
            return rendered + scriptString;
        }

        return rendered.replace('</body>', scriptString + '</body>');
    }

    /**
     * Injects custom styles for empty containers and contentlets before the closing head tag.
     * @param {string} [rendered=''] - HTML string to modify.
     * @returns {string} HTML with custom styles injected.
     */
    private addCustomStyles(rendered = ''): string {
        const styles = `<style>
        [data-dot-object="container"]:empty {
            width: 100%;
            background-color: #ECF0FD;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #030E32;
            height: 10rem;
        }

        [data-dot-object="contentlet"].empty-contentlet {
            min-height: 4rem;
            width: 100%;
        }

        [data-dot-object="container"]:empty::after {
            content: '${this.dotMessageService.get('editpage.container.is.empty')}';
        }
        </style>
        `;

        const headExists = rendered.includes('</head>');

        if (!headExists) {
            return rendered + styles;
        }

        return rendered.replace('</head>', styles + '</head>');
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

        fromEvent<MouseEvent>(win, 'click')
            .pipe(
                filter((e) => {
                    const target = e.target as HTMLElement;
                    const hasLink = !!target.closest('a')?.getAttribute('href');
                    const hasInlineEditTarget =
                        !!target.closest('[data-mode]') || !!target.dataset?.mode;
                    return hasLink || hasInlineEditTarget;
                }),
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

        this.dotSeoMetaTagsService.getMetaTagsResults(doc).subscribe((results) => {
            const ogTags = this.dotSeoMetaTagsUtilService.getMetaTags(doc);
            this.uveStore.setOgTags(ogTags);
            this.uveStore.viewSetOGTagResults(results);
        });
    }
}
