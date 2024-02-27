import { CUSTOMER_ACTIONS, postMessageToEditor } from './actions/client.actions';
import { getContainerData, getPageElementBound } from './utils/editor.utils';

interface DotCMSPageEditorConfig {
    onReload: () => void;
}
class DotCMSPageEditor {
    private config: DotCMSPageEditorConfig;

    constructor(config?: DotCMSPageEditorConfig) {
        this.config = config ?? { onReload: defaultReloadFn };
    }

    init() {
        console.log('SdkDotPageEditor init!');
        const isInsideEditor = this.checkIfInsideEditor();
        if (isInsideEditor) {
            this.listenEditorMessages();
            setTimeout(() => {
                this.listenHoveredContentlet();
            }, 100);
            this.scrollHandler();
            this.listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
        }
    }

    setUrl(pathname: string) {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }

    listenEditorMessages() {
        window.addEventListener('message', (event: MessageEvent) => {
            switch (event.data) {
                case 'ema-request-bounds': {
                    console.log('Requesting bounds');
                    this.setBounds();
                    break;
                }

                case 'ema-reload-page': {
                    console.log('Reloading page');
                    this.reloadPage();
                    break;
                }
            }
        });
    }

    listenHoveredContentlet() {
        const contentletElements = document.querySelectorAll('[data-dot-object="contentlet"]');

        contentletElements.forEach((element) => {
            element.addEventListener('pointerenter', (event) => {
                // Your pointer enter event handler logic here
                // console.log('Pointer entered:', event.target);
                const target = event.target as HTMLElement;

                const { x, y, width, height } = target.getBoundingClientRect();
                

                const contentletPayload = {
                    container:
                        // Here extract dot-container from contentlet if is Headless
                        // or search in parent container if is VTL
                        target.dataset?.['dotContainer']
                            ? JSON.parse(target.dataset?.['dotContainer'])
                            : getContainerData(element),
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
            });
        });
    }

    scrollHandler() {
        window.addEventListener('scroll', () => {
            // console.log('scroll');
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            });
        });
    }

    checkIfInsideEditor() {
        if (window.parent === window) {
            return false;
        }
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });
        return true;
    }

    listenContentChange() {
        // const config = { attributes: true, childList: true, subtree: true };

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
                        // console.log('Content change!!!!');
                        postMessageToEditor({
                            action: CUSTOMER_ACTIONS.CONTENT_CHANGE
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
        const rows = Array.from(
            document.querySelectorAll('[data-dot="row"]')
        ) as unknown as HTMLDivElement[];

        const positionData = getPageElementBound(rows);
        console.log("position: ",positionData);

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_BOUNDS,
            payload: positionData
        });
    }

    reloadPage() {
        this.config.onReload();
    }
}

const defaultReloadFn = () => window.location.reload();

export const sdkDotPageEditor = {
    init: (config?: DotCMSPageEditorConfig) => {
        const dotCMSPageEditor = new DotCMSPageEditor(config);
        dotCMSPageEditor.init();

        return dotCMSPageEditor;
    }
};

