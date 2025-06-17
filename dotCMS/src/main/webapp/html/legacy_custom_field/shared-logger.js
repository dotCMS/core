/**
 * DotCMS Legacy Custom Field - Shared Logger Utility
 *
 * Centralized logging system with configurable levels for all legacy custom field modules.
 * This prevents code duplication and provides consistent logging across modules.
 */

/**
 * Logging Levels
 */
const LOG_LEVELS = {
    ERROR: 0,   // Only errors
    WARN: 1,    // Errors and warnings
    INFO: 2,    // Errors, warnings, and important info
    DEBUG: 3,   // All logs including debug info
    TRACE: 4    // Everything including trace logs
};

/**
 * Shared Logging Configuration
 */
const SHARED_LOGGER_CONFIG = {
    logLevel: LOG_LEVELS.ERROR,     // Default log level
    enableLogging: true            // Master logging switch
};

/**
 * Creates a logger instance for a specific module
 * @param {string} moduleName - Name of the module (e.g., 'FieldInterceptor', 'IframeHeight')
 * @returns {Object} Logger instance with error, warn, info, debug, trace methods
 */
function createLogger(moduleName) {
    const modulePrefix = `[${moduleName}]`;

    return {
        error: (message, ...args) => {
            if (SHARED_LOGGER_CONFIG.enableLogging && SHARED_LOGGER_CONFIG.logLevel >= LOG_LEVELS.ERROR) {
                console.error(`❌ ${modulePrefix} ${message}`, ...args);
            }
        },
        warn: (message, ...args) => {
            if (SHARED_LOGGER_CONFIG.enableLogging && SHARED_LOGGER_CONFIG.logLevel >= LOG_LEVELS.WARN) {
                console.warn(`⚠️ ${modulePrefix} ${message}`, ...args);
            }
        },
        info: (message, ...args) => {
            if (SHARED_LOGGER_CONFIG.enableLogging && SHARED_LOGGER_CONFIG.logLevel >= LOG_LEVELS.INFO) {
                console.log(`ℹ️ ${modulePrefix} ${message}`, ...args);
            }
        },
        debug: (message, ...args) => {
            if (SHARED_LOGGER_CONFIG.enableLogging && SHARED_LOGGER_CONFIG.logLevel >= LOG_LEVELS.DEBUG) {
                console.log(`🔧 ${modulePrefix} ${message}`, ...args);
            }
        },
        trace: (message, ...args) => {
            if (SHARED_LOGGER_CONFIG.enableLogging && SHARED_LOGGER_CONFIG.logLevel >= LOG_LEVELS.TRACE) {
                console.log(`🔍 ${modulePrefix} ${message}`, ...args);
            }
        }
    };
}

/**
 * Set log level for all modules
 * @param {string|number} level - Log level (string name or numeric value)
 */
function setGlobalLogLevel(level) {
    if (typeof level === 'string') {
        level = LOG_LEVELS[level.toUpperCase()];
    }
    if (level !== undefined && level >= 0 && level <= 4) {
        SHARED_LOGGER_CONFIG.logLevel = level;
        console.log(`📝 [SharedLogger] Global log level set to: ${Object.keys(LOG_LEVELS)[level]}`);
    } else {
        console.error('❌ Invalid log level. Use: ERROR, WARN, INFO, DEBUG, TRACE or 0-4');
    }
}

/**
 * Enable/disable logging globally
 * @param {boolean} enabled - Whether logging should be enabled
 */
function setLoggingEnabled(enabled) {
    SHARED_LOGGER_CONFIG.enableLogging = !!enabled;
    console.log(`📝 [SharedLogger] Logging ${enabled ? 'enabled' : 'disabled'}`);
}

// Export for use in other modules
window.DotLegacyLogger = {
    LOG_LEVELS,
    createLogger,
    setGlobalLogLevel,
    setLoggingEnabled,
    getConfig: () => SHARED_LOGGER_CONFIG
};
