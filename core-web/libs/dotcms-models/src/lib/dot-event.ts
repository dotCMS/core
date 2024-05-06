/**
 * Interface for custom events.
 *
 * @interface
 */
export interface DotEvent<T> {
    name: string;
    data?: T;
}

type EventName = 'close' | 'edit-contentlet-data-updated';

type CloseEventDetail = {
    name: EventName;
    data: {
        languageId: string;
        redirectUrl: string;
    };
};

type EditContentletDataUpdatedEventDetail = {
    name: EventName;
    payload: boolean;
};

type EventDetail = CloseEventDetail | EditContentletDataUpdatedEventDetail;

export type CustomIframeDialogEvent = CustomEvent<EventDetail>;
