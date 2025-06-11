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
    enableLogging: true,
    retryDelay: 100,
    maxRetries: 3,
    interceptorTimeout: {
        immediate: 100,
        short: 500,
        long: 1000
    }
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
    }

    /**
     * Initialize the interceptor manager
     * @param {Array} fields - Array of field definitions
     * @param {Object} contentlet - Current contentlet data
     */
    init(fields, contentlet) {
        if (this.isInitialized) {
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('⚠️ Field interceptor manager already initialized');
            }
            return;
        }

        this.allFields = fields || [];
        this.contentlet = contentlet || {};
        this.bodyElement = document.querySelector('body');
        this.isInitialized = true;

        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('🔧 Initializing Field Interceptor Manager', {
                fieldsCount: this.allFields.length,
                contentletKeys: Object.keys(this.contentlet)
            });
        }



        this.createInitialHiddenInputs();
        this.installGlobalInterceptors();
    }

            /**
     * Simple method to sync all visible values
     */
    syncAllVisibleValues() {
        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('🔄 Simple sync: Checking all inputs for values...');
        }

        let syncCount = 0;

        this.allFields.forEach(({ variable }) => {
            let foundValue = null;
            let source = '';

            // Check hidden input we created
            const hiddenInput = document.getElementById(variable);

            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log(`🔍 Checking ${variable}:`);
            }

            // Method 1: Check the exact DOM element that VTL sets (dojo.byId)
            const vtlTargetElement = document.getElementById(variable);
            if (vtlTargetElement && vtlTargetElement.value && vtlTargetElement !== hiddenInput) {
                foundValue = vtlTargetElement.value;
                source = 'VTL target element';
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`  ✅ Found in VTL target: "${foundValue}"`);
                }
            }

            // Method 2: Check DOM input by name/id
            if (!foundValue) {
                const domInput = document.querySelector(`input[name="${variable}"]`);
                if (domInput && domInput.value && domInput !== hiddenInput) {
                    foundValue = domInput.value;
                    source = 'DOM input';
                    if (INTERCEPTOR_CONFIG.enableLogging) {
                        console.log(`  ✅ Found in DOM input: "${foundValue}"`);
                    }
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
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`  ✅ Found in Dojo widget: "${foundValue}"`);
                        }
                    }
                }
            }

            // Method 4: Check "box" pattern widget (like cachettlbox -> cachettl)
            if (!foundValue && typeof dijit !== 'undefined') {
                const boxWidget = dijit.byId(variable + 'box');
                if (boxWidget && boxWidget.get) {
                    const boxValue = boxWidget.get('value');
                    if (boxValue) {
                        foundValue = boxValue;
                        source = 'Dojo box widget';
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`  ✅ Found in Dojo box widget: "${foundValue}"`);
                        }
                    }
                }
            }

                        // If we found a value, sync it
            if (foundValue && hiddenInput) {
                const currentHiddenValue = hiddenInput.value;
                const shouldForceSync = !hiddenInput._angularSynced;

                if (currentHiddenValue !== foundValue || shouldForceSync) {
                    if (INTERCEPTOR_CONFIG.enableLogging) {
                        const reason = shouldForceSync ? "FORCE INITIAL SYNC" : "VALUE CHANGED";
                        console.log(`📋 Syncing ${variable}: "${currentHiddenValue}" → "${foundValue}" (${source}) [${reason}]`);
                    }

                    hiddenInput.value = foundValue;

                    if (window.DotCustomFieldApi) {
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`🔄 Calling DotCustomFieldApi.set("${variable}", "${foundValue}")`);
                        }

                        try {
                            window.DotCustomFieldApi.set(variable, foundValue);
                            hiddenInput._angularSynced = true;

                            if (INTERCEPTOR_CONFIG.enableLogging) {
                                console.log(`✅ Successfully called DotCustomFieldApi.set for ${variable}`);
                            }
                        } catch (error) {
                            if (INTERCEPTOR_CONFIG.enableLogging) {
                                console.error(`❌ Error calling DotCustomFieldApi.set for ${variable}:`, error);
                            }
                        }

                        // Verify it was set
                        setTimeout(() => {
                            if (window.DotCustomFieldApi.get) {
                                try {
                                    const verifyValue = window.DotCustomFieldApi.get(variable);
                                    if (INTERCEPTOR_CONFIG.enableLogging) {
                                        console.log(`🔍 Verification: DotCustomFieldApi.get("${variable}") = "${verifyValue}"`);
                                    }
                                } catch (error) {
                                    if (INTERCEPTOR_CONFIG.enableLogging) {
                                        console.log(`⚠️ Could not verify ${variable}:`, error);
                                    }
                                }
                            }
                        }, 100);
                    } else {
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`❌ DotCustomFieldApi not available!`);
                        }
                    }

                    syncCount++;
                } else if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`  ℹ️ Already synced: "${foundValue}"`);
                }
            } else if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log(`  ❌ No value found for ${variable}`);
            }
        });

        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log(`✅ Simple sync completed: ${syncCount} fields synchronized`);
        }
    }

    /**
     * Creates a hidden input field for a specific variable
     * @param {string} variable - The field variable name
     * @param {string} value - The initial value
     * @returns {HTMLInputElement} The created input element
     */
    createHiddenInput(variable, value) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = variable;
        input.id = variable;
        input.setAttribute('dojoType', 'dijit.form.TextBox');
        input.value = value || '';
        this.bodyElement.appendChild(input);
        this.addSmartInterceptor(input, variable);
        return input;
    }

    /**
     * Smart Interceptor for Input Values
     *
     * Intercepts and manages value changes on inputs to ensure proper
     * synchronization between Angular and legacy fields.
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
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`🔄 Programmatic value change detected on ${variable}: "${value}"`);
                        }
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

            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log(`✅ Smart interceptor added for: ${variable}`);
            }
        } catch (error) {
            console.warn('⚠️ Failed to intercept input:', variable, error.message);
        }

        return input;
    }

    /**
     * Intercepts existing inputs in the DOM
     */
    interceptExistingInputs() {
        document.querySelectorAll('input').forEach(input => {
            const variable = input.name || input.id;
            if (variable && this.allFields.some(f => f.variable === variable) && !input._dotIntercepted) {
                input.setAttribute('data-angular-tracked', 'true');
                this.addSmartInterceptor(input, variable);
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`🔗 Intercepted existing input: ${variable}`);
                }
            }
        });
    }

    /**
     * Global Input Interceptors
     *
     * Sets up global interceptors to handle:
     * 1. Dynamic input creation (via MutationObserver)
     * 2. Value changes through setAttribute
     */
    installGlobalInterceptors() {
        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('🛠️ Installing global interceptors...');
        }

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

        // Run interceptor on existing inputs with delays to catch dynamically created ones
        const timeouts = INTERCEPTOR_CONFIG.interceptorTimeout;
        setTimeout(() => this.interceptExistingInputs(), timeouts.immediate);
        setTimeout(() => this.interceptExistingInputs(), timeouts.short);
        setTimeout(() => this.interceptExistingInputs(), timeouts.long);

        // Watch for new inputs being added
        this.setupMutationObserver();

        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('✅ Global interceptors installed');
        }
    }

    /**
     * Sets up MutationObserver to watch for dynamically added inputs
     */
    setupMutationObserver() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
        }

        this.mutationObserver = new MutationObserver(mutations => {
            for (const mutation of mutations) {
                if (mutation.type === 'childList') {
                    for (const node of mutation.addedNodes) {
                        if (node.nodeType === 1) {
                            const inputs = node.tagName === 'INPUT' ? [node] : node.querySelectorAll?.('input') || [];
                            for (const input of inputs) {
                                const variable = input.name || input.id;
                                if (variable && this.allFields.some(f => f.variable === variable) && !input._dotIntercepted) {
                                    input.setAttribute('data-angular-tracked', 'true');
                                    this.addSmartInterceptor(input, variable);
                                    if (INTERCEPTOR_CONFIG.enableLogging) {
                                        console.log(`🆕 Intercepted new input: ${variable}`);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        this.mutationObserver.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    /**
     * Creates initial hidden inputs for all fields
     */
    createInitialHiddenInputs() {
        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('📝 Creating initial hidden inputs...');
        }

        this.allFields.forEach(({ variable }) => {
            const input = this.createHiddenInput(variable, this.contentlet[variable] || "");
            this.addSmartInterceptor(input, variable);
        });

                if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log(`✅ Created ${this.allFields.length} hidden inputs`);
        }

        // Debug Angular API immediately
        this.debugAngularAPI();

                // Set up blur event listener for field synchronization
        this.setupBlurEventHandlers();

        // Wait for Angular to be ready, then sync values
        this.waitForAngularAndSync();
    }

    /**
     * Sets up blur event handlers for field synchronization
     * This is the primary mechanism for detecting when users finish editing fields
     */
    setupBlurEventHandlers() {
        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('🎯 Setting up blur event handlers...');
            console.log('🔍 Available fields:', this.allFields.map(f => f.variable));
        }

                // Listen to all input events for debugging
        document.addEventListener('blur', (event) => {
            const target = event.target;
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('🔥 Blur event detected on:', target.tagName, target.id, target.name, target.value);
            }

            if (target && target.tagName === 'INPUT') {
                const variable = target.name || target.id;
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`🔍 Checking variable "${variable}" against fields:`, this.allFields.map(f => f.variable));
                }

                // Check if this is a direct field match
                if (variable && this.allFields.some(f => f.variable === variable)) {
                    if (INTERCEPTOR_CONFIG.enableLogging) {
                        console.log(`🎯 Direct blur event on ${variable}: "${target.value}"`);
                    }
                    this.syncFieldValue(variable, target.value);
                }
                // Check if this is a widget input (like cachettlbox -> cachettl)
                else if (variable && variable.endsWith('box')) {
                    const baseVariable = variable.replace(/box$/, '');
                    if (this.allFields.some(f => f.variable === baseVariable)) {
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`🎯 Widget blur event on ${variable} -> syncing ${baseVariable}: "${target.value}"`);
                        }
                        this.syncFieldValue(baseVariable, target.value);

                        // Also try to sync the actual hidden field value if it exists
                        setTimeout(() => {
                            const hiddenField = document.getElementById(baseVariable);
                            if (hiddenField && hiddenField.value) {
                                this.syncFieldValue(baseVariable, hiddenField.value);
                            }
                        }, 100); // Small delay to let Dojo update the hidden field
                    }
                } else {
                    if (INTERCEPTOR_CONFIG.enableLogging) {
                        console.log(`⚠️ Variable "${variable}" not found in fields or no variable detected`);
                    }
                }
            }
        }, true); // Use capture phase to catch blur events

        // Also listen for change events on the entire document
        document.addEventListener('change', (event) => {
            const target = event.target;
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('🔄 Change event detected on:', target.tagName, target.id, target.name, target.value);
            }

            if (target && target.tagName === 'INPUT') {
                const variable = target.name || target.id;
                if (variable && this.allFields.some(f => f.variable === variable)) {
                    if (INTERCEPTOR_CONFIG.enableLogging) {
                        console.log(`🔄 Change event on ${variable}: "${target.value}"`);
                    }
                    this.syncFieldValue(variable, target.value);
                }
            }
        }, true);

        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('✅ Blur and change event handlers set up');
        }
    }

        /**
     * Syncs a single field value with Angular
     */
    syncFieldValue(variable, value) {
        const hiddenInput = document.getElementById(variable);
        if (hiddenInput && hiddenInput.value !== value) {
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log(`📋 Syncing ${variable}: "${value}"`);
            }

            hiddenInput.value = value;

            if (window.DotCustomFieldApi) {
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`🔄 syncFieldValue: Calling DotCustomFieldApi.set("${variable}", "${value}")`);
                }
                window.DotCustomFieldApi.set(variable, value);

                // Verify it was set
                setTimeout(() => {
                    if (window.DotCustomFieldApi.get) {
                        const verifyValue = window.DotCustomFieldApi.get(variable);
                        if (INTERCEPTOR_CONFIG.enableLogging) {
                            console.log(`🔍 syncFieldValue verification: DotCustomFieldApi.get("${variable}") = "${verifyValue}"`);
                        }
                    }
                }, 100);
            } else {
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log(`❌ syncFieldValue: DotCustomFieldApi not available!`);
                }
            }
        }
    }

    /**
     * Sets up integration with Dojo framework
     */
    setupDojoIntegration() {
        if (typeof dojo === 'undefined') {
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('⚠️ Dojo not available, skipping Dojo integration');
            }
            return;
        }

        dojo.addOnLoad(() => {
            // Set up DWR utilities
            if (typeof dwr !== 'undefined') {
                dojo.global.DWRUtil = dwr.util;
                dojo.global.DWREngine = dwr.engine;
                dwr.engine.setErrorHandler(this.DWRErrorHandler);
                dwr.engine.setWarningHandler(this.DWRErrorHandler);
            }

            // Set up field change listeners for Dojo widgets
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

            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('✅ Dojo integration setup complete');
            }
        });
    }

    /**
     * DWR Error Handler
     */
    DWRErrorHandler(msg, e) {
        console.log('DWR Error:', msg, e);
    }

    /**
     * Cleanup method to disconnect observers and clear resources
     */
    cleanup() {
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
            this.mutationObserver = null;
        }

        if (INTERCEPTOR_CONFIG.enableLogging) {
            console.log('🧹 Field interceptor manager cleaned up');
        }

        this.isInitialized = false;
    }

    /**
     * Get current configuration
     */
    getConfig() {
        return INTERCEPTOR_CONFIG;
    }

        /**
     * Debug method to inspect DOM inputs
     */
    debugDOMInputs() {
        console.log('🔍 DEBUG: DOM Inputs Analysis');
        console.log('==============================');

        const allInputs = document.querySelectorAll('input');
        console.log(`📊 Found ${allInputs.length} input elements in DOM:`);

        allInputs.forEach((input, index) => {
            console.log(`  [${index}] ${input.tagName}`, {
                id: input.id || 'no-id',
                name: input.name || 'no-name',
                type: input.type,
                value: input.value || 'empty',
                dojoType: input.getAttribute('dojoType') || 'none',
                tracked: input.getAttribute('data-angular-tracked') || 'false'
            });
        });

        console.log('\n🎯 Fields we are looking for:', this.allFields.map(f => f.variable));
        console.log('==============================');
    }

        /**
     * Wait for Angular to be ready, then sync values
     */
    waitForAngularAndSync() {
        if (window.DotCustomFieldApi && window.DotCustomFieldApi.ready) {
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('⏳ Waiting for Angular to be ready...');
            }

            window.DotCustomFieldApi.ready((api) => {
                if (INTERCEPTOR_CONFIG.enableLogging) {
                    console.log('🎉 Angular is ready! API available:', api);
                    console.log('✅ DotCustomFieldApi.set exists:', typeof api.set);
                    console.log('✅ DotCustomFieldApi.get exists:', typeof api.get);
                }

                // Now sync all values
                setTimeout(() => this.syncAllVisibleValues(), 100);
                setTimeout(() => this.syncAllVisibleValues(), 500);
                setTimeout(() => this.syncAllVisibleValues(), 1000);
            });
        } else {
            if (INTERCEPTOR_CONFIG.enableLogging) {
                console.log('⚠️ DotCustomFieldApi.ready not available, falling back to timeouts');
            }
            // Fallback to timeouts if ready method doesn't exist
            setTimeout(() => this.syncAllVisibleValues(), 500);
            setTimeout(() => this.syncAllVisibleValues(), 1000);
            setTimeout(() => this.syncAllVisibleValues(), 2000);
        }
    }

    /**
     * Debug Angular API
     */
    debugAngularAPI() {
        console.log('🔍 DEBUG: Angular API Analysis');
        console.log('==============================');
        console.log('window.DotCustomFieldApi exists:', !!window.DotCustomFieldApi);
        if (window.DotCustomFieldApi) {
            console.log('DotCustomFieldApi.set exists:', typeof window.DotCustomFieldApi.set);
            console.log('DotCustomFieldApi.get exists:', typeof window.DotCustomFieldApi.get);
            console.log('DotCustomFieldApi.ready exists:', typeof window.DotCustomFieldApi.ready);
            console.log('DotCustomFieldApi.onChangeField exists:', typeof window.DotCustomFieldApi.onChangeField);
            console.log('DotCustomFieldApi object:', window.DotCustomFieldApi);

            // Try to get current values
            console.log('\n📋 Trying to get current field values:');
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
    // Debug utilities
    debugInputs: () => {
        if (window.DotFieldInterceptorManager_Instance) {
            window.DotFieldInterceptorManager_Instance.debugDOMInputs();
        }
    },
    // Simple sync trigger
    syncNow: () => {
        if (window.DotFieldInterceptorManager_Instance) {
            window.DotFieldInterceptorManager_Instance.syncAllVisibleValues();
        }
    },
    // Debug Angular API
    debugAngularAPI: () => {
        if (window.DotFieldInterceptorManager_Instance) {
            window.DotFieldInterceptorManager_Instance.debugAngularAPI();
        } else {
            console.log('❌ Field Interceptor Manager not initialized');
        }
    }
};
