import { useEffect, useRef } from 'react';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

import { getPageElementBound } from '../utils/utils';

/**
 * This hook is used to handle the events from your webapp to dotcms page editor
 *
 * @category Hooks
 *
 * @return {*}
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

/**
 * Properties for the usePageEditor hook
 *
 * @interface PageEditorOptions
 */
interface PageEditorOptions {
    /**
     * Reload function to be called when the editor needs to reload the page to get changes
     *
     * @memberof PageEditorOptions
     */
    reloadFunction?: () => void;
    /**
     * Current pathname of your webapp, it should be send every time the pathname changes
     *
     * @type {string}
     * @memberof PageEditorOptions
     */
    pathname?: string;
}

/**
 * This hook is used to handle the events from your webapp to dotcms page editor
 *
 * @category Hooks
 * @param {PageEditorOptions} props
 * @return {*}
 * @throws {Error} If pathname is not provided
 */
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
