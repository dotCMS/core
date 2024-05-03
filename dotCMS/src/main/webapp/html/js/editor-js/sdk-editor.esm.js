/**
 * Actions send to the dotcms editor
 *
 * @export
 * @enum {number}
 */ var CUSTOMER_ACTIONS;
(function(CUSTOMER_ACTIONS) {
    /**
     * Tell the dotcms editor that page change
     */ CUSTOMER_ACTIONS["NAVIGATION_UPDATE"] = "set-url";
    /**
     * Send the element position of the rows, columnsm containers and contentlets
     */ CUSTOMER_ACTIONS["SET_BOUNDS"] = "set-bounds";
    /**
     * Send the information of the hovered contentlet
     */ CUSTOMER_ACTIONS["SET_CONTENTLET"] = "set-contentlet";
    /**
     * Tell the editor that the page is being scrolled
     */ CUSTOMER_ACTIONS["IFRAME_SCROLL"] = "scroll";
    /**
     * Tell the editor that the page has stopped scrolling
     */ CUSTOMER_ACTIONS["IFRAME_SCROLL_END"] = "scroll-end";
    /**
     * Ping the editor to see if the page is inside the editor
     */ CUSTOMER_ACTIONS["PING_EDITOR"] = "ping-editor";
    /**
     * Tell the editor to init the inline editing editor.
     */ CUSTOMER_ACTIONS["INIT_INLINE_EDITING"] = "init-inline-editing";
    /**
     * Tell the editor to open the Copy-contentlet dialog
     * To copy a content and then edit it inline.
     */ CUSTOMER_ACTIONS["COPY_CONTENTLET_INLINE_EDITING"] = "copy-contentlet-inline-editing";
    /**
     * Tell the editor to save inline edited contentlet
     */ CUSTOMER_ACTIONS["UPDATE_CONTENTLET_INLINE_EDITING"] = "update-contentlet-inline-editing";
    /**
     * Tell the editor to trigger a menu reorder
     */ CUSTOMER_ACTIONS["REORDER_MENU"] = "reorder-menu";
    CUSTOMER_ACTIONS["NOOP"] = "noop";
})(CUSTOMER_ACTIONS || (CUSTOMER_ACTIONS = {}));
/**
 * Post message to dotcms page editor
 *
 * @export
 * @template T
 * @param {PostMessageProps<T>} message
 */ function postMessageToEditor(message) {
    window.parent.postMessage(message, "*");
}

/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
 */ var NOTIFY_CUSTOMER;
(function(NOTIFY_CUSTOMER) {
    /**
     * Request to page to reload
     */ NOTIFY_CUSTOMER["EMA_RELOAD_PAGE"] = "ema-reload-page";
    /**
     * Request the bounds for the elements
     */ NOTIFY_CUSTOMER["EMA_REQUEST_BOUNDS"] = "ema-request-bounds";
    /**
     * Received pong from the editor
     */ NOTIFY_CUSTOMER["EMA_EDITOR_PONG"] = "ema-editor-pong";
    /**
     * Received scroll event trigger from the editor
     */ NOTIFY_CUSTOMER["EMA_SCROLL_INSIDE_IFRAME"] = "scroll-inside-iframe";
})(NOTIFY_CUSTOMER || (NOTIFY_CUSTOMER = {}));

