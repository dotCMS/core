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
    const isInsideEditor = checkIfInsideEditor();
    // console.log('isInsideEditor', isInsideEditor);
    if (isInsideEditor) {
        setTimeout(() => {
            listenHoveredContentlet();
        }, 100);
        scrollHandler();
        listenContentChange(); // We can use const observer = listenContentChange() to disconnect later
    }
}

function listenHoveredContentlet() {
    const contentletElements = document.querySelectorAll('[data-dot="contentlet"]');
    console.log("Contentlets: ",contentletElements);

    contentletElements.forEach((element) => {
        element.addEventListener('pointerenter', (event) => {
            // Your pointer enter event handler logic here
            // console.log('Pointer entered:', event.target);
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
    console.log("second option")
    const iframe = document.querySelector('iframe');
    console.log(iframe)
    if (iframe) {
        iframe.onload = function () {
            const iframeDocument = iframe.contentDocument || iframe.contentWindow?.document;
            console.log(iframeDocument);

            iframeDocument?.addEventListener('pointerenter', (event) => {
                console.log("event: ",event);
                if (
                    event.target instanceof HTMLElement &&
                    event.target.hasAttribute('data-dot') &&
                    event.target.getAttribute('data-dot') === 'contentlet'
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