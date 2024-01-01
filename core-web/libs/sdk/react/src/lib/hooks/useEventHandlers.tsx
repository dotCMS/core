import { useEffect, useCallback } from 'react';

import { CUSTOMER_ACTIONS } from '@dotcms/client';

import { getPageElementBound } from '../utils/utils';

type Props = {
    /**
     * Pass the rows elements to get the bonds of the page
     *
     * @type {React.MutableRefObject<HTMLDivElement[]>}
     */
    rows: React.MutableRefObject<HTMLDivElement[]>;
    /**
     * Reload the page
     *
     * @type {() => void}
     */
    reload?: () => void;
};

/**
 * This hook is used to handle the events from your webapp to dotcms page editor
 *
 * @export
 * @param {Props} {
 *     rows,
 *     reload = window.location.reload,
 * }
 */
export function useEventHandlers({ rows, reload = window.location.reload }: Props) {
    const eventMessageHandler = useCallback(
        (event: MessageEvent) => {
            const positionData = getPageElementBound(rows.current);

            switch (event.data) {
                case 'ema-reload-page':
                    reload();
                    break;

                case 'ema-request-bounds':
                    window.parent.postMessage(
                        {
                            action: CUSTOMER_ACTIONS.SET_BOUNDS,
                            payload: positionData
                        },
                        '*'
                    );
                    break;

                default:
                    break;
            }
        },
        [rows, reload]
    );

    const eventScrollHandler = useCallback((_event: Event) => {
        window.parent.postMessage(
            {
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            },
            '*'
        );
    }, []);

    useEffect(() => {
        window.addEventListener('message', eventMessageHandler);
        window.addEventListener('scroll', eventScrollHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
            window.removeEventListener('scroll', eventScrollHandler);
        };
    }, [eventMessageHandler, eventScrollHandler]);
}
