export class DotEventsSocketURL {
    constructor(private protocol: string, private baseUrl: string, private endPoint: string) {}

    get url(): string {
        return `${this.protocol}://${this.baseUrl}${this.endPoint}`;
    }

    public getHttpUrl(): string {
        return new DotEventsSocketURL(this.protocol === 'ws' ? 'http' : 'https', this.baseUrl, this.endPoint).url;
    }
}
