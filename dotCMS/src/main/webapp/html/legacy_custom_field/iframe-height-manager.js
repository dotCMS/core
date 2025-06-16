/**
 * DotCMS Iframe Height Manager
 *
 * This module handles automatic height adjustment of iframes in DotCMS custom fields.
 * It provides smooth resizing and multiple detection methods for accurate height calculations.
 */

/**
 * Configuration Object
 *
 * Centralized configuration for performance tuning and debugging
 */
const IFRAME_CONFIG = {
    heightCheckThrottle: 100,      // ms - throttle for height calculations
    resizeTimeout: 200,            // ms - timeout for resize events
    mutationTimeout: 100,          // ms - timeout for mutation events
    eventTimeout: 100,             // ms - timeout for user interaction events
    loadTimeout: 50,               // ms - timeout for load detection
    safetyTimeout: 3000,           // ms - safety timeout for load detection
    enableLogging: true,          // enable/disable console logging
    maxRetries: 3,                 // max retries for failed operations
    enableDebug: false             // enable/disable debug mode
};

/**
 * DotIframeLoadDetector
 *
 * Handles automatic height adjustment of the iframe based on its content.
 * Uses multiple detection methods (ResizeObserver, MutationObserver, events)
 * to ensure accurate height calculations and smooth resizing.
 */
class DotIframeLoadDetector {
    constructor() {
        this.isFullyLoaded = false;
        this.instanceId = Math.random().toString(36).substr(2, 9);
        this.lastHeight = null;
        this.observers = [];
        this.timeouts = new Set();
        this._heightCache = null;
        this._lastHeightCheck = 0;
        this._heightCheckThrottle = IFRAME_CONFIG.heightCheckThrottle;
        this.iframeId = null;
        this.inode = null;
    }

    init(iframeId, inode) {
        this.iframeId = iframeId;
        this.inode = inode || 'new';

        if (window.DotIframeLoadDetector_Instance) {
            if (IFRAME_CONFIG.enableLogging) {
                console.log('⚠️ WARNING: Another instance already exists!', window.DotIframeLoadDetector_Instance);
            }
        }
        window.DotIframeLoadDetector_Instance = this.instanceId;
        this.setupLoadDetection();
    }

    getCurrentHeight() {
        const now = Date.now();
        if (this._heightCache && now - this._lastHeightCheck < this._heightCheckThrottle) {
            return this._heightCache;
        }

        this._lastHeightCheck = now;
        this._heightCache = Math.max(
            document.body.scrollHeight || 0,
            document.body.offsetHeight || 0,
            document.documentElement.scrollHeight || 0,
            document.documentElement.offsetHeight || 0
        );
        return this._heightCache;
    }

    onFullyLoaded() {
        if (this.isFullyLoaded) return;
        this.isFullyLoaded = true;
        const initialHeight = this.getCurrentHeight();
        this.sendHeightToParent(initialHeight);
        this.startHeightMonitoring();
    }

    sendHeightToParent(height) {
        if (window.parent && window.parent !== window && height > 0 && this.iframeId) {
            const uniqueId = `${this.iframeId}-${this.inode}`;

            if (IFRAME_CONFIG.enableLogging) {
                console.log(`📤 Sending height to parent: ${height}px for iframe: ${uniqueId}`);
            }

            window.parent.postMessage({
                type: 'dotcms:iframe:resize',
                height,
                iframeId: uniqueId,
                fieldVariable: this.iframeId
            }, '*');
        }
    }

    checkHeightChange() {
        const currentHeight = this.getCurrentHeight();
        if (this.lastHeight !== currentHeight && currentHeight > 0) {
            if (IFRAME_CONFIG.enableLogging) {
                console.log(`🔄 Height changed from ${this.lastHeight} to ${currentHeight}`);
            }
            this.lastHeight = currentHeight;
            this.sendHeightToParent(currentHeight);
        }
    }

    createThrottledFunction(fn, delay) {
        let lastCall = 0;
        return (...args) => {
            const now = Date.now();
            if (now - lastCall >= delay) {
                lastCall = now;
                fn.apply(this, args);
            }
        };
    }

