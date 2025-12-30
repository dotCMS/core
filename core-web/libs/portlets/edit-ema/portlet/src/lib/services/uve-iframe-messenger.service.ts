import { Injectable } from '@angular/core';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

export interface IframeMessage {
    name: string;
    payload?: unknown;
    direction?: 'up' | 'down';
}

/**
 * Service to manage communication with the UVE editor iframe.
 * Centralizes all postMessage calls to the iframe window.
 */
@Injectable({
    providedIn: 'root'
})
export class UveIframeMessengerService {
    private iframeWindow: Window | null = null;
    private readonly host = '*';

    /**
     * Sets the iframe window reference.
     * Call this from the parent component after iframe is loaded.
     *
     * @param window - The iframe's content window
     */
    setIframeWindow(window: Window | null): void {
        this.iframeWindow = window;
    }

    /**
     * Gets the current iframe window reference.
     *
     * @returns The iframe window or null if not set
     */
    getIframeWindow(): Window | null {
        return this.iframeWindow;
    }

    /**
     * Sends a message to the iframe.
     *
     * @param message - The message to send
     */
    sendPostMessage(message: IframeMessage): void {
        if (!this.iframeWindow) {
            console.warn('Iframe window not set. Cannot send message:', message);
            return;
        }

        this.iframeWindow.postMessage(message, this.host);
    }

    /**
     * Convenience method to send page data updates to the iframe.
     *
     * @param payload - The page data payload
     */
    sendPageData(payload: unknown): void {
        this.sendPostMessage({
            name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
            payload
        });
    }

    /**
     * Convenience method to request bounds from the iframe.
     */
    requestBounds(): void {
        this.sendPostMessage({
            name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS
        });
    }

    /**
     * Convenience method to reload the page in the iframe.
     */
    reloadPage(): void {
        this.sendPostMessage({
            name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
        });
    }

    /**
     * Convenience method to send scroll direction to the iframe.
     *
     * @param direction - The scroll direction ('up' or 'down')
     */
    scrollInsideIframe(direction: 'up' | 'down'): void {
        this.sendPostMessage({
            name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME,
            direction
        });
    }

    /**
     * Convenience method to send copy contentlet inline editing success message.
     *
     * @param payload - The payload data
     */
    copyContentletInlineEditingSuccess(payload: unknown): void {
        this.sendPostMessage({
            name: __DOTCMS_UVE_EVENT__.UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS,
            payload
        });
    }
}
