import { CUSTOMER_ACTIONS, postMessageToEditor } from './models/client.model';
import { NOTIFY_CUSTOMER } from './models/editor.model';
import {
    findContentletElement,
    getClosestContainerData,
    getPageElementBound
} from './utils/editor.utils';

interface DotCMSPageEditorConfig {
    onReload: () => void;
}
interface DotCMSPageEditorListener {
    type: 'listener';
    event: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    callback: (ev: any) => void;
}

interface DotCMSPageEditorObserver {
    type: 'observer';
    observer: MutationObserver;
}

type DotCMSPageEditorSubscription = DotCMSPageEditorListener | DotCMSPageEditorObserver;

export class DotCMSPageEditor {
    private config: DotCMSPageEditorConfig;
    private subscriptions: DotCMSPageEditorSubscription[] = [];

    isInsideEditor!: boolean;

    constructor(config?: DotCMSPageEditorConfig) {
        this.config = config ?? { onReload: defaultReloadFn };
    }

    /**
     * Initializes the SDK editor.
     * Checks if the editor is being used and sets up event listeners accordingly.
     * @memberof DotCMSPageEditor
     */
    init() {
        this.isInsideEditor = this.checkIfInsideEditor();
        if (this.isInsideEditor) {
            this.listenEditorMessages();
            this.listenHoveredContentlet();
            this.scrollHandler();
            this.listenContentChange();
        }
    }

    /**
     * Destroys the SDK editor by removing all event listeners and disconnecting all observers.
     *
     * @memberof DotCMSPageEditor
     */
    destroy() {
        this.subscriptions.forEach((subscription) => {
            if (subscription.type === 'listener') {
                window?.removeEventListener(subscription.event, subscription.callback);
            }

            if (subscription.type === 'observer') {
                subscription.observer.disconnect();
            }
        });
    }

    /**
     *
     * Updates the navigation in the editor.
     * @param {string} pathname - The pathname to update the navigation with.
     * @memberof DotCMSPageEditor
     */
    updateNavigation(pathname: string) {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }

    /**
     * Listens for editor messages and performs corresponding actions based on the received message.
     *
     * @private
     * @memberof DotCMSPageEditor
     */
    private listenEditorMessages() {
        const messageCallback = (event: MessageEvent) => {
            switch (event.data) {
                case NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS: {
                    this.setBounds();
                    break;
                }

                case NOTIFY_CUSTOMER.EMA_RELOAD_PAGE: {
                    this.reloadPage();
                    break;
                }
            }
        };

        window.addEventListener('message', messageCallback);
        this.subscriptions.push({
            type: 'listener',
            event: 'message',
            callback: messageCallback
        });
    }

    /**
     * Listens for pointer move events and extracts information about the hovered contentlet.
     *
     * @private
     * @memberof DotCMSPageEditor
     */
    private listenHoveredContentlet() {
        const pointerMoveCallback = (event: PointerEvent) => {
            const target = findContentletElement(event.target as HTMLElement);
            if (!target) return;
            const { x, y, width, height } = target.getBoundingClientRect();

            const contentletPayload = {
                container:
                    // Here extract dot-container from contentlet if is Headless
                    // or search in parent container if is VTL
                    target.dataset?.['dotContainer']
                        ? JSON.parse(target.dataset?.['dotContainer'])
                        : getClosestContainerData(target),
                contentlet: {
                    identifier: target.dataset?.['dotIdentifier'],
                    title: target.dataset?.['dotTitle'],
                    inode: target.dataset?.['dotInode'],
                    contentType: target.dataset?.['dotType']
                }
            };

            postMessageToEditor({
                action: CUSTOMER_ACTIONS.SET_CONTENTLET,
                payload: {
                    x,
                    y,
                    width,
                    height,
                    payload: contentletPayload
                }
            });
        };

        document.addEventListener('pointermove', pointerMoveCallback);
        this.subscriptions.push({
            type: 'listener',
            event: 'pointermove',
            callback: pointerMoveCallback
        });
    }

    /**
     * v
     *
     * @private
     * @memberof DotCMSPageEditor
     */
    private scrollHandler() {
        const scrollCallback = () => {
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            });
        };

        window?.addEventListener('scroll', scrollCallback);
        this.subscriptions.push({
            type: 'listener',
            event: 'scroll',
            callback: scrollCallback
        });
    }

    /**
     * Checks if the current page is inside an editor.
     *
     * @private
     * @returns {boolean} Returns true if the page is inside an editor, false otherwise.
     * @memberof DotCMSPageEditor
     */
    private checkIfInsideEditor() {
        if (window?.parent === window) {
            return false;
        }
        
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });

        return true;
    }

    /**
     * Listens for changes in the content and triggers a custom action when the content changes.
     *
     * @private
     * @memberof DotCMSPageEditor
     */
    private listenContentChange() {
        const observer = new MutationObserver((mutationsList) => {
            for (const { addedNodes, removedNodes, type } of mutationsList) {
                if (type === 'childList') {
                    const didNodesChanged = [
                        ...Array.from(addedNodes),
                        ...Array.from(removedNodes)
                    ].filter(
                        (node) => (node as HTMLDivElement).dataset?.['dotObject'] === 'contentlet'
                    ).length;

                    if (didNodesChanged) {
                        postMessageToEditor({
                            action: CUSTOMER_ACTIONS.CONTENT_CHANGE
                        });
                    }
                }
            }
        });

        observer.observe(document, { childList: true, subtree: true });
        this.subscriptions.push({
            type: 'observer',
            observer
        });
    }

    /**
     * Sets the bounds of the containers in the editor.
     * Retrieves the containers from the DOM and sends their position data to the editor.
     * @private
     * @memberof DotCMSPageEditor
     */
    private setBounds() {
        const containers = Array.from(
            document.querySelectorAll('[data-dot-object="container"]')
        ) as unknown as HTMLDivElement[];
        const positionData = getPageElementBound(containers);

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_BOUNDS,
            payload: positionData
        });
    }

    private reloadPage() {
        this.config.onReload();
    }
}

/**
 * Default reload function that reloads the current window.
 */
const defaultReloadFn = () => window.location.reload();

export const dotPageEditor = {
    createClient: (config?: DotCMSPageEditorConfig) => {
        const dotCMSPageEditor = new DotCMSPageEditor(config);
        dotCMSPageEditor.init();

        return dotCMSPageEditor;
    }
};
