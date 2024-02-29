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
    CUSTOMER_ACTIONS["CONTENT_CHANGE"] = "content-change";
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

function getPageElementBound(containers) {
    return containers.map(function(container) {
        var _container_dataset;
        var containerRect = container.getBoundingClientRect();
        var contentlets = Array.from(container.querySelectorAll('[data-dot-object="contentlet"]'));
        var _container_dataset_content;
        return {
            x: containerRect.x,
            y: containerRect.y,
            width: containerRect.width,
            height: containerRect.height,
            payload: (_container_dataset_content = (_container_dataset = container.dataset) === null || _container_dataset === void 0 ? void 0 : _container_dataset["content"]) !== null && _container_dataset_content !== void 0 ? _container_dataset_content : {
                container: getContainerData(container)
            },
            contentlets: getContentletsBound(containerRect, contentlets)
        };
    });
}
function getContentletsBound(containerRect, contentlets) {
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
//Used to get container data from VTLS.
function getContainerData(container) {
    var _container_dataset, _container_dataset1, _container_dataset2, _container_dataset3;
    return {
        acceptTypes: (_container_dataset = container.dataset) === null || _container_dataset === void 0 ? void 0 : _container_dataset["dotAcceptTypes"],
        identifier: (_container_dataset1 = container.dataset) === null || _container_dataset1 === void 0 ? void 0 : _container_dataset1["dotIdentifier"],
        maxContentlets: (_container_dataset2 = container.dataset) === null || _container_dataset2 === void 0 ? void 0 : _container_dataset2["maxContentlets"],
        uuid: (_container_dataset3 = container.dataset) === null || _container_dataset3 === void 0 ? void 0 : _container_dataset3["dotUuid"]
    };
}
function getClosestContainerData(element) {
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
//TODO: Fix typeLater
// USed to find contentlets and later add the listeners "onHover"
function findContentletElement(element) {
    var _element_dataset;
    if (!element) return null;
    if (element.dataset && ((_element_dataset = element.dataset) === null || _element_dataset === void 0 ? void 0 : _element_dataset["dotObject"]) === "contentlet") {
        return element;
    } else {
        return findContentletElement(element === null || element === void 0 ? void 0 : element["parentElement"]);
    }
}

function _array_like_to_array(arr, len) {
    if (len == null || len > arr.length) len = arr.length;
    for(var i = 0, arr2 = new Array(len); i < len; i++)arr2[i] = arr[i];
    return arr2;
}
function _array_without_holes(arr) {
    if (Array.isArray(arr)) return _array_like_to_array(arr);
}
function _class_call_check(instance, Constructor) {
    if (!(instance instanceof Constructor)) {
        throw new TypeError("Cannot call a class as a function");
    }
}
function _defineProperties(target, props) {
    for(var i = 0; i < props.length; i++){
        var descriptor = props[i];
        descriptor.enumerable = descriptor.enumerable || false;
        descriptor.configurable = true;
        if ("value" in descriptor) descriptor.writable = true;
        Object.defineProperty(target, descriptor.key, descriptor);
    }
}
function _create_class(Constructor, protoProps, staticProps) {
    if (protoProps) _defineProperties(Constructor.prototype, protoProps);
    if (staticProps) _defineProperties(Constructor, staticProps);
    return Constructor;
}
function _define_property(obj, key, value) {
    if (key in obj) {
        Object.defineProperty(obj, key, {
            value: value,
            enumerable: true,
            configurable: true,
            writable: true
        });
    } else {
        obj[key] = value;
    }
    return obj;
}
function _iterable_to_array(iter) {
    if (typeof Symbol !== "undefined" && iter[Symbol.iterator] != null || iter["@@iterator"] != null) return Array.from(iter);
}
function _non_iterable_spread() {
    throw new TypeError("Invalid attempt to spread non-iterable instance.\\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.");
}
function _to_consumable_array(arr) {
    return _array_without_holes(arr) || _iterable_to_array(arr) || _unsupported_iterable_to_array(arr) || _non_iterable_spread();
}
function _unsupported_iterable_to_array(o, minLen) {
    if (!o) return;
    if (typeof o === "string") return _array_like_to_array(o, minLen);
    var n = Object.prototype.toString.call(o).slice(8, -1);
    if (n === "Object" && o.constructor) n = o.constructor.name;
    if (n === "Map" || n === "Set") return Array.from(n);
    if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _array_like_to_array(o, minLen);
}
var DotCMSPageEditor = /*#__PURE__*/ function() {
    function DotCMSPageEditor(config) {
        _class_call_check(this, DotCMSPageEditor);
        _define_property(this, "config", void 0);
        _define_property(this, "subscriptions", []);
        _define_property(this, "isInsideEditor", void 0);
        this.config = config !== null && config !== void 0 ? config : {
            onReload: defaultReloadFn
        };
    }
    _create_class(DotCMSPageEditor, [
        {
            key: "init",
            value: function init() {
                console.log("SdkDotPageEditor init!");
                this.isInsideEditor = this.checkIfInsideEditor();
                if (this.isInsideEditor) {
                    this.listenEditorMessages();
                    this.listenHoveredContentlet();
                    this.scrollHandler();
                    this.listenContentChange();
                }
            }
        },
        {
            key: "destroy",
            value: function destroy() {
                console.log("SdkDotPageEditor Headless destroy.");
                this.subscriptions.forEach(function(subscription) {
                    if (subscription.type === "listener") {
                        var _window;
                        (_window = window) === null || _window === void 0 ? void 0 : _window.removeEventListener(subscription.event, subscription.callback);
                    }
                    if (subscription.type === "observer") {
                        subscription.observer.disconnect();
                    }
                });
            }
        },
        {
            key: "updateNavigation",
            value: function updateNavigation(pathname) {
                postMessageToEditor({
                    action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
                    payload: {
                        url: pathname === "/" ? "index" : pathname === null || pathname === void 0 ? void 0 : pathname.replace("/", "")
                    }
                });
            }
        },
        {
            key: "listenEditorMessages",
            value: function listenEditorMessages() {
                var _this = this;
                var _window;
                var messageCallback = function(event) {
                    switch(event.data){
                        case NOTIFY_CUSTOMER.EMA_REQUEST_BOUNDS:
                            {
                                _this.setBounds();
                                break;
                            }
                        case NOTIFY_CUSTOMER.EMA_RELOAD_PAGE:
                            {
                                _this.reloadPage();
                                break;
                            }
                    }
                };
                (_window = window) === null || _window === void 0 ? void 0 : _window.addEventListener("message", messageCallback);
                this.subscriptions.push({
                    type: "listener",
                    event: "message",
                    callback: messageCallback
                });
            }
        },
        {
            key: "listenHoveredContentlet",
            value: function listenHoveredContentlet() {
                var pointerMoveCallback = function(event) {
                    var // Here extract dot-container from contentlet if is Headless
                    // or search in parent container if is VTL
                    _target_dataset, _target_dataset1, _target_dataset2, _target_dataset3, _target_dataset4, _target_dataset5;
                    var target = findContentletElement(event.target);
                    if (!target) return;
                    var _target_getBoundingClientRect = target.getBoundingClientRect(), x = _target_getBoundingClientRect.x, y = _target_getBoundingClientRect.y, width = _target_getBoundingClientRect.width, height = _target_getBoundingClientRect.height;
                    var contentletPayload = {
                        container: ((_target_dataset = target.dataset) === null || _target_dataset === void 0 ? void 0 : _target_dataset["dotContainer"]) ? JSON.parse((_target_dataset1 = target.dataset) === null || _target_dataset1 === void 0 ? void 0 : _target_dataset1["dotContainer"]) : getClosestContainerData(target),
                        contentlet: {
                            identifier: (_target_dataset2 = target.dataset) === null || _target_dataset2 === void 0 ? void 0 : _target_dataset2["dotIdentifier"],
                            title: (_target_dataset3 = target.dataset) === null || _target_dataset3 === void 0 ? void 0 : _target_dataset3["dotTitle"],
                            inode: (_target_dataset4 = target.dataset) === null || _target_dataset4 === void 0 ? void 0 : _target_dataset4["dotInode"],
                            contentType: (_target_dataset5 = target.dataset) === null || _target_dataset5 === void 0 ? void 0 : _target_dataset5["dotType"]
                        }
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
                this.subscriptions.push({
                    type: "listener",
                    event: "pointermove",
                    callback: pointerMoveCallback
                });
            }
        },
        {
            key: "scrollHandler",
            value: function scrollHandler() {
                var _window;
                var scrollCallback = function() {
                    postMessageToEditor({
                        action: CUSTOMER_ACTIONS.IFRAME_SCROLL
                    });
                };
                (_window = window) === null || _window === void 0 ? void 0 : _window.addEventListener("scroll", scrollCallback);
                this.subscriptions.push({
                    type: "listener",
                    event: "scroll",
                    callback: scrollCallback
                });
            }
        },
        {
            key: "checkIfInsideEditor",
            value: function checkIfInsideEditor() {
                var _window;
                if (((_window = window) === null || _window === void 0 ? void 0 : _window.parent) === window) {
                    return false;
                }
                postMessageToEditor({
                    action: CUSTOMER_ACTIONS.PING_EDITOR
                });
                return true;
            }
        },
        {
            key: "listenContentChange",
            value: function listenContentChange() {
                var observer = new MutationObserver(function(mutationsList) {
                    var _iteratorNormalCompletion = true, _didIteratorError = false, _iteratorError = undefined;
                    try {
                        for(var _iterator = mutationsList[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true){
                            var _step_value = _step.value, addedNodes = _step_value.addedNodes, removedNodes = _step_value.removedNodes, type = _step_value.type;
                            if (type === "childList") {
                                var didNodesChanged = _to_consumable_array(Array.from(addedNodes)).concat(_to_consumable_array(Array.from(removedNodes))).filter(function(node) {
                                    var _node_dataset;
                                    return ((_node_dataset = node.dataset) === null || _node_dataset === void 0 ? void 0 : _node_dataset["dot"]) === "contentlet";
                                }).length;
                                if (didNodesChanged) {
                                    postMessageToEditor({
                                        action: CUSTOMER_ACTIONS.CONTENT_CHANGE
                                    });
                                }
                            }
                        }
                    } catch (err) {
                        _didIteratorError = true;
                        _iteratorError = err;
                    } finally{
                        try {
                            if (!_iteratorNormalCompletion && _iterator.return != null) {
                                _iterator.return();
                            }
                        } finally{
                            if (_didIteratorError) {
                                throw _iteratorError;
                            }
                        }
                    }
                });
                observer.observe(document, {
                    childList: true,
                    subtree: true
                });
                this.subscriptions.push({
                    type: "observer",
                    observer: observer
                });
            }
        },
        {
            key: "setBounds",
            value: function setBounds() {
                var containers = Array.from(document.querySelectorAll('[data-dot-object="container"]'));
                var positionData = getPageElementBound(containers);
                postMessageToEditor({
                    action: CUSTOMER_ACTIONS.SET_BOUNDS,
                    payload: positionData
                });
            }
        },
        {
            key: "reloadPage",
            value: function reloadPage() {
                this.config.onReload();
            }
        }
    ]);
    return DotCMSPageEditor;
}();
var defaultReloadFn = function() {
    return window.location.reload();
};
var sdkDotPageEditor = {
    createClient: function(config) {
        var dotCMSPageEditor = new DotCMSPageEditor(config);
        dotCMSPageEditor.init();
        return dotCMSPageEditor;
    }
};

var client = sdkDotPageEditor.createClient();
client.init();
