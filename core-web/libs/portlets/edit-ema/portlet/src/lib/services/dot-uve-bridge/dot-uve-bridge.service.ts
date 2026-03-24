import { fromEvent, Observable } from 'rxjs';

import { Injectable, ElementRef, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DotCMSUVEAction } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';

import { PostMessage } from '../../shared/models';

@Injectable()
export class DotUveBridgeService {
    private readonly window = inject(WINDOW);
    private readonly destroyRef = inject(DestroyRef);
    private iframeElement?: HTMLIFrameElement;

    initialize(iframe: ElementRef<HTMLIFrameElement>): Observable<MessageEvent> {
        this.iframeElement = iframe.nativeElement;

        return fromEvent<MessageEvent>(this.window, 'message').pipe(
            takeUntilDestroyed(this.destroyRef)
        );
    }

    handleMessage(
        event: MessageEvent,
        onUveMessage: (message: PostMessage) => void,
        onClampScroll: () => void
    ): void {
        const data = event.data;
        if (this.isUvePostMessage(data)) {
            onUveMessage(data);
            if (data.action === DotCMSUVEAction.IFRAME_HEIGHT) {
                onClampScroll();
            }
        }
    }

    sendMessageToIframe(message: unknown, host = '*'): void {
        this.iframeElement?.contentWindow?.postMessage(message, host);
    }

    private isUvePostMessage(data: unknown): data is PostMessage {
        return !!data && typeof data === 'object' && 'action' in data;
    }
}
