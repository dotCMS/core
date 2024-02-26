import { CUSTOMER_ACTIONS, postMessageToEditor } from './actions/client.actions';

export const sdkDotPageEditor = {
    init: init,
    setUrl: (pathname: string) => {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: {
                url: pathname === '/' ? 'index' : pathname?.replace('/', '')
            }
        });
    },
    destroy: () => {
        //Remove all listeners
        // observer.disconnect();
    }
};

function init() {
    console.log("SdkDotPageEditor init!")
    const isInsideEditor = checkIfInsideEditor();
    if (isInsideEditor) {
        setTimeout(() => {
            listenHoveredContentlet();
        }, 100);
        scrollHandler();
        listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
    }
}

function listenHoveredContentlet() {
    const contentletElements = document.querySelectorAll('[data-dot-object="contentlet"]');

    contentletElements.forEach((element) => {
        element.addEventListener('pointerenter', (event) => {
            // Your pointer enter event handler logic here
            // console.log('Pointer entered:', event.target);
            const target = event.target as HTMLElement;

            const { x, y, width, height } = target.getBoundingClientRect();
            console.log(target.dataset);
            const contentletPayload = {
                container: target.dataset?.['dotContainer'] ?? getContainerData(element),
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

function scrollHandler() {
    window.addEventListener('scroll', () => {
        // console.log('scroll');
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
    });
}

function checkIfInsideEditor() {
    if (window.parent === window) {
        return false;
    }
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR
    });
    return true;
}

function listenContentChange() {
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
                    listenHoveredContentlet();
                }
            }
        }
    });

    observer.observe(document, { childList: true, subtree: true });
    return observer;
}

function listenHoveredContentlet2() {
    const iframe = document.querySelector('iframe');
    if (iframe) {
        iframe.onload = function () {
            const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document;

            iframeDocument?.addEventListener('pointerenter', (event) => {
                console.log('event: ', event);
                if (
                    event.target instanceof HTMLElement &&
                    event.target.hasAttribute('data-dot-object') &&
                    event.target.getAttribute('data-dot-object') === 'contentlet'
                ) {
                    // Log the event target
                    console.log('Pointer entered:', event.target);
                    const target = event.target as HTMLElement;

                    const { x, y, width, height } = target.getBoundingClientRect();



                    const contentletPayload = JSON.parse(target.dataset?.['content'] ?? '{}');

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
                }
            });
        };
    }
}

function getContainerData(element: Element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]') as HTMLElement;
    
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
function init() {
    console.log("SdkDotPageEditor init!")
    const isInsideEditor = checkIfInsideEditor();
    console.log('isInsideEditor', isInsideEditor);
    if (isInsideEditor) {
        setTimeout(() => {
            listenHoveredContentlet();
        }, 1000);
        scrollHandler();
        listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
    }
}

function listenHoveredContentlet() {
    const contentletElements = document.querySelectorAll('[data-dot-object="contentlet"]');
    console.log('Contentlets: ', contentletElements);

    contentletElements.forEach((element) => {
        element.addEventListener('pointerenter', (event) => {
            // Your pointer enter event handler logic here
            // console.log('Pointer entered:', event.target);
            const target = event.target

            const { x, y, width, height } = target.getBoundingClientRect();

            console.log(target.dataset);
            const contentletPayload = {
                container: target.dataset?.['dotContainer'] ?? getContainerData(element),
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

function getContainerData(element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]');
    
    // If a container element is found
    if (container) {
        // Return the dataset of the container element
        return {
            acceptTypes: container.dataset.dotAcceptTypes,
            identifier: container.dataset.dotIdentifier,
            maxContentlets: container.dataset.dotMaxContentlets,
            uuid: container.dataset.dotUuid
        };
    } else {
        // If no container element is found, return null
        return null;
    }
}

function scrollHandler() {
    window.addEventListener('scroll', () => {
        // console.log('scroll');
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
    });
}

function checkIfInsideEditor() {
    if (window.parent === window) {
        return false;
    }
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR
    });
    return true;
}

function listenContentChange() {
    // const config = { attributes: true, childList: true, subtree: true };
    console.log("Called observer!");
    const observer = new MutationObserver((mutationsList) => {
        for (const { addedNodes, removedNodes, type } of mutationsList) {
            console.log("A change detected!!")
            if (type === 'childList') {
                const didNodesChanged = [
                    ...Array.from(addedNodes),
                    ...Array.from(removedNodes)
                ].filter(
                    (node) => (node).dataset?.['dot'] === 'contentlet'
                ).length;

                if (didNodesChanged) {
                    // console.log('Content change!!!!');
                    postMessageToEditor({
                        action: CUSTOMER_ACTIONS.CONTENT_CHANGE
                    });
                    //To add the listener to the new contents.
                    listenHoveredContentlet();
                }
            }
        }
    });

    observer.observe(document, { attributes: true, childList: true, subtree: true });
    return observer;
}

function listenHoveredContentlet2() {
    console.log('second option');
    const iframe = document.querySelector('iframe');
    console.log(iframe);
    if (iframe) {
        iframe.onload = function () {
            const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document;
            console.log(iframeDocument);

            iframeDocument?.addEventListener('pointerenter', (event) => {
                console.log('event: ', event);
                if (
                    event.target instanceof HTMLElement &&
                    event.target.hasAttribute('data-dot') &&
                    event.target.getAttribute('data-dot') === 'contentlet'
                ) {
                    // Log the event target
                    console.log('Pointer entered:', event.target);
                    const target = event.target

                    const { x, y, width, height } = target.getBoundingClientRect();

                    const contentletPayload = JSON.parse(target.dataset?.['content'] ?? '{}');

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
                }
            });
        };
    }
}

init();`