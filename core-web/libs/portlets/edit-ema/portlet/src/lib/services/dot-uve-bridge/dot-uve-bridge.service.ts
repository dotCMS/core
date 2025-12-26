import { Injectable, ElementRef, inject, signal, WritableSignal } from '@angular/core';
import { fromEvent, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { WINDOW } from '@dotcms/utils';
import { DestroyRef } from '@angular/core';
import { PostMessage } from '../../shared/models';
import { DotUveZoomService } from '../dot-uve-zoom/dot-uve-zoom.service';

export interface IframeHeightMessage {
    height: number;
}

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

    sendMessageToIframe(message: unknown, host: string = '*'): void {
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