    startHeightMonitoring() {
        this.lastHeight = this.getCurrentHeight();

        if (IFRAME_CONFIG.enableLogging) {
            console.log('👀 Starting height monitoring...');
        }

        // ResizeObserver with configurable throttling
        if (window.ResizeObserver) {
            const throttledCheck = this.createThrottledFunction(
                () => this.checkHeightChange(),
                IFRAME_CONFIG.heightCheckThrottle
            );
            const resizeObserver = new ResizeObserver(throttledCheck);
            resizeObserver.observe(document.body);
            this.observers.push(resizeObserver);
        }

        // MutationObserver with configurable throttling
        const throttledMutationCheck = this.createThrottledFunction(
            () => this.checkHeightChange(),
            IFRAME_CONFIG.mutationTimeout
        );
        const mutationObserver = new MutationObserver(throttledMutationCheck);
        mutationObserver.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['style', 'class']
        });
        this.observers.push(mutationObserver);

        // Combined event handler with configurable throttling
        const throttledEventCheck = this.createThrottledFunction(
            () => this.checkHeightChange(),
            IFRAME_CONFIG.eventTimeout
        );
        const eventTypes = ['click', 'focus', 'input', 'change', 'keyup'];
        const eventHandler = () => {
            throttledEventCheck();
        };
        eventTypes.forEach(type => {
            document.addEventListener(type, eventHandler, { passive: true });
        });
        this.observers.push({
            disconnect: () => {
                eventTypes.forEach(type => {
                    document.removeEventListener(type, eventHandler);
                });
            }
        });

        // Window resize with configurable throttling
        const throttledResizeCheck = this.createThrottledFunction(
            () => this.checkHeightChange(),
            IFRAME_CONFIG.resizeTimeout
        );
        window.addEventListener('resize', throttledResizeCheck);
        this.observers.push({
            disconnect: () => {
                window.removeEventListener('resize', throttledResizeCheck);
            }
        });

        if (IFRAME_CONFIG.enableLogging) {
            console.log('✅ Height monitoring started with multiple methods');
        }
    }

    stopHeightMonitoring() {
        if (IFRAME_CONFIG.enableLogging) {
            console.log('🛑 Stopping height monitoring...');
        }
        this.observers.forEach(observer => observer.disconnect?.());
        this.observers = [];
        this.timeouts.forEach(timeout => clearTimeout(timeout));
        this.timeouts.clear();
        this._heightCache = null;
    }

    setupLoadDetection() {
        if (IFRAME_CONFIG.enableLogging) {
            console.log('📡 Setting up load detection...');
            console.log('📊 Current readyState:', document.readyState);
        }

        if (document.readyState === 'complete') {
            const timeout = setTimeout(() => this.onFullyLoaded(), IFRAME_CONFIG.loadTimeout);
            this.timeouts.add(timeout);
            return;
        }

        const loadHandler = () => {
            const timeout = setTimeout(() => this.onFullyLoaded(), IFRAME_CONFIG.loadTimeout);
            this.timeouts.add(timeout);
        };

        window.addEventListener('load', loadHandler);
        document.addEventListener('readystatechange', () => {
            if (document.readyState === 'complete') {
                loadHandler();
            }
        });

        const safetyTimeout = setTimeout(() => {
            if (!this.isFullyLoaded) {
                if (IFRAME_CONFIG.enableLogging) {
                    console.log('⏰ Safety timeout - forcing load detection');
                }
                this.onFullyLoaded();
            }
        }, IFRAME_CONFIG.safetyTimeout);
        this.timeouts.add(safetyTimeout);
    }
}

/**
 * Robust Cleanup Handler
 *
 * Ensures proper cleanup of all resources when the page unloads
 */
const setupCleanupHandlers = () => {
    // Cleanup before page unload
    window.addEventListener('beforeunload', () => {
        if (IFRAME_CONFIG.enableLogging) {
            console.log('🧹 Cleaning up resources before page unload...');
        }

        if (window.DotIframeLoadDetector && window.DotIframeLoadDetector.stopHeightMonitoring) {
            window.DotIframeLoadDetector.stopHeightMonitoring();
        }

        // Clean up any remaining global variables
        if (window.DotIframeLoadDetector_Instance) {
            delete window.DotIframeLoadDetector_Instance;
        }

        if (window.DotIframeLoadDetector_GlobalInit) {
            delete window.DotIframeLoadDetector_GlobalInit;
        }
    });

    // Cleanup on page hide (for mobile browsers)
    window.addEventListener('pagehide', () => {
        if (IFRAME_CONFIG.enableLogging) {
            console.log('🧹 Cleaning up resources on page hide...');
        }

        if (window.DotIframeLoadDetector && window.DotIframeLoadDetector.stopHeightMonitoring) {
            window.DotIframeLoadDetector.stopHeightMonitoring();
        }
    });

    // Cleanup on visibility change (when tab becomes hidden)
    document.addEventListener('visibilitychange', () => {
        if (document.hidden && IFRAME_CONFIG.enableLogging) {
            console.log('👁️ Page became hidden, monitoring continues...');
        }
    });
};

/**
 * Initialize Height Manager
 *
 * @param {string} iframeId - The iframe identifier
 * @param {string} inode - The content inode
 * @param {boolean} modalMode - Whether running in modal mode
 */
const initializeHeightManager = (iframeId, inode, modalMode = false) => {
    // Initialize DotIframeLoadDetector only once and only in inline mode
    if (!window.DotIframeLoadDetector_GlobalInit) {
        window.DotIframeLoadDetector_GlobalInit = true;
        if (!modalMode) {
            window.DotIframeLoadDetector = new DotIframeLoadDetector();
            window.DotIframeLoadDetector.init(iframeId, inode);
        } else {
            window.DotIframeLoadDetector = new DotIframeLoadDetector();
        }
    }

    // Initialize cleanup handlers
    setupCleanupHandlers();
};

// Export for use in JSP
window.DotIframeHeightManager = {
    DotIframeLoadDetector,
    initializeHeightManager,
    setupCleanupHandlers,
    IFRAME_CONFIG
};
