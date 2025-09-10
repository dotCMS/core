/**
 * DotCMS Field Interceptors Manager
 *
 * This module handles the creation and management of field interceptors for legacy custom fields.
 * It provides smart interceptors for input values and dynamic field creation management.
 */

/**
 * Configuration for Field Interceptors
 */
const INTERCEPTOR_CONFIG = {
    // Performance configuration
    retryDelay: 100,
    maxRetries: 3,
    interceptorTimeout: {
        immediate: 100,
        short: 500,
        long: 1000
    },
    // Normal sync timeouts (when Angular is available)
    syncTimeouts: [100, 500, 1000],
    // Fallback timeouts (when Angular is not available)
    fallbackTimeouts: [500, 1000, 2000]
};



/**
 * DotFieldInterceptorManager
 *
 * Manages field interceptors for legacy custom fields, handling:
 * 1. Smart value interceptors for input synchronization
 * 2. Dynamic input creation and management
 * 3. Integration with Angular form system
 */
class DotFieldInterceptorManager {
    constructor() {
        this.allFields = [];
        this.contentlet = {};
        this.bodyElement = null;
        this.mutationObserver = null;
        this.isInitialized = false;
        this.syncScheduled = false;
    }

    /**
     * Initializes the field interceptor manager with field definitions and contentlet data
     * @param {Array} fields - Array of field definitions
     * @param {Object} contentlet - Current contentlet data
     */
    init(fields, contentlet) {
        if (this.isInitialized) {
            return;
        }

        this.allFields = fields || [];
        this.contentlet = contentlet || {};
        this.bodyElement = document.querySelector('body');
        this.isInitialized = true;

        this.createInitialHiddenInputs();
        this.installGlobalInterceptors();
    }

    /**
     * Unified method to sync field values with Angular API
     * @param {string} specificVariable - If provided, syncs only this field
     * @param {string} specificValue - Value for the specific field
     */
    syncFieldValues(specificVariable = null, specificValue = null) {
        let syncCount = 0;
        let errorCount = 0;

        const fieldsToSync = specificVariable ?
            [{ variable: specificVariable }] :
            this.allFields;

        fieldsToSync.forEach(({ variable }) => {
            let foundValue = specificValue;
            let source = specificValue ? 'provided value' : '';


            if (!foundValue) {
                const hiddenInput = document.getElementById(variable);

                // Method 1: Check the exact DOM element that VTL sets
                const vtlTargetElement = document.getElementById(variable);
                if (vtlTargetElement && vtlTargetElement.value && vtlTargetElement !== hiddenInput) {
                    foundValue = vtlTargetElement.value;
                    source = 'VTL target element';
                }

                // Method 2: Check DOM input by name/id
                if (!foundValue) {
                    const domInput = document.querySelector(`input[name="${variable}"]`);
                    if (domInput && domInput.value && domInput !== hiddenInput) {
                        foundValue = domInput.value;
                        source = 'DOM input';
                    }
                }

                // Method 3: Check Dojo widget
                if (!foundValue && typeof dijit !== 'undefined') {
                    const widget = dijit.byId(variable);
                    if (widget && widget.get) {
                        const widgetValue = widget.get('value');
                        if (widgetValue) {
                            foundValue = widgetValue;
                            source = 'Dojo widget';
                        }
                    }
                }

                // Method 4: Check "box" pattern widget
                if (!foundValue && typeof dijit !== 'undefined') {
                    const boxWidget = dijit.byId(variable + 'box');
                    if (boxWidget && boxWidget.get) {
                        const boxValue = boxWidget.get('value');
                        if (boxValue) {
                            foundValue = boxValue;
                            source = 'Dojo box widget';
                        }
                    }
                }
            }

            // Perform sync if value found
            if (foundValue) {
                const hiddenInput = document.getElementById(variable);
                if (hiddenInput) {
                    const currentHiddenValue = hiddenInput.value;
                    const shouldForceSync = !hiddenInput._angularSynced;

                    // Smart initial sync: only sync if there's a real difference
                    const hasRealDifference = currentHiddenValue !== foundValue;
                    const isInitialSyncNeeded = shouldForceSync && hasRealDifference;

                    if (hasRealDifference || isInitialSyncNeeded) {
                        hiddenInput.value = foundValue;

                        if (window.DotCustomFieldApi) {
                            try {
                                window.DotCustomFieldApi.set(variable, foundValue);
                                hiddenInput._angularSynced = true;
                                syncCount++;
                            } catch (error) {
                                console.error(`Error calling DotCustomFieldApi.set for ${variable}:`, error);
                                errorCount++;
                            }
                        } else {
                            console.error('DotCustomFieldApi not available!');
                            errorCount++;
                        }
                    } else if (shouldForceSync && !hasRealDifference) {
                        hiddenInput._angularSynced = true;
                    }
                }
            }
        });


    }





