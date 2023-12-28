import { useEffect, useCallback } from 'react';

import { getPageElementBound } from '../utils/utils';

export function useEventHandlers({
    rows,
    reload = window.location.reload,
}: {
    rows: React.MutableRefObject<HTMLDivElement[]>;
    reload?: () => void;
}) {
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
                            action: 'set-bounds',
                            payload: positionData,
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
                action: 'scroll',
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
