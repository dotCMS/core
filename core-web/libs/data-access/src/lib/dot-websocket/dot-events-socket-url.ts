import { InjectionToken } from '@angular/core';

/**
 * Represents the URL configuration for the WebSocket event endpoint.
 * Provide this via DotEventsSocketURL injection token.
 */
export class DotEventsSocketURL {
    constructor(
        private url: string,
        private useSSL: boolean
    ) {}

    getWebSocketURL(): string {
        return `${this.useSSL ? 'wss' : 'ws'}://${this.url}`;
    }
}

export const DOT_EVENTS_SOCKET_URL = new InjectionToken<DotEventsSocketURL>('DotEventsSocketURL');
