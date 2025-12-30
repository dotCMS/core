import { fromEvent, Observable } from 'rxjs';

import { Injectable, ElementRef, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { WINDOW } from '@dotcms/utils';

import { PostMessage } from '../../shared/models';
import { DotUveZoomService } from '../dot-uve-zoom/dot-uve-zoom.service';

export interface IframeHeightMessage {
    height: number;
}

/**
 * ==================================================================================
 * HEIGHT REPORTER - SDK IMPLEMENTATIONS
 * ==================================================================================
 *
 * The UVE editor expects pages to report their height via postMessage.
 * SDK developers should implement height reporting in their applications.
 *
 * Accepted message formats:
 *   - { type: 'dotcms:iframeHeight', height: number }        (preferred)
 *   - { name: 'dotcms:iframeHeight', payload: { height } }   (alternative)
 *
 * ----------------------------------------------------------------------------------
 * VANILLA JAVASCRIPT
 * ----------------------------------------------------------------------------------
 *
 * (function() {
 *     let debounceTimer = null;
 *     let lastHeight = 0;
 *
 *     function getDocumentHeight() {
 *         const body = document.body;
 *         if (!body) return 0;
 *
 *         const bodyRect = body.getBoundingClientRect();
 *         let height = bodyRect.height;
 *
 *         const children = body.children;
 *         if (children.length > 0) {
 *             const lastChild = children[children.length - 1];
 *             const lastRect = lastChild.getBoundingClientRect();
 *             const lastBottom = lastRect.bottom - bodyRect.top;
 *             height = Math.max(height, lastBottom);
 *         }
 *
 *         return Math.max(Math.ceil(height), 100);
 *     }
 *
 *     function reportHeight() {
 *         const height = getDocumentHeight();
 *         if (height !== lastHeight && height > 0) {
 *             lastHeight = height;
 *             window.parent.postMessage({ type: 'dotcms:iframeHeight', height }, '*');
 *         }
 *     }
 *
 *     function debouncedReportHeight() {
 *         if (debounceTimer) clearTimeout(debounceTimer);
 *         debounceTimer = setTimeout(reportHeight, 50);
 *     }
 *
 *     if (document.readyState === 'complete') {
 *         reportHeight();
 *     } else {
 *         window.addEventListener('load', reportHeight);
 *     }
 *
 *     if (typeof ResizeObserver !== 'undefined') {
 *         const observer = new ResizeObserver(debouncedReportHeight);
 *         observer.observe(document.body);
 *     }
 * })();
 *
 * ----------------------------------------------------------------------------------
 * REACT HOOK
 * ----------------------------------------------------------------------------------
 *
 * import { useEffect, useRef } from 'react';
 *
 * export function useDotCMSHeightReporter() {
 *     const lastHeightRef = useRef(0);
 *     const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
 *
 *     useEffect(() => {
 *         const getDocumentHeight = (): number => {
 *             const body = document.body;
 *             if (!body) return 0;
 *
 *             const bodyRect = body.getBoundingClientRect();
 *             let height = bodyRect.height;
 *
 *             const children = body.children;
 *             if (children.length > 0) {
 *                 const lastChild = children[children.length - 1];
 *                 const lastRect = lastChild.getBoundingClientRect();
 *                 const lastBottom = lastRect.bottom - bodyRect.top;
 *                 height = Math.max(height, lastBottom);
 *             }
 *
 *             return Math.max(Math.ceil(height), 100);
 *         };
 *
 *         const reportHeight = () => {
 *             const height = getDocumentHeight();
 *             if (height !== lastHeightRef.current && height > 0) {
 *                 lastHeightRef.current = height;
 *                 window.parent.postMessage({ type: 'dotcms:iframeHeight', height }, '*');
 *             }
 *         };
 *
 *         const debouncedReportHeight = () => {
 *             if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
 *             debounceTimerRef.current = setTimeout(reportHeight, 50);
 *         };
 *
 *         reportHeight();
 *
 *         const observer = new ResizeObserver(debouncedReportHeight);
 *         observer.observe(document.body);
 *
 *         return () => {
 *             observer.disconnect();
 *             if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
 *         };
 *     }, []);
 * }
 *
 * // Usage: In your root layout component, call useDotCMSHeightReporter();
 *
 * ----------------------------------------------------------------------------------
 * ANGULAR SERVICE
 * ----------------------------------------------------------------------------------
 *
 * import { Injectable, OnDestroy, inject } from '@angular/core';
 * import { DOCUMENT } from '@angular/common';
 *
 * @Injectable({ providedIn: 'root' })
 * export class DotCMSHeightReporterService implements OnDestroy {
 *     private readonly document = inject(DOCUMENT);
 *     private observer: ResizeObserver | null = null;
 *     private debounceTimer: ReturnType<typeof setTimeout> | null = null;
 *     private lastHeight = 0;
 *
 *     start(): void {
 *         this.reportHeight();
 *
 *         if (typeof ResizeObserver !== 'undefined') {
 *             this.observer = new ResizeObserver(() => this.debouncedReportHeight());
 *             this.observer.observe(this.document.body);
 *         }
 *     }
 *
 *     private getDocumentHeight(): number {
 *         const body = this.document.body;
 *         if (!body) return 0;
 *
 *         const bodyRect = body.getBoundingClientRect();
 *         let height = bodyRect.height;
 *
 *         const children = body.children;
 *         if (children.length > 0) {
 *             const lastChild = children[children.length - 1];
 *             const lastRect = lastChild.getBoundingClientRect();
 *             const lastBottom = lastRect.bottom - bodyRect.top;
 *             height = Math.max(height, lastBottom);
 *         }
 *
 *         return Math.max(Math.ceil(height), 100);
 *     }
 *
 *     private reportHeight(): void {
 *         const height = this.getDocumentHeight();
 *         if (height !== this.lastHeight && height > 0) {
 *             this.lastHeight = height;
 *             window.parent.postMessage({ type: 'dotcms:iframeHeight', height }, '*');
 *         }
 *     }
 *
 *     private debouncedReportHeight(): void {
 *         if (this.debounceTimer) clearTimeout(this.debounceTimer);
 *         this.debounceTimer = setTimeout(() => this.reportHeight(), 50);
 *     }
 *
 *     ngOnDestroy(): void {
 *         this.observer?.disconnect();
 *         if (this.debounceTimer) clearTimeout(this.debounceTimer);
 *     }
 * }
 *
 * // Usage: In AppComponent constructor, inject and call start()
 *
 * ==================================================================================
 */

@Injectable()
export class DotUveBridgeService {
    private readonly window = inject(WINDOW);
    private readonly destroyRef = inject(DestroyRef);
    private zoomService?: DotUveZoomService;
    private iframeElement?: HTMLIFrameElement;
    private editorContent?: HTMLElement;

    private readonly $iframeDocHeight = signal<number>(0);

    initialize(
        iframe: ElementRef<HTMLIFrameElement>,
        editorContent: ElementRef<HTMLElement>,
        zoomService: DotUveZoomService,
    ): Observable<MessageEvent> {
        this.iframeElement = iframe.nativeElement;
        this.editorContent = editorContent.nativeElement;
        this.zoomService = zoomService;

        return fromEvent<MessageEvent>(this.window, 'message').pipe(
            takeUntilDestroyed(this.destroyRef)
        );
    }

    handleMessage(event: MessageEvent, onUveMessage: (message: PostMessage) => void, onClampScroll: () => void): void {
        // 1) Cross-origin iframe height bridge (e.g. Next.js at localhost:3000)
        if (this.maybeHandleIframeHeightMessage(event, onClampScroll)) {
            return;
        }

        // 2) UVE messages
        const data = event.data;
        if (this.isUvePostMessage(data)) {
            onUveMessage(data);
        }
    }

    sendMessageToIframe(message: unknown, host = '*'): void {
        this.iframeElement?.contentWindow?.postMessage(message, host);
    }

    getContentWindow(): Window | null {
        return this.iframeElement?.contentWindow || null;
    }

    getIframeDocHeight(): number {
        return this.$iframeDocHeight();
    }

    private isUvePostMessage(data: unknown): data is PostMessage {
        return (
            !!data &&
            typeof data === 'object' &&
            'action' in data
        );
    }

    private maybeHandleIframeHeightMessage(event: MessageEvent, onClampScroll: () => void): boolean {
        if (!this.iframeElement?.contentWindow) {
            return false;
        }

        // Only accept messages from the current iframe window
        if (event.source !== this.iframeElement.contentWindow) {
            return false;
        }

        const data = event.data as unknown;
        if (!data || typeof data !== 'object') {
            return false;
        }

        const record = data as Record<string, unknown>;
        const isNamed =
            record['name'] === 'dotcms:iframeHeight' && typeof record['payload'] === 'object';
        const isTyped = record['type'] === 'dotcms:iframeHeight';

        let height: number | null = null;
        if (isNamed) {
            const payload = record['payload'] as Record<string, unknown>;
            height = typeof payload['height'] === 'number' ? payload['height'] : null;
        } else if (isTyped) {
            height = typeof record['height'] === 'number' ? (record['height'] as number) : null;
        }

        if (!height || !Number.isFinite(height) || height <= 0) {
            return false;
        }

        // Apply height so iframe never scrolls; also update our layout sizing for zoom/scroll
        if (this.iframeElement) {
            this.iframeElement.style.height = `${Math.ceil(height)}px`;
            this.$iframeDocHeight.set(Math.ceil(height));
            if (this.zoomService) {
                this.zoomService.setIframeDocHeight(Math.ceil(height));
            }
            onClampScroll();
        }

        return true;
    }
}

