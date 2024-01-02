import { useEffect, useRef } from 'react';

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
}

/**
 * This hook is used to handle the events from your webapp to dotcms page editor
 *
 * @category Hooks
 *
 * @export
 * @param {useEventHandlerProps} props
 */
function useRequestBounds() {
    const rows = useRef<HTMLDivElement[]>([]);

    useEffect(() => {
        function eventMessageHandler(event: MessageEvent) {
            if (event.data === 'ema-request-bounds') {
                const positionData = getPageElementBound(rows.current);

                postMessageToEditor({
                    action: CUSTOMER_ACTIONS.SET_BOUNDS,
                    payload: positionData
                });
            }
        }

        window.addEventListener('message', eventMessageHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
        };
    }, [rows]);

    return rows;
}

function useScrollEvent() {
    useEffect(() => {
        function eventScrollHandler() {
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            });
        }

        window.addEventListener('scroll', eventScrollHandler);

        return () => {
            window.removeEventListener('scroll', eventScrollHandler);
        };
    }, []);
}

function useReloadPage(reload: () => void = window.location.reload) {
    useEffect(() => {
        function eventMessageHandler(event: MessageEvent) {
            if (event.data === 'ema-reload-page') {
                reload();
            }
        }

        window.addEventListener('message', eventMessageHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
        };
    }, [reload]);
}

function usePostUrlToEditor(pathname: string) {
    useEffect(() => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }, [pathname]);
}

interface PageEditorOptions {
    reloadFunction?: () => void;
    pathname?: string;
}

export const usePageEditor = (props: PageEditorOptions) => {
    const { reloadFunction = window.location.reload, pathname = null } = props;

    if (!pathname) {
        throw new Error('Dotcms page editor required the pathname of your webapp');
    }

    usePostUrlToEditor(pathname);
    useScrollEvent();
    useReloadPage(reloadFunction);
    const rowsRef = useRequestBounds();

    return rowsRef;
};
