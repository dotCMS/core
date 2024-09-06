/**
 * Represent a url to connect with a evend end point
 *
 * @export
 */
export class DotEventsSocketURL {
    constructor(
        private url: string,
        private useSSL: boolean
    ) {}

    /**
     * Return the web socket url to connect with the Event end point
     *
     * @returns string
     * @memberof DotEventsSocketURL
     */
    public getWebSocketURL(): string {
        return `${this.getWebSocketProtocol()}://${this.url}`;
    }

    /**
     * Return the long polling url to connect with the Event end point
     *
     * @returns string
     * @memberof DotEventsSocketURL
     */
    public getLongPoolingURL(): string {
        return `${this.getHttpProtocol()}://${this.url}`;
    }

    private getWebSocketProtocol(): string {
        return `${this.useSSL ? 'wss' : 'ws'}`;
    }

    private getHttpProtocol(): string {
        return `${this.useSSL ? 'https' : 'http'}`;
    }
}
