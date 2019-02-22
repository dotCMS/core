export interface DotAppEventsParams {
    name: string;
    data: { [key: string]: any };
}

export class DotAppEvent {
    emit({ name, data }: DotAppEventsParams): void {
        const customEvent = window.top.document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false, {
            name: name,
            data: data
        });
        window.top.document.dispatchEvent(customEvent);
    }
}
