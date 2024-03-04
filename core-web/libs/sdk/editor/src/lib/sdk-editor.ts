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

    init() {
        console.log('SdkDotPageEditor init!!');
        this.isInsideEditor = this.checkIfInsideEditor();
        if (this.isInsideEditor) {
            this.listenEditorMessages();
            this.listenHoveredContentlet();
            this.scrollHandler();
            this.listenContentChange();
        }
    }

    destroy() {
        console.log('SdkDotPageEditor destroyed!');
        this.subscriptions.forEach((subscription) => {
            if (subscription.type === 'listener') {
                window?.removeEventListener(subscription.event, subscription.callback);
            }

            if (subscription.type === 'observer') {
                subscription.observer.disconnect();
            }
        });
    }

    updateNavigation(pathname: string) {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }

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

    private checkIfInsideEditor() {
        if (window?.parent === window) {
            return false;
        }
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });
        return true;
    }

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

const defaultReloadFn = () => window.location.reload();

export const sdkDotPageEditor = {
    createClient: (config?: DotCMSPageEditorConfig) => {
        const dotCMSPageEditor = new DotCMSPageEditor(config);
        dotCMSPageEditor.init();

        return dotCMSPageEditor;
    }
};
