import { fromEvent } from 'rxjs';

import { NgStyle } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    inject,
    Input,
    OnDestroy,
    Output,
    ViewChild,
    signal,
    DestroyRef,
    effect
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { toObservable } from '@angular/core/rxjs-interop';

import { DotMessageService, DotSeoMetaTagsService, DotSeoMetaTagsUtilService } from '@dotcms/data-access';
import { SeoMetaTags } from '@dotcms/dotcms-models';
import { SafeUrlPipe } from '@dotcms/ui';

import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { UVEStore } from '../../../store/dot-uve.store';
import { SDK_EDITOR_SCRIPT_SOURCE } from '../../../utils';

@Component({
    selector: 'dot-uve-iframe',
    standalone: true,
    templateUrl: './dot-uve-iframe.component.html',
    styleUrls: ['./dot-uve-iframe.component.scss'],
    imports: [NgStyle, SafeUrlPipe]
})
export class DotUveIframeComponent implements OnDestroy {
    @ViewChild('iframe') iframe!: ElementRef<HTMLIFrameElement>;

    @Input() src!: string | null;
    @Input() title!: string;
    @Input() pointerEvents!: string | null;
    @Input() opacity!: number | null;
    @Input() host = '*';

    @Output() load = new EventEmitter<void>();
    @Output() internalNav = new EventEmitter<MouseEvent>();
    @Output() inlineEditing = new EventEmitter<MouseEvent>();
    @Output() iframeDocHeightChange = new EventEmitter<number>();

    protected readonly uveStore = inject(UVEStore);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private readonly dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private readonly inlineEditingService = inject(InlineEditService);
    private readonly destroyRef = inject(DestroyRef);

    #iframeContentResizeObserver: ResizeObserver | null = null;
    #iframeMutationObserver: MutationObserver | null = null;
    #didSetInitialScroll = false;

    readonly $iframeDocHeight = signal<number>(0);

    readonly $pageRender = this.uveStore.$pageRender;
    readonly $enableInlineEdit = this.uveStore.$enableInlineEdit;
    readonly $isTraditionalPageEffect = effect(() => {
        const isTraditional = this.uveStore.isTraditionalPage();
        const pageRender = this.$pageRender();
        const enableInlineEdit = this.$enableInlineEdit();

        if (isTraditional && pageRender && this.iframe?.nativeElement?.contentDocument) {
            this.insertPageContent(pageRender, enableInlineEdit);
        }
    });

    get contentWindow(): Window | null {
        return this.iframe?.nativeElement?.contentWindow || null;
    }

    get iframeElement(): HTMLIFrameElement | null {
        return this.iframe?.nativeElement || null;
    }

    onIframeLoad(): void {
        if (!this.uveStore.isTraditionalPage()) {
            this.load.emit();
            return;
        }

        this.insertPageContent(this.$pageRender(), this.$enableInlineEdit());
        this.setSeoData();
        this.setupIframeAutoHeight();
        this.load.emit();
    }

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

    private injectCodeToVTL(rendered: string): string {
        const fileWithScript = this.addEditorPageScript(rendered);
        return this.addCustomStyles(fileWithScript);
    }

    private addEditorPageScript(rendered = ''): string {
        const scriptString = `<script src="${SDK_EDITOR_SCRIPT_SOURCE}"></script>`;
        const bodyExists = rendered.includes('</body>');

        if (!bodyExists) {
            return rendered + scriptString;
        }

        return rendered.replace('</body>', scriptString + '</body>');
    }

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

    private handleInlineScripts(enableInlineEdit: boolean): void {
        const win = this.contentWindow;

        if (!win) {
            return;
        }

        fromEvent(win, 'click')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((e: MouseEvent) => {
                this.internalNav.emit(e);
                this.inlineEditing.emit(e);
            });

        if (enableInlineEdit) {
            this.inlineEditingService.injectInlineEdit(this.iframe);
        } else {
            this.inlineEditingService.removeInlineEdit(this.iframe);
        }
    }

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
            this.uveStore.setOGTagResults(results);
        });
    }

    private setupIframeAutoHeight(): void {
        const iframeEl = this.iframe?.nativeElement;

        if (!iframeEl) {
            return;
        }

        this.teardownIframeAutoHeight();

        let doc: Document | null = null;
        let win: Window | null = null;
        try {
            doc = iframeEl.contentDocument;
            win = iframeEl.contentWindow;
        } catch {
            // Cross-origin; height must be provided via postMessage.
            return;
        }

        if (!doc || !win) {
            return;
        }

        const updateHeight = () => {
            const body = doc.body;
            const root = doc.documentElement;

            if (!body || !root) {
                return;
            }

            const height = Math.max(body.scrollHeight, root.scrollHeight);
            iframeEl.style.height = `${height}px`;
            this.$iframeDocHeight.set(height);
            this.iframeDocHeightChange.emit(height);
        };

        requestAnimationFrame(() => {
            updateHeight();
            requestAnimationFrame(updateHeight);
        });

        if (typeof ResizeObserver !== 'undefined') {
            this.#iframeContentResizeObserver = new ResizeObserver(() => updateHeight());
            this.#iframeContentResizeObserver.observe(doc.body);
            this.#iframeContentResizeObserver.observe(doc.documentElement);
        }

        if (typeof MutationObserver !== 'undefined') {
            this.#iframeMutationObserver = new MutationObserver(() => updateHeight());
            this.#iframeMutationObserver.observe(doc.documentElement, {
                childList: true,
                subtree: true,
                attributes: true,
                characterData: true
            });
        }

        fromEvent(win, 'load')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => updateHeight());
    }

    private teardownIframeAutoHeight(): void {
        this.#iframeContentResizeObserver?.disconnect();
        this.#iframeContentResizeObserver = null;
        this.#iframeMutationObserver?.disconnect();
        this.#iframeMutationObserver = null;
    }

    ngOnDestroy(): void {
        this.teardownIframeAutoHeight();
    }
}

