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
    // Performance configuration
    heightCheckThrottle: 100,      // ms - throttle for height calculations
    resizeTimeout: 200,            // ms - timeout for resize events
    mutationTimeout: 100,          // ms - timeout for mutation events
    eventTimeout: 100,             // ms - timeout for user interaction events
    loadTimeout: 50,               // ms - timeout for load detection
    safetyTimeout: 3000,           // ms - safety timeout for load detection
    maxRetries: 3,                 // max retries for failed operations
    enableDebug: false             // Deprecated - use shared logger instead
};

/**
 * Logger instance for this module
 */
const iframeLogger = window.DotLegacyLogger?.createLogger('IframeHeight') || {
    error: (msg) => console.error(`❌ [IframeHeight] ${msg}`),
    warn: (msg) => console.warn(`⚠️ [IframeHeight] ${msg}`),
    info: (msg) => console.log(`ℹ️ [IframeHeight] ${msg}`),
    debug: (msg) => console.log(`🔧 [IframeHeight] ${msg}`),
    trace: (msg) => console.log(`🔍 [IframeHeight] ${msg}`)
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
        this._heightChangeCount = 0;
    }

    init(iframeId, inode) {
        this.iframeId = iframeId;
        this.inode = inode || 'new';

        if (window.DotIframeLoadDetector_Instance) {
            iframeLogger.warn(`Another instance already exists! Current: ${window.DotIframeLoadDetector_Instance}, New: ${this.instanceId}`);
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
        iframeLogger.info(`Iframe fully loaded. Initial height: ${initialHeight}px`);
        this.sendHeightToParent(initialHeight);
        this.startHeightMonitoring();
    }

    sendHeightToParent(height) {
        if (window.parent && window.parent !== window && height > 0 && this.iframeId) {
            const uniqueId = `${this.iframeId}-${this.inode}`;

            iframeLogger.debug(`Sending height to parent: ${height}px for iframe: ${uniqueId}`);

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
            this._heightChangeCount++;
            iframeLogger.trace(`Height changed from ${this.lastHeight} to ${currentHeight} (change #${this._heightChangeCount})`);
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

        iframeLogger.info('Starting height monitoring with multiple detection methods');

        // ResizeObserver with configurable throttling
        if (window.ResizeObserver) {
            const throttledCheck = this.createThrottledFunction(
                () => this.checkHeightChange(),
                IFRAME_CONFIG.heightCheckThrottle
            );
            const resizeObserver = new ResizeObserver(throttledCheck);
            resizeObserver.observe(document.body);
            this.observers.push(resizeObserver);
            iframeLogger.debug('ResizeObserver monitoring enabled');
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
        iframeLogger.debug('MutationObserver monitoring enabled');

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
        iframeLogger.debug(`Event monitoring enabled for: ${eventTypes.join(', ')}`);

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

        iframeLogger.info(`Height monitoring started with ${this.observers.length} methods`);
    }

    stopHeightMonitoring() {
        iframeLogger.info(`Stopping height monitoring (${this._heightChangeCount} total height changes detected)`);
        this.observers.forEach(observer => observer.disconnect?.());
        this.observers = [];
        this.timeouts.forEach(timeout => clearTimeout(timeout));
        this.timeouts.clear();
        this._heightCache = null;
    }

    setupLoadDetection() {
        iframeLogger.debug(`Setting up load detection (readyState: ${document.readyState})`);

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
                iframeLogger.warn('Safety timeout reached - forcing load detection');
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
        iframeLogger.debug('Cleaning up resources before page unload...');

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
        iframeLogger.debug('Cleaning up resources on page hide...');

        if (window.DotIframeLoadDetector && window.DotIframeLoadDetector.stopHeightMonitoring) {
            window.DotIframeLoadDetector.stopHeightMonitoring();
        }
    });

    // Cleanup on visibility change (when tab becomes hidden)
    document.addEventListener('visibilitychange', () => {
        if (document.hidden) {
            iframeLogger.trace('Page became hidden, monitoring continues...');
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
            iframeLogger.info(`Initializing iframe height manager for ${iframeId} (inline mode)`);
            window.DotIframeLoadDetector = new DotIframeLoadDetector();
            window.DotIframeLoadDetector.init(iframeId, inode);
        } else {
            iframeLogger.info(`Iframe height manager initialized for ${iframeId} (modal mode - height disabled)`);
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
    IFRAME_CONFIG,

    // Set log level utility (delegates to shared logger)
    setLogLevel: (level) => {
        if (window.DotLegacyLogger) {
            window.DotLegacyLogger.setGlobalLogLevel(level);
        } else {
            console.error('❌ Shared logger not available');
        }
    }
};
