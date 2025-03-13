import { __NOTIFY_CLIENT__ } from './enums';

import { UVECallback, UVEEvent } from '../lib/types';

// TODO: WE NEED TO LOOK FOR ALL THE NOTIFY_CLIENT EVENTS AND ADD THEM TO THE UVE_EVENTS CONSTANT WHEN WE MIGRATE THE EDITOR TO THE NEW UVE LIBRARY

/**
 * Events that can be subscribed to in the UVE
 *
 * @internal
 * @type {Record<string, UVEEvent>}
 */
export const __UVE_EVENTS__: Record<string, UVEEvent> = {
    changes: (callback: UVECallback) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === __NOTIFY_CLIENT__.UVE_SET_PAGE_DATA) {
                callback(event.data.payload);
            }
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: 'changes'
        };
    }
};

/**
 * Default UVE event
 *
 * @param {string} event - The event to subscribe to.
 * @internal
 */
export const __UVE_EVENT_ERROR_FALLBACK__ = (event: string) => {
    return {
        unsubscribe: () => {
            /* do nothing */
        },
        event
    };
};
