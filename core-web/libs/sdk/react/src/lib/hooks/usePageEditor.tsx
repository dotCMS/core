import { useEffect, useRef } from 'react';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '@dotcms/client';

import { getPageElementBound } from '../utils/utils';

/**
 * `PageEditorOptions` is an interface that defines the options for the `usePageEditor` hook.
 * It includes an optional `reloadFunction` that is called when the editor needs to reload the page to get changes,
 * and an optional `pathname` that represents the path of the page that the editor is editing.
 *
 * @interface PageEditorOptions
 *
 * @property {Function} reloadFunction - An optional function that is called when the editor needs to reload the page to get changes.
 * @property {string} pathname - An optional string that represents the path of the page that the editor is editing.
 */
interface PageEditorOptions {
    /**
     * `reloadFunction` is an optional function that can be provided to the `PageEditorOptions` object.
     * It is called when the dotcms editor needs to reload the page to get changes.
     *
     * @property {Function} reloadFunction
     * @default window.location.reload
     * @memberof PageEditorOptions
     * @type {() => void}
     * @optional
     */
    reloadFunction?: () => void;
    /**
     * `pathname` is an optional string that can be provided to the `PageEditorOptions` object.
     * It represents the path of the page that the editor is editing.
     * When this path changes, the editor will update its own state and reload the page to get the changes.
     * @property {string} pathname
     * @memberof PageEditorOptions
     * @type {string}
     * @optional
     */
    pathname?: string;
}

/**
 * `usePageEditor` is a custom React hook that sets up the page editor for a DotCMS page.
 * It takes a `PageEditorOptions` object as a parameter and returns a reference to the rows of the page.
 *
 * This hook is the main brigde between your webapp and the dotcms page editor.
 *
 *
 * @category Hooks
 * @param {PageEditorOptions} props - The options for the page editor. Includes a `reloadFunction` and a `pathname`.
 * @returns {React.RefObject<HTMLDivElement>[]} - A reference to the rows of the page.
 * @throws {Error} - Throws an error if the `pathname` is not provided.
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
