import { useEffect, useRef, useState } from 'react';

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
 * @returns {{rowsRef: React.RefObject<HTMLDivElement>[], isInsideEditor: boolean}} - Returns a reference to the rows of the page and a boolean that indicates if the page is inside the editor.
 * @throws {Error} - Throws an error if the `pathname` is not provided.
 */
export const usePageEditor = (
    props: PageEditorOptions
): { rowsRef: React.MutableRefObject<HTMLDivElement[]>; isInsideEditor: boolean } => {
    const { reloadFunction = window.location.reload, pathname = null } = props;

    if (!pathname) {
        throw new Error('Dotcms page editor required the pathname of your webapp');
    }

    const { rowsRef, isInsideEditor } = useEventMessageHandler({ reload: reloadFunction });

    usePostUrlToEditor(pathname, isInsideEditor);
    useScrollEvent(isInsideEditor);

    return { rowsRef, isInsideEditor };
};

function useEventMessageHandler({ reload = window.location.reload }: { reload: () => void }) {
    const rows = useRef<HTMLDivElement[]>([]);

    const [isInsideEditor, setIsInsideEditor] = useState(false);

    useEffect(() => {
        // If the page is not inside an iframe we do nothing.
        if (window.parent === window) return;

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.PING_EDITOR // This is to let the editor know that the page is ready
        });
    }, []);

    useEffect(() => {
        function eventMessageHandler(event: MessageEvent) {
            if (!isInsideEditor) {
                // Editor is telling us that we can set ourselves into edit mode
                if (event.data === 'ema-editor-pong') {
                    setIsInsideEditor(true);
                }

                return;
            }

            switch (event.data) {
                case 'ema-request-bounds': {
                    const positionData = getPageElementBound(rows.current);

                    postMessageToEditor({
                        action: CUSTOMER_ACTIONS.SET_BOUNDS,
                        payload: positionData
                    });

                    break;
                }

                case 'ema-reload-page': {
                    reload();

                    break;
                }
            }
        }

        window.addEventListener('message', eventMessageHandler);

        return () => {
            window.removeEventListener('message', eventMessageHandler);
        };
    }, [rows, reload, isInsideEditor]);

    return {
        rowsRef: rows,
        isInsideEditor
    };
}

function useScrollEvent(isInsideEditor: boolean) {
    useEffect(() => {
        if (!isInsideEditor) return;

        function eventScrollHandler() {
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            });
        }

        window.addEventListener('scroll', eventScrollHandler);

        return () => {
            window.removeEventListener('scroll', eventScrollHandler);
        };
    }, [isInsideEditor]);
}

function usePostUrlToEditor(pathname: string, isInsideEditor: boolean) {
    useEffect(() => {
        if (!isInsideEditor) return;

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }, [pathname, isInsideEditor]);
}