    /**
     * Creates a hidden input element for field tracking
     * @param {string} variable - Field variable name
     * @param {string} value - Initial field value
     * @returns {HTMLInputElement} Created input element with interceptor
     */
    createHiddenInput(variable, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = variable;
        input.id = variable;
        input.setAttribute('dojoType', 'dijit.form.TextBox');
        input.value = value || '';
        this.bodyElement.appendChild(input);
        return this.addSmartInterceptor(input, variable);
    }

    /**
     * Adds smart value change interceptor to input element
     * @param {HTMLInputElement} input - Input element to intercept
     * @param {string} variable - Field variable name
     * @returns {HTMLInputElement} Input element with interceptor
     */
    addSmartInterceptor(input, variable) {
        if (input._dotIntercepted) return input;

        let isAngularUpdate = false;
        const valueDescriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');

        try {
            Object.defineProperty(input, 'value', {
                get() { return valueDescriptor.get.apply(this); },
                set(value) {
                    const oldValue = valueDescriptor.get.apply(this);
                    valueDescriptor.set.apply(this, [value]);
                    if (oldValue !== value && !isAngularUpdate) {
                        if (window.DotCustomFieldApi) {
                            window.DotCustomFieldApi.set(variable, value);
                        }
                    }
                },
                configurable: true
            });

            input.setFromAngular = value => {
                isAngularUpdate = true;
                input.value = value;
                isAngularUpdate = false;
            };

            input._dotIntercepted = true;
        } catch (error) {
            console.error(`Failed to intercept input: ${variable} - ${error.message}`);
        }

        return input;
    }

    /**
     * Intercepts input elements and adds smart interceptors
     * @param {HTMLInputElement[]} inputs - Array of input elements to intercept
     * @returns {number} Count of intercepted inputs
     */
    interceptInputs(inputs) {
        let interceptedCount = 0;

        inputs.forEach(input => {
            const variable = input.name || input.id;
            if (variable && this.allFields.some(f => f.variable === variable) && !input._dotIntercepted) {
                input.setAttribute('data-angular-tracked', 'true');
                this.addSmartInterceptor(input, variable);
                interceptedCount++;
            }
        });

        return interceptedCount;
    }

    /**
     * Finds and intercepts existing input elements in the DOM
     */
    interceptExistingInputs() {
        const inputs = Array.from(document.querySelectorAll('input'));
        this.interceptInputs(inputs);
    }

    /**
     * Installs global interceptors for input tracking and value changes
     */
    installGlobalInterceptors() {
        // Intercept setAttribute calls
        const originalSetAttribute = HTMLInputElement.prototype.setAttribute;
        HTMLInputElement.prototype.setAttribute = function(name, value) {
            const oldValue = this.value;
            originalSetAttribute.call(this, name, value);
            if (name === 'value' && oldValue !== value && this.hasAttribute('data-angular-tracked')) {
                const variable = this.name || this.id;
                if (variable && window.DotCustomFieldApi) {
                    window.DotCustomFieldApi.set(variable, value);
                }
            }
        };

        // Run interceptor on existing inputs with delays
        const timeouts = INTERCEPTOR_CONFIG.interceptorTimeout;
        setTimeout(() => this.interceptExistingInputs(), timeouts.immediate);
        setTimeout(() => this.interceptExistingInputs(), timeouts.short);
        setTimeout(() => this.interceptExistingInputs(), timeouts.long);

        // Watch for new inputs being added
        this.setupMutationObserver();
    }

