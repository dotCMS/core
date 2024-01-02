import { useEffect, useCallback } from 'react';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

import { getPageElementBound } from '../utils/utils';

/**
 * Props for the useEventHandler hook
 *
 * @interface useEventHandlerProps
 */
export interface useEventHandlerProps {
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
}

/**
 * This hook is used to handle the events from your webapp to dotcms page editor
 *
 * @category Hooks
 *
 * @export
 * @param {useEventHandlerProps} props
 */
export function useEventHandlers(props: useEventHandlerProps) {
    const { rows, reload = window.location.reload } = props;
    const eventMessageHandler = useCallback(
        (event: MessageEvent) => {
            const positionData = getPageElementBound(rows.current);

            switch (event.data) {
                case 'ema-reload-page':
                    reload();
                    break;

                case 'ema-request-bounds':
                    postMessageToEditor({
                        action: CUSTOMER_ACTIONS.SET_BOUNDS,
                        payload: positionData
                    });
                    break;

                default:
                    break;
            }
        },
        [rows, reload]
    );

    const eventScrollHandler = useCallback((_event: Event) => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
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
