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
     * Ping the editor to see if the page is inside the editor
     */ CUSTOMER_ACTIONS["PING_EDITOR"] = "ping-editor";
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
 */ function findContentletElement(element) {
    var _element_dataset, _element_dataset1;
    if (!element) return null;
    if ((element === null || element === void 0 ? void 0 : (_element_dataset = element.dataset) === null || _element_dataset === void 0 ? void 0 : _element_dataset["dotObject"]) === "contentlet" || (element === null || element === void 0 ? void 0 : (_element_dataset1 = element.dataset) === null || _element_dataset1 === void 0 ? void 0 : _element_dataset1["dotObject"]) === "container" && element.children.length === 0) {
        return element;
    }
    return findContentletElement(element === null || element === void 0 ? void 0 : element["parentElement"]);
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
        var _target_dataset, _target_dataset1, _target_dataset2, _target_dataset3, _target_dataset4, _target_dataset5, _target_dataset6, // Here extract dot-container from contentlet if is Headless
        // or search in parent container if is VTL
        _target_dataset7, _target_dataset8;
        var target = findContentletElement(event.target);
        if (!target) return;
        var _target_getBoundingClientRect = target.getBoundingClientRect(), x = _target_getBoundingClientRect.x, y = _target_getBoundingClientRect.y, width = _target_getBoundingClientRect.width, height = _target_getBoundingClientRect.height;
        var isEmptyContainer = ((_target_dataset = target.dataset) === null || _target_dataset === void 0 ? void 0 : _target_dataset["dotObject"]) === "container";
        var contentletForEmptyContainer = {
            identifier: "TEMP_EMPTY_CONTENTLET",
            title: "TEMP_EMPTY_CONTENTLET",
            contentType: "TEMP_EMPTY_CONTENTLET_TYPE",
            inode: "TEMPY_EMPTY_CONTENTLET_INODE",
            widgetTitle: "TEMP_EMPTY_CONTENTLET",
            onNumberOfPages: 1
        };
        var contentlet = {
            identifier: (_target_dataset1 = target.dataset) === null || _target_dataset1 === void 0 ? void 0 : _target_dataset1["dotIdentifier"],
            title: (_target_dataset2 = target.dataset) === null || _target_dataset2 === void 0 ? void 0 : _target_dataset2["dotTitle"],
            inode: (_target_dataset3 = target.dataset) === null || _target_dataset3 === void 0 ? void 0 : _target_dataset3["dotInode"],
            contentType: (_target_dataset4 = target.dataset) === null || _target_dataset4 === void 0 ? void 0 : _target_dataset4["dotType"],
            widgetTitle: (_target_dataset5 = target.dataset) === null || _target_dataset5 === void 0 ? void 0 : _target_dataset5["dotWidgetTitle"],
            onNumberOfPages: (_target_dataset6 = target.dataset) === null || _target_dataset6 === void 0 ? void 0 : _target_dataset6["dotOnNumberOfPages"]
        };
        var vtlFiles = findVTLData(target);
        var contentletPayload = {
            container: ((_target_dataset7 = target.dataset) === null || _target_dataset7 === void 0 ? void 0 : _target_dataset7["dotContainer"]) ? JSON.parse((_target_dataset8 = target.dataset) === null || _target_dataset8 === void 0 ? void 0 : _target_dataset8["dotContainer"]) : getClosestContainerData(target),
            contentlet: isEmptyContainer ? contentletForEmptyContainer : contentlet,
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
    window.addEventListener("scroll", scrollCallback);
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