    /**
     * Sets up DOM mutation observer to track dynamically added inputs
     */
    setupMutationObserver() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
        }

        this.mutationObserver = new MutationObserver(mutations => {
            const newInputs = [];

            for (const mutation of mutations) {
                if (mutation.type === 'childList') {
                    for (const node of mutation.addedNodes) {
                        if (node.nodeType === 1) {
                            const inputs = node.tagName === 'INPUT' ? [node] : node.querySelectorAll?.('input') || [];
                            newInputs.push(...inputs);
                        }
                    }
                }
            }

            if (newInputs.length > 0) {
                this.interceptInputs(newInputs);
            }
        });

        this.mutationObserver.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    /**
     * Creates initial hidden inputs for all field definitions
     */
    createInitialHiddenInputs() {
        this.allFields.forEach(({ variable }) => {
            this.createHiddenInput(variable, this.contentlet[variable] || "");
        });

        this.setupEventHandlers();
        this.waitForAngularAndSync();
    }

    /**
     * Sets up unified blur and change event handlers for field synchronization
     */
    setupEventHandlers() {
        const handleInputEvent = (event, eventType) => {
            const target = event.target;

            if (target && target.tagName === 'INPUT') {
                const variable = target.name || target.id;

                // Direct field match
                if (variable && this.allFields.some(f => f.variable === variable)) {
                    this.syncFieldValues(variable, target.value);
                }
                // Widget input (like cachettlbox -> cachettl)
                else if (variable && variable.endsWith('box')) {
                    const baseVariable = variable.replace(/box$/, '');
                    if (this.allFields.some(f => f.variable === baseVariable)) {
                        this.syncFieldValues(baseVariable, target.value);

                        // Delayed sync for hidden field
                        setTimeout(() => {
                            const hiddenField = document.getElementById(baseVariable);
                            if (hiddenField && hiddenField.value) {
                                this.syncFieldValues(baseVariable, hiddenField.value);
                            }
                        }, 100);
                    }
                }
            }
        };

        document.addEventListener('blur', (event) => handleInputEvent(event, 'blur'), true);
        document.addEventListener('change', (event) => handleInputEvent(event, 'change'), true);
    }

    /**
     * Schedules synchronization with staggered timeouts to prevent multiple simultaneous syncs
     */
    scheduleSync() {
        if (this.syncScheduled) return;

        this.syncScheduled = true;

        INTERCEPTOR_CONFIG.syncTimeouts.forEach(timeout => {
            setTimeout(() => {
                this.syncFieldValues();
                // Reset flag after last sync
                if (timeout === INTERCEPTOR_CONFIG.syncTimeouts[INTERCEPTOR_CONFIG.syncTimeouts.length - 1]) {
                    this.syncScheduled = false;
                }
            }, timeout);
        });
    }

    /**
     * Waits for Angular API to be ready, then triggers initial field synchronization
     */
    waitForAngularAndSync() {
        if (window.DotCustomFieldApi && window.DotCustomFieldApi.ready) {
            window.DotCustomFieldApi.ready((api) => {
                this.scheduleSync();
            });
        } else {
            // Fallback with longer timeouts
            INTERCEPTOR_CONFIG.fallbackTimeouts.forEach(timeout => {
                setTimeout(() => this.syncFieldValues(), timeout);
            });
        }
    }

    /**
     * Sets up integration with Dojo framework and DWR utilities
     */
    setupDojoIntegration() {
        if (typeof dojo === 'undefined') {
            return;
        }

        dojo.addOnLoad(() => {
            if (typeof dwr !== 'undefined') {
                dojo.global.DWRUtil = dwr.util;
                dojo.global.DWREngine = dwr.engine;
                dwr.engine.setErrorHandler(this.DWRErrorHandler);
                dwr.engine.setWarningHandler(this.DWRErrorHandler);
            }

            window.DotCustomFieldApi?.ready(() => {
                this.allFields.forEach(({ variable }) => {
                    window.DotCustomFieldApi.onChangeField?.(variable, value => {
                        const dijitWidget = dijit.byId(variable);
                        if (dijitWidget?.setValue) {
                            dijitWidget.setValue(value);
                        }
                    });
                });
            });
        });
    }

    /**
     * Handles DWR (Direct Web Remoting) errors
     * @param {string} msg - Error message
     * @param {Error} e - Error object
     */
    DWRErrorHandler(msg, e) {
        console.error(`DWR Error: ${msg}`, e);
    }

    /**
     * Cleans up resources and disconnects observers
     */
    cleanup() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
            this.mutationObserver = null;
        }

        this.isInitialized = false;
    }

    /**
     * Returns current configuration object
     * @returns {Object} Configuration settings
     */
    getConfig() {
        return INTERCEPTOR_CONFIG;
    }


}

/**
 * Cleanup handlers for page unload
 */
const setupInterceptorCleanup = () => {
    const cleanupHandler = () => {
        if (window.DotFieldInterceptorManager_Instance) {
            window.DotFieldInterceptorManager_Instance.cleanup();
        }
    };

    window.addEventListener('beforeunload', cleanupHandler);
    window.addEventListener('pagehide', cleanupHandler);
};

/**
 * Initialize Field Interceptor Manager
 *
 * @param {Array} fields - Array of field definitions
 * @param {Object} contentlet - Current contentlet data
 * @returns {DotFieldInterceptorManager} - The initialized manager instance
 */
const initializeFieldInterceptors = (fields, contentlet) => {
    if (!window.DotFieldInterceptorManager_Instance) {
        window.DotFieldInterceptorManager_Instance = new DotFieldInterceptorManager();
        setupInterceptorCleanup();
    }

    window.DotFieldInterceptorManager_Instance.init(fields, contentlet);
    window.DotFieldInterceptorManager_Instance.setupDojoIntegration();

    return window.DotFieldInterceptorManager_Instance;
};

// Export for use in JSP
window.DotFieldInterceptors = {
    DotFieldInterceptorManager,
    initializeFieldInterceptors,
    setupInterceptorCleanup,
    INTERCEPTOR_CONFIG,




};
