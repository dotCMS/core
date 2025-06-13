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
    // Sync timeouts consolidados
    syncTimeouts: [100, 500, 1000]
};

/**
 * Logger instance for this module
 */
const fieldLogger = window.DotLegacyLogger?.createLogger('FieldInterceptor') || {
    error: (msg) => console.error(`❌ [FieldInterceptor] ${msg}`),
    warn: (msg) => console.warn(`⚠️ [FieldInterceptor] ${msg}`),
    info: (msg) => console.log(`ℹ️ [FieldInterceptor] ${msg}`),
    debug: (msg) => console.log(`🔧 [FieldInterceptor] ${msg}`),
    trace: (msg) => console.log(`🔍 [FieldInterceptor] ${msg}`)
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
            fieldLogger.warn('Field interceptor manager already initialized');
            return;
        }

        this.allFields = fields || [];
        this.contentlet = contentlet || {};
        this.bodyElement = document.querySelector('body');
        this.isInitialized = true;

        fieldLogger.info(`Initializing Field Interceptor Manager (${this.allFields.length} fields)`);

        this.createInitialHiddenInputs();
        this.installGlobalInterceptors();
    }

    /**
     * Unified method to sync field values with Angular API
     * @param {string} specificVariable - If provided, syncs only this field
     * @param {string} specificValue - Value for the specific field
     */
    syncFieldValues(specificVariable = null, specificValue = null) {
        fieldLogger.debug(specificVariable ?
            `Syncing specific field: ${specificVariable}` :
            'Starting field synchronization...'
        );

        let syncCount = 0;
        let errorCount = 0;

        const fieldsToSync = specificVariable ?
            [{ variable: specificVariable }] :
            this.allFields;

        fieldsToSync.forEach(({ variable }) => {
            let foundValue = specificValue;
            let source = specificValue ? 'provided value' : '';

            // Si no tenemos un valor específico, buscamos el valor actual
            if (!foundValue) {
                const hiddenInput = document.getElementById(variable);
                fieldLogger.trace(`Checking field: ${variable}`);

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

                    if (currentHiddenValue !== foundValue || shouldForceSync) {
                        const reason = shouldForceSync ? "FORCE INITIAL SYNC" : "VALUE CHANGED";
                        fieldLogger.debug(`Syncing ${variable}: "${currentHiddenValue}" → "${foundValue}" (${source}) [${reason}]`);

                        hiddenInput.value = foundValue;

                        if (window.DotCustomFieldApi) {
                            try {
                                window.DotCustomFieldApi.set(variable, foundValue);
                                hiddenInput._angularSynced = true;
                                syncCount++;

                                // Verification only in debug mode
                                this.verifySync(variable);
                            } catch (error) {
                                fieldLogger.error(`Error calling DotCustomFieldApi.set for ${variable}:`, error);
                                errorCount++;
                            }
                        } else {
                            fieldLogger.error('DotCustomFieldApi not available!');
                            errorCount++;
                        }
                    }
                }
            }
        });

        // Summary log
        if (syncCount > 0 || errorCount > 0) {
            fieldLogger.info(`Sync completed: ${syncCount} fields synchronized${errorCount > 0 ? `, ${errorCount} errors` : ''}`);
        }
    }

    /**
     * Syncs all visible field values with Angular
     */
    syncAllVisibleValues() {
        this.syncFieldValues();
    }

    /**
     * Syncs a specific field value with Angular
     * @param {string} variable - Field variable name
     * @param {string} value - Field value to sync
     */
    syncFieldValue(variable, value) {
        this.syncFieldValues(variable, value);
    }

    /**
     * Verifies field synchronization in debug mode only
     * @param {string} variable - Field variable to verify
     */
    verifySync(variable) {
        if (window.DotLegacyLogger?.getConfig().logLevel >= window.DotLegacyLogger?.LOG_LEVELS.DEBUG) {
            setTimeout(() => {
                if (window.DotCustomFieldApi.get) {
                    try {
                        const verifyValue = window.DotCustomFieldApi.get(variable);
                        fieldLogger.trace(`Verification: ${variable} = "${verifyValue}"`);
                    } catch (error) {
                        fieldLogger.warn(`Could not verify ${variable}: ${error.message}`);
                    }
                }
            }, 100);
        }
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
                        fieldLogger.debug(`Programmatic value change detected on ${variable}: "${value}"`);
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
            fieldLogger.debug(`Smart interceptor added for: ${variable}`);
        } catch (error) {
            fieldLogger.error(`Failed to intercept input: ${variable} - ${error.message}`);
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
        const interceptedCount = this.interceptInputs(inputs);

        if (interceptedCount > 0) {
            fieldLogger.debug(`Intercepted ${interceptedCount} existing inputs`);
        }
    }

    /**
     * Installs global interceptors for input tracking and value changes
     */
    installGlobalInterceptors() {
        fieldLogger.debug('Installing global interceptors...');

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

        fieldLogger.info('Global interceptors installed');
    }

    /**
     * Sets up DOM mutation observer to track dynamically added inputs
     */
    setupMutationObserver() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
        }

        let newInputCount = 0;
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
                newInputCount += this.interceptInputs(newInputs);
            }

            if (newInputCount > 0) {
                fieldLogger.debug(`Intercepted ${newInputCount} new dynamic inputs`);
                newInputCount = 0;
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
        fieldLogger.debug(`Creating ${this.allFields.length} initial hidden inputs...`);

        this.allFields.forEach(({ variable }) => {
            // ELIMINADA la llamada duplicada a addSmartInterceptor
            this.createHiddenInput(variable, this.contentlet[variable] || "");
        });

        // this.debugAngularAPI();
        this.setupEventHandlers();
        this.waitForAngularAndSync();
    }

    /**
     * Sets up unified blur and change event handlers for field synchronization
     */
    setupEventHandlers() {
        fieldLogger.debug('Setting up event handlers...');

        const handleInputEvent = (event, eventType) => {
            const target = event.target;
            fieldLogger.trace(`${eventType} event on: ${target.tagName} ${target.id} ${target.name} = "${target.value}"`);

            if (target && target.tagName === 'INPUT') {
                const variable = target.name || target.id;

                // Direct field match
                if (variable && this.allFields.some(f => f.variable === variable)) {
                    fieldLogger.debug(`Direct ${eventType} event on ${variable}: "${target.value}"`);
                    this.syncFieldValue(variable, target.value);
                }
                // Widget input (like cachettlbox -> cachettl)
                else if (variable && variable.endsWith('box')) {
                    const baseVariable = variable.replace(/box$/, '');
                    if (this.allFields.some(f => f.variable === baseVariable)) {
                        fieldLogger.debug(`Widget ${eventType} event on ${variable} -> syncing ${baseVariable}: "${target.value}"`);
                        this.syncFieldValue(baseVariable, target.value);

                        // Delayed sync for hidden field
                        setTimeout(() => {
                            const hiddenField = document.getElementById(baseVariable);
                            if (hiddenField && hiddenField.value) {
                                this.syncFieldValue(baseVariable, hiddenField.value);
                            }
                        }, 100);
                    }
                }
            }
        };

        // Event listeners unificados
        document.addEventListener('blur', (event) => handleInputEvent(event, 'blur'), true);
        document.addEventListener('change', (event) => handleInputEvent(event, 'change'), true);

        fieldLogger.info('Event handlers set up');
    }

    /**
     * Schedules synchronization with staggered timeouts to prevent multiple simultaneous syncs
     */
    scheduleSync() {
        if (this.syncScheduled) return;

        this.syncScheduled = true;

        INTERCEPTOR_CONFIG.syncTimeouts.forEach(timeout => {
            setTimeout(() => {
                this.syncAllVisibleValues();
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
            fieldLogger.debug('Waiting for Angular to be ready...');

            window.DotCustomFieldApi.ready((api) => {
                fieldLogger.info('Angular is ready! Starting initial sync...');
                fieldLogger.debug('API methods available:', {
                    set: typeof api.set,
                    get: typeof api.get
                });

                this.scheduleSync();
            });
        } else {
            fieldLogger.warn('DotCustomFieldApi.ready not available, using fallback timeouts');
            // Fallback con timeouts más largos
            [500, 1000, 2000].forEach(timeout => {
                setTimeout(() => this.syncAllVisibleValues(), timeout);
            });
        }
    }

    /**
     * Sets up integration with Dojo framework and DWR utilities
     */
    setupDojoIntegration() {
        if (typeof dojo === 'undefined') {
            fieldLogger.warn('Dojo not available, skipping Dojo integration');
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

            fieldLogger.info('Dojo integration setup complete');
        });
    }

    /**
     * Handles DWR (Direct Web Remoting) errors
     * @param {string} msg - Error message
     * @param {Error} e - Error object
     */
    DWRErrorHandler(msg, e) {
        fieldLogger.error(`DWR Error: ${msg}`, e);
    }

    /**
     * Cleans up resources and disconnects observers
     */
    cleanup() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
            this.mutationObserver = null;
        }

        fieldLogger.info('Field interceptor manager cleaned up');
        this.isInitialized = false;
    }

    /**
     * Returns current configuration object
     * @returns {Object} Configuration settings
     */
    getConfig() {
        return INTERCEPTOR_CONFIG;
    }

    /**
     * Debug utility to analyze Angular API availability and field values
     */
    debugAngularAPI() {
        if (window.DotLegacyLogger?.getConfig().logLevel < window.DotLegacyLogger?.LOG_LEVELS.DEBUG) return;

        console.log('🔍 DEBUG: Angular API Analysis');
        console.log('==============================');
        console.log('window.DotCustomFieldApi exists:', !!window.DotCustomFieldApi);
        if (window.DotCustomFieldApi) {
            console.log('API methods:', {
                set: typeof window.DotCustomFieldApi.set,
                get: typeof window.DotCustomFieldApi.get,
                ready: typeof window.DotCustomFieldApi.ready,
                onChangeField: typeof window.DotCustomFieldApi.onChangeField
            });

            console.log('\n📋 Current field values:');
            this.allFields.forEach(({ variable }) => {
                if (window.DotCustomFieldApi.get) {
                    try {
                        const value = window.DotCustomFieldApi.get(variable);
                        console.log(`  ${variable}: "${value}"`);
                    } catch (error) {
                        console.log(`  ${variable}: ERROR - ${error.message}`);
                    }
                }
            });
        } else {
            console.log('❌ DotCustomFieldApi NOT FOUND');
            console.log('Available window properties:', Object.keys(window).filter(k => k.includes('Dot')));
        }
        console.log('==============================');
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

    syncNow: () => {
        if (window.DotFieldInterceptorManager_Instance) {
            window.DotFieldInterceptorManager_Instance.syncAllVisibleValues();
        }
    },

    setLogLevel: (level) => {
        if (window.DotLegacyLogger) {
            window.DotLegacyLogger.setGlobalLogLevel(level);
        } else {
            console.error('❌ Shared logger not available');
        }
    }
};