export const sdkAsString = `
const CUSTOMER_ACTIONS =  {
    /**
     * Tell the dotcms editor that page change
     */
    SET_URL: 'set-url',
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */
    SET_BOUNDS: 'set-bounds',
    /**
     * Send the information of the hovered contentlet
     */
    SET_CONTENTLET:'set-contentlet', // Check
    /**
     * Tell the editor that the page is being scrolled
     */
    IFRAME_SCROLL: 'scroll', // Check
    /**
     * Ping the editor to see if the page is inside the editor
     */
    PING_EDITOR :'ping-editor', //Check

    CONTENT_CHANGE :'content-change', //Check

    NOOP :'noop'
}


  function postMessageToEditor(message) {
    window.parent.postMessage(message, '*');
    }

    function getPageElementBound(rowsNodes) {
        if (!rowsNodes) {
            return [];
        }
    
        return rowsNodes.map((row) => {
            const rowRect = row.getBoundingClientRect();
            const columns = row.children;
    
            return {
                x: rowRect.x,
                y: rowRect.y,
                width: rowRect.width,
                height: rowRect.height,
                columns: Array.from(columns).map((column) => {
                    const columnRect = column.getBoundingClientRect();
                    const containers = Array.from(
                        column.querySelectorAll('[data-dot="container"]')
                    ) 
    
                    const columnX = columnRect.left - rowRect.left;
                    const columnY = columnRect.top - rowRect.top;
    
                    return {
                        x: columnX,
                        y: columnY,
                        width: columnRect.width,
                        height: columnRect.height,
                        containers: containers.map((container) => {
                            const containerRect = container.getBoundingClientRect();
                            const contentlets = Array.from(
                                container.querySelectorAll('[data-dot-object="contentlet"]')
                            ) 
    
                            return {
                                x: 0,
                                y: containerRect.y - rowRect.top,
                                width: containerRect.width,
                                height: containerRect.height,
                                payload: container.dataset?.['content'], //TODO: Change this
                                contentlets: contentlets.map((contentlet) => {
                                    const contentletRect = contentlet.getBoundingClientRect();
    
                                    return {
                                        x: 0,
                                        y: contentletRect.y - containerRect.y,
                                        width: contentletRect.width,
                                        height: contentletRect.height,
                                        payload: JSON.stringify({
                                            container: contentlet.dataset?.['dotContainer']
                                                ? JSON.parse(contentlet.dataset?.['dotContainer'])
                                                : getContainerData(contentlet),
                                            contentlet: {
                                                identifier: contentlet.dataset?.['dotIdentifier'],
                                                title: contentlet.dataset?.['dotTitle'],
                                                inode: contentlet.dataset?.['dotInode'],
                                                contentType: contentlet.dataset?.['dotType']
                                            }
                                        })
                                    };
                                })
                            };
                        })
                    };
                })
            };
        });
    }
    
    function getContainerData(element) {
        // Find the closest ancestor element with data-dot-object="container" attribute
        const container = element.closest('[data-dot-object="container"]') 
    
        // If a container element is found
        if (container) {
            // Return the dataset of the container element
            return {
                acceptTypes: container.dataset?.['dotAcceptTypes'],
                identifier: container.dataset?.['dotIdentifier'],
                maxContentlets: container.dataset?.['dotMaxContentlets'],
                uuid: container.dataset?.['dotUuid']
            };
        } else {
            // If no container element is found, return null
            return null;
        }
    }
    
    


class DotCMSPageEditor {
    config;

    constructor(config) {
        this.config = config ?? { onReload: defaultReloadFn };
    }

    init() {
        console.log('SdkDotPageEditor init from VTL!');
        const isInsideEditor = this.checkIfInsideEditor();
        if (isInsideEditor) {
            this.listenEditorMessages();
            setTimeout(() => {
                this.listenHoveredContentlet();
            }, 100);
            this.scrollHandler();
            this.listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
        }
    }

    setUrl(pathname) {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    }

    listenEditorMessages() {
        window.addEventListener('message', (event) => {
            switch (event.data) {
                case 'ema-request-bounds': {
                    console.log('Requesting bounds');
                    this.setBounds();
                    break;
                }

                case 'ema-reload-page': {
                    console.log('Reloading page');
                    this.reloadPage();
                    break;
                }
            }
        });
    }

    listenHoveredContentlet() {
        const contentletElements = document.querySelectorAll('[data-dot-object="contentlet"]');

        contentletElements.forEach((element) => {
            element.addEventListener('pointerenter', (event) => {
                // Your pointer enter event handler logic here
                // console.log('Pointer entered:', event.target);
                const target = event.target;

                const { x, y, width, height } = target.getBoundingClientRect();

                const contentletPayload = {
                    container:
                        // Here extract dot-container from contentlet if is Headless
                        // or search in parent container if is VTL
                        target.dataset?.['dotContainer'] ? JSON.parse(target.dataset?.['dotContainer']) :
                getContainerData(element),
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
            });
        });
    }

    scrollHandler() {
        window.addEventListener('scroll', () => {
            // console.log('scroll');
            postMessageToEditor({
                action: CUSTOMER_ACTIONS.IFRAME_SCROLL
            });
        });
    }

    checkIfInsideEditor() {
        if (window.parent === window) {
            return false;
        }
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });
        return true;
    }

    listenContentChange() {
        // const config = { attributes: true, childList: true, subtree: true };

        const observer = new MutationObserver((mutationsList) => {
            for (const { addedNodes, removedNodes, type } of mutationsList) {
                if (type === 'childList') {
                    const didNodesChanged = [
                        ...Array.from(addedNodes),
                        ...Array.from(removedNodes)
                    ].filter(
                        (node) => node.dataset?.['dot'] === 'contentlet'
                    ).length;

                    if (didNodesChanged) {
                        // console.log('Content change!!!!');
                        postMessageToEditor({
                            action: CUSTOMER_ACTIONS.CONTENT_CHANGE
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
        const rows = Array.from(
            document.querySelectorAll('[data-dot="row"]')
        )

        console.log("rows: ",rows);

        const positionData = getPageElementBound(rows);

        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_BOUNDS,
            payload: positionData
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
    }
};

sdkDotPageEditor.init();


`;
