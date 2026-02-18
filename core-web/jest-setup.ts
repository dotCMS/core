/**
 * Jest setup file for global test environment configuration
 * This file is executed before each test file runs
 */

/**
 * Polyfill for structuredClone in Jest/JSDOM environment
 *
 * structuredClone is a native JavaScript function (ES2021) available in:
 * - Node.js 17+
 * - Modern browsers (Chrome 98+, Firefox 94+, Safari 15.4+)
 *
 * However, Jest uses JSDOM which doesn't include this API by default.
 * This polyfill provides a basic implementation using JSON serialization.
 *
 * Note: This is sufficient for test scenarios. Production code uses the
 * native browser implementation which handles more complex objects.
 */
if (typeof global.structuredClone === 'undefined') {
    global.structuredClone = function structuredClone<T>(obj: T): T {
        // Use JSON serialization for basic deep cloning
        // This handles: objects, arrays, primitives, Date (as ISO string)
        // Limitations: no circular refs, functions, symbols, undefined values
        return JSON.parse(JSON.stringify(obj));
    };
}
