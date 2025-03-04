import { NOTIFY_CLIENT } from './enums';

import { UVECallback, UVEEvent } from '../lib/types';

// TODO: WE NEED TO LOOK FOR ALL THE NOTIFY_CLIENT EVENTS AND ADD THEM TO THE UVE_EVENTS CONSTANT WHEN WE MIGRATE THE EDITOR TO THE NEW UVE LIBRARY

/**
 * Events that can be subscribed to in the UVE
 *
 * @type {Record<string, UVEEvent>}
 */
export const UVE_EVENTS: Record<string, UVEEvent> = {
    changes: (callback: UVECallback) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === NOTIFY_CLIENT.UVE_SET_PAGE_DATA) {
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