/**
 * Bound information for a contentlet.
 *
 * @interface ContentletBound
 */ /**
 * Calculates the bounding information for each page element within the given containers.
 *
 * @export
 * @param {HTMLDivElement[]} containers
 * @return {*} An array of objects containing the bounding information for each page element.
 */ function getPageElementBound(containers) {
    return containers.map(function(container) {
        var containerRect = container.getBoundingClientRect();
        var contentlets = Array.from(container.querySelectorAll('[data-dot-object="contentlet"]'));
        return {
            x: containerRect.x,
            y: containerRect.y,
            width: containerRect.width,
            height: containerRect.height,
            payload: JSON.stringify({
                container: getContainerData(container)
            }),
            contentlets: getContentletsBound(containerRect, contentlets)
        };
    });
}
/**
 * An array of objects containing the bounding information for each contentlet inside a container.
 *
 * @export
 * @param {DOMRect} containerRect
 * @param {HTMLDivElement[]} contentlets
 * @return {*}
 */ function getContentletsBound(containerRect, contentlets) {
    return contentlets.map(function(contentlet) {
        var _contentlet_dataset, _contentlet_dataset1, _contentlet_dataset2, _contentlet_dataset3, _contentlet_dataset4, _contentlet_dataset5;
        var contentletRect = contentlet.getBoundingClientRect();
        return {
            x: 0,
            y: contentletRect.y - containerRect.y,
            width: contentletRect.width,
            height: contentletRect.height,
            payload: JSON.stringify({
                container: ((_contentlet_dataset = contentlet.dataset) === null || _contentlet_dataset === void 0 ? void 0 : _contentlet_dataset["dotContainer"]) ? JSON.parse((_contentlet_dataset1 = contentlet.dataset) === null || _contentlet_dataset1 === void 0 ? void 0 : _contentlet_dataset1["dotContainer"]) : getClosestContainerData(contentlet),
                contentlet: {
                    identifier: (_contentlet_dataset2 = contentlet.dataset) === null || _contentlet_dataset2 === void 0 ? void 0 : _contentlet_dataset2["dotIdentifier"],
                    title: (_contentlet_dataset3 = contentlet.dataset) === null || _contentlet_dataset3 === void 0 ? void 0 : _contentlet_dataset3["dotTitle"],
                    inode: (_contentlet_dataset4 = contentlet.dataset) === null || _contentlet_dataset4 === void 0 ? void 0 : _contentlet_dataset4["dotInode"],
                    contentType: (_contentlet_dataset5 = contentlet.dataset) === null || _contentlet_dataset5 === void 0 ? void 0 : _contentlet_dataset5["dotType"]
                }
            })
        };
    });
}
/**
 * Get container data from VTLS.
 *
 * @export
 * @param {HTMLElement} container
 * @return {*}
 */ function getContainerData(container) {
    var _container_dataset, _container_dataset1, _container_dataset2, _container_dataset3;
    return {
        acceptTypes: ((_container_dataset = container.dataset) === null || _container_dataset === void 0 ? void 0 : _container_dataset["dotAcceptTypes"]) || "",
        identifier: ((_container_dataset1 = container.dataset) === null || _container_dataset1 === void 0 ? void 0 : _container_dataset1["dotIdentifier"]) || "",
        maxContentlets: ((_container_dataset2 = container.dataset) === null || _container_dataset2 === void 0 ? void 0 : _container_dataset2["maxContentlets"]) || "",
        uuid: ((_container_dataset3 = container.dataset) === null || _container_dataset3 === void 0 ? void 0 : _container_dataset3["dotUuid"]) || ""
    };
}
/**
 * Get the closest container data from the contentlet.
 *
 * @export
 * @param {Element} element
 * @return {*}
 */ function getClosestContainerData(element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    var container = element.closest('[data-dot-object="container"]');
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
/**
 * Find the closest contentlet element based on HTMLElement.
 *
 * @export
 * @param {(HTMLElement | null)} element
 * @return {*}
 */ function findDotElement(element) {
    var _element_dataset, _element_dataset1;
    if (!element) return null;
    if ((element === null || element === void 0 ? void 0 : (_element_dataset = element.dataset) === null || _element_dataset === void 0 ? void 0 : _element_dataset["dotObject"]) === "contentlet" || (element === null || element === void 0 ? void 0 : (_element_dataset1 = element.dataset) === null || _element_dataset1 === void 0 ? void 0 : _element_dataset1["dotObject"]) === "container" && element.children.length === 0) {
        return element;
    }
    return findDotElement(element === null || element === void 0 ? void 0 : element["parentElement"]);
}
function findVTLData(target) {
    var vltElements = target.querySelectorAll('[data-dot-object="vtl-file"]');
    if (!vltElements.length) {
        return null;
    }
    return Array.from(vltElements).map(function(vltElement) {
        var _vltElement_dataset, _vltElement_dataset1;
        return {
            inode: (_vltElement_dataset = vltElement.dataset) === null || _vltElement_dataset === void 0 ? void 0 : _vltElement_dataset["dotInode"],
            name: (_vltElement_dataset1 = vltElement.dataset) === null || _vltElement_dataset1 === void 0 ? void 0 : _vltElement_dataset1["dotUrl"]
        };
    });
}

/**
 * Default reload function that reloads the current window.
 */ var defaultReloadFn = function() {
    return window.location.reload();
};
/**
 * Configuration object for the DotCMSPageEditor.
 */ var pageEditorConfig = {
    onReload: defaultReloadFn
};
/**
 * Sets the bounds of the containers in the editor.
 * Retrieves the containers from the DOM and sends their position data to the editor.
 * @private
 * @memberof DotCMSPageEditor
 */ function setBounds() {
    var containers = Array.from(document.querySelectorAll('[data-dot-object="container"]'));
    var positionData = getPageElementBound(containers);
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.SET_BOUNDS,
        payload: positionData
    });
}
/**
 * Reloads the page and triggers the onReload callback if it exists in the config object.
 */ function reloadPage() {
    pageEditorConfig === null || pageEditorConfig === void 0 ? void 0 : pageEditorConfig.onReload();
}
/**
 * Listens for editor messages and performs corresponding actions based on the received message.
 *
 * @private
 * @memberof DotCMSPageEditor
 */ function listenEditorMessages() {
    var messageCallback = function(event) {
        switch(event.data){
            case NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS:
                {
                    setBounds();
                    break;
                }
            case NOTIFY_CUSTOMER.EMA_RELOAD_PAGE:
                {
                    reloadPage();
                    break;
                }
        }
        if (event.data.name === NOTIFY_CUSTOMER.EMA_SCROLL_INSIDE_IFRAME) {
            var scrollY = event.data.direction === "up" ? -120 : 120;
            window.scrollBy({
                left: 0,
                top: scrollY,
                behavior: "smooth"
            });
        }
    };
    window.addEventListener("message", messageCallback);
}
/**
 * Listens for pointer move events and extracts information about the hovered contentlet.
 *
 * @private
 * @memberof DotCMSPageEditor
 */ function listenHoveredContentlet() {
    var pointerMoveCallback = function(event) {
        var _foundElement_dataset, _foundElement_dataset1, _foundElement_dataset2, _foundElement_dataset3, _foundElement_dataset4, _foundElement_dataset5, _foundElement_dataset6, _foundElement_dataset7, // Here extract dot-container from contentlet if is Headless
        // or search in parent container if is VTL
        _foundElement_dataset8, _foundElement_dataset9;
        var foundElement = findDotElement(event.target);
        if (!foundElement) return;
        var _foundElement_getBoundingClientRect = foundElement.getBoundingClientRect(), x = _foundElement_getBoundingClientRect.x, y = _foundElement_getBoundingClientRect.y, width = _foundElement_getBoundingClientRect.width, height = _foundElement_getBoundingClientRect.height;
        var isContainer = ((_foundElement_dataset = foundElement.dataset) === null || _foundElement_dataset === void 0 ? void 0 : _foundElement_dataset["dotObject"]) === "container";
        var contentletForEmptyContainer = {
            identifier: "TEMP_EMPTY_CONTENTLET",
            title: "TEMP_EMPTY_CONTENTLET",
            contentType: "TEMP_EMPTY_CONTENTLET_TYPE",
            inode: "TEMPY_EMPTY_CONTENTLET_INODE",
            widgetTitle: "TEMP_EMPTY_CONTENTLET",
            baseType: "TEMP_EMPTY_CONTENTLET",
            onNumberOfPages: 1
        };
        var contentlet = {
            identifier: (_foundElement_dataset1 = foundElement.dataset) === null || _foundElement_dataset1 === void 0 ? void 0 : _foundElement_dataset1["dotIdentifier"],
            title: (_foundElement_dataset2 = foundElement.dataset) === null || _foundElement_dataset2 === void 0 ? void 0 : _foundElement_dataset2["dotTitle"],
            inode: (_foundElement_dataset3 = foundElement.dataset) === null || _foundElement_dataset3 === void 0 ? void 0 : _foundElement_dataset3["dotInode"],
            contentType: (_foundElement_dataset4 = foundElement.dataset) === null || _foundElement_dataset4 === void 0 ? void 0 : _foundElement_dataset4["dotType"],
            baseType: (_foundElement_dataset5 = foundElement.dataset) === null || _foundElement_dataset5 === void 0 ? void 0 : _foundElement_dataset5["dotBasetype"],
            widgetTitle: (_foundElement_dataset6 = foundElement.dataset) === null || _foundElement_dataset6 === void 0 ? void 0 : _foundElement_dataset6["dotWidgetTitle"],
            onNumberOfPages: (_foundElement_dataset7 = foundElement.dataset) === null || _foundElement_dataset7 === void 0 ? void 0 : _foundElement_dataset7["dotOnNumberOfPages"]
        };
        var vtlFiles = findVTLData(foundElement);
        var contentletPayload = {
            container: ((_foundElement_dataset8 = foundElement.dataset) === null || _foundElement_dataset8 === void 0 ? void 0 : _foundElement_dataset8["dotContainer"]) ? JSON.parse((_foundElement_dataset9 = foundElement.dataset) === null || _foundElement_dataset9 === void 0 ? void 0 : _foundElement_dataset9["dotContainer"]) : getClosestContainerData(foundElement),
            contentlet: isContainer ? contentletForEmptyContainer : contentlet,
            vtlFiles: vtlFiles
        };
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
            payload: {
                x: x,
                y: y,
                width: width,
                height: height,
                payload: contentletPayload
            }
        });
    };
    document.addEventListener("pointermove", pointerMoveCallback);
}
/**
 * Attaches a scroll event listener to the window
 * and sends a message to the editor when the window is scrolled.
 *
 * @private
 * @memberof DotCMSPageEditor
 */ function scrollHandler() {
    var scrollCallback = function() {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL
        });
        window.lastScrollYPosition = window.scrollY;
    };
    var scrollEndCallback = function() {
        postMessageToEditor({
            action: CUSTOMER_ACTIONS.IFRAME_SCROLL_END
        });
    };
    window.addEventListener("scroll", scrollCallback);
    window.addEventListener("scrollend", scrollEndCallback);
}
/**
 * Restores the scroll position of the window when an iframe is loaded.
 * Only used in VTL Pages.
 * @export
 */ function preserveScrollOnIframe() {
    var preserveScrollCallback = function() {
        window.scrollTo(0, window.lastScrollYPosition);
    };
    window.addEventListener("load", preserveScrollCallback);
}
/**
 * Sends a ping message to the editor.
 *
 */ function pingEditor() {
    postMessageToEditor({
        action: CUSTOMER_ACTIONS.PING_EDITOR
    });
}

/**
 * Checks if the code is running inside an editor.
 * @returns {boolean} Returns true if the code is running inside an editor, otherwise false.
 */ function isInsideEditor() {
    if (typeof window === "undefined") {
        return false;
    }
    return window.parent !== window;
}

/**
 * This is the main entry point for the SDK VTL.
 * This is added to VTL Script in the EditPage
 *
 * @remarks
 * This module sets up the necessary listeners and functionality for the SDK VTL.
 * It checks if the script is running inside the editor and then initializes the client by pinging the editor,
 * listening for editor messages, hovered contentlet changes, and content changes.
 *
 */ if (isInsideEditor()) {
    pingEditor();
    listenEditorMessages();
    scrollHandler();
    preserveScrollOnIframe();
    listenHoveredContentlet();
}
