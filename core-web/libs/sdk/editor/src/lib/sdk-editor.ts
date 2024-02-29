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

class DotCMSPageEditor {
    private config: DotCMSPageEditorConfig;
    private subscriptions: DotCMSPageEditorSubscription[] = [];

    isInsideEditor!: boolean;

    constructor(config?: DotCMSPageEditorConfig) {
        this.config = config ?? { onReload: defaultReloadFn };
    }

    init() {
        console.log('SdkDotPageEditor Headless init.');
        this.isInsideEditor = this.checkIfInsideEditor();
        if (this.isInsideEditor) {
            this.listenEditorMessages();
            this.listenHoveredContentlet();
            this.scrollHandler();
            this.listenContentChange();
        }
    }

    destroy() {
        console.log('SdkDotPageEditor Headless destroy.');
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
        window?.addEventListener('message', messageCallback);
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
                        (node) => (node as HTMLDivElement).dataset?.['dot'] === 'contentlet'
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

export const sdkAsString = `
const CUSTOMER_ACTIONS = {
    /**
     * Tell the dotcms editor that page change
     */
    SET_URL: "set-url",
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */
    SET_BOUNDS: "set-bounds",
    /**
     * Send the information of the hovered contentlet
     */
    SET_CONTENTLET: "set-contentlet", // Check
    /**
     * Tell the editor that the page is being scrolled
     */
    IFRAME_SCROLL: "scroll", // Check
    /**
     * Ping the editor to see if the page is inside the editor
     */
    PING_EDITOR: "ping-editor", //Check
  
    CONTENT_CHANGE: "content-change", //Check
  
    NOOP: "noop",
  };
  
  const NOTIFY_CUSTOMER = {
    EMA_RELOAD_PAGE: "ema-reload-page", // We need to reload the ema page
    EMA_REQUEST_BOUNDS: "ema-request-bounds",
    EMA_EDITOR_PONG: "ema-editor-pong",
  };
  
  function postMessageToEditor(message) {
      window.parent.postMessage(message, '*');
  }
  
  function getPageElementBound(containers) {
    return containers.map((container) => {
      const containerRect = container.getBoundingClientRect();
      const contentlets = Array.from(
        container.querySelectorAll('[data-dot-object="contentlet"]')
      );
  
      return {
        x: containerRect.x,
        y: containerRect.y,
        width: containerRect.width,
        height: containerRect.height,
        payload: container.dataset?.["content"] ?? {
          container: getContainerData(container),
        },
        contentlets: getContentletsBound(containerRect, contentlets),
      };
    });
  }
  
  function getContentletsBound(containerRect, contentlets) {
    return contentlets.map((contentlet) => {
      const contentletRect = contentlet.getBoundingClientRect();
  
      return {
        x: 0,
        y: contentletRect.y - containerRect.y,
        width: contentletRect.width,
        height: contentletRect.height,
        payload: JSON.stringify({
          container: contentlet.dataset?.["dotContainer"]
            ? JSON.parse(contentlet.dataset?.["dotContainer"])
            : getClosestContainerData(contentlet),
          contentlet: {
            identifier: contentlet.dataset?.["dotIdentifier"],
            title: contentlet.dataset?.["dotTitle"],
            inode: contentlet.dataset?.["dotInode"],
            contentType: contentlet.dataset?.["dotType"],
          },
        }),
      };
    });
  }
  //Used to get container data from VTLS.
  function getContainerData(container) {
    return {
      acceptTypes: container.dataset?.["dotAcceptTypes"],
      identifier: container.dataset?.["dotIdentifier"],
      maxContentlets: container.dataset?.["maxContentlets"],
      uuid: container.dataset?.["dotUuid"],
    };
  }
  
  function getClosestContainerData(element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]');
  
    // If a container element is found
    if (container) {
      // Return the dataset of the container element
      return getContainerData(container);
    } else {
      // If no container element is found, return null
      console.warn("No container found for the contentlet");
      return null;
    }
  }
  //TODO: Fix typeLater
  // USed to find contentlets and later add the listeners "onHover"
  function findContentletElement(element) {
    if (!element) return null;
  
    if (element.dataset && element.dataset?.["dotObject"] === "contentlet") {
      return element;
    } else {
      return findContentletElement(element?.["parentElement"]);
    }
  }
  
  class DotCMSPageEditor {
    config;
    isInsideEditor;
  
    constructor(config) {
      this.config = config ?? { onReload: defaultReloadFn };
    }
  
    init() {
      console.log("SdkDotPageEditor VTL init!!!!!");
      this.isInsideEditor = this.checkIfInsideEditor();
      if (this.isInsideEditor) {
        this.listenEditorMessages();
        this.listenHoveredContentlet();
        this.scrollHandler();
        this.listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
      }
    }
  
    setUrl(pathname) {
      postMessageToEditor({
        action: CUSTOMER_ACTIONS.SET_URL,
        payload: {
          url: pathname === "/" ? "index" : pathname?.replace("/", ""),
        },
      });
    }
  
    listenEditorMessages() {
      window.addEventListener("message", (event) => {
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
      });
    }
  
    listenHoveredContentlet() {
      document.addEventListener("pointermove", (event) => {
        const target = findContentletElement(event.target);
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
            identifier: target.dataset?.["dotIdentifier"],
            title: target.dataset?.["dotTitle"],
            inode: target.dataset?.["dotInode"],
            contentType: target.dataset?.["dotType"],
          },
        };
  
        postMessageToEditor({
          action: CUSTOMER_ACTIONS.SET_CONTENTLET,
          payload: {
            x,
            y,
            width,
            height,
            payload: contentletPayload,
          },
        });
      });
    }
  
    scrollHandler() {
      window.addEventListener("scroll", () => {
        postMessageToEditor({
          action: CUSTOMER_ACTIONS.IFRAME_SCROLL,
        });
      });
    }
  
    checkIfInsideEditor() {
      if (window.parent === window) {
        return false;
      }
      postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR,
      });
      return true;
    }
  
    listenContentChange() {
      const observer = new MutationObserver((mutationsList) => {
        for (const { addedNodes, removedNodes, type } of mutationsList) {
          if (type === "childList") {
            const didNodesChanged = [
              ...Array.from(addedNodes),
              ...Array.from(removedNodes),
            ].filter((node) => node.dataset?.["dot"] === "contentlet").length;
  
            if (didNodesChanged) {
              postMessageToEditor({
                action: CUSTOMER_ACTIONS.CONTENT_CHANGE,
              });
              //To add the listener to the new contents.
              this.listenHoveredContentlet();
            }
          }
        }
      });
  
      observer.observe(document, { childList: true, subtree: true });
      return observer;
    }
  
    setBounds() {
      const containers = Array.from(
        document.querySelectorAll('[data-dot-object="container"]')
      );
      const positionData = getPageElementBound(containers);
  
      postMessageToEditor({
        action: CUSTOMER_ACTIONS.SET_BOUNDS,
        payload: positionData,
      });
    }
  
    reloadPage() {
      this.config.onReload();
    }
  }
  
  const defaultReloadFn = () => window.location.reload();
  
  const sdkDotPageEditor = {
    init: (config) => {
      const dotCMSPageEditor = new DotCMSPageEditor(config);
      dotCMSPageEditor.init();
  
      return dotCMSPageEditor;
    },
  };
  
  sdkDotPageEditor.init();
  `;
