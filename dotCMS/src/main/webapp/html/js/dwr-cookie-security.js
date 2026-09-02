/**
 * DWR Cookie Security Enhancement
 * Configurable security attributes for DWRSESSIONID cookie
 * for enhanced security compliance (banking/enterprise requirements).
 *
 * Configuration is provided via window.dwrCookieSecurityConfig object set by JSP.
 * Defaults to disabled (no changes) to avoid affecting existing customers.
 *
 * This script must be loaded BEFORE dwr/engine.js
 */
(function() {
    'use strict';

    // Read configuration from window object (set by JSP)
    var config = window.dwrCookieSecurityConfig || {};
    var enableSecure = config.secure === true;
    var sameSiteValue = config.sameSite || '';

    // If both disabled, don't intercept at all
    if (!enableSecure && !sameSiteValue) {
        if (console && console.debug) {
            console.debug('DWR Cookie Security: Disabled via configuration (DWRSESSIONID_SECURE=false, DWRSESSIONID_SAMESITE empty)');
        }
        return;
    }

    // Store the original cookie descriptor
    var cookieDesc = Object.getOwnPropertyDescriptor(Document.prototype, 'cookie') ||
                     Object.getOwnPropertyDescriptor(HTMLDocument.prototype, 'cookie');

    if (!cookieDesc || !cookieDesc.configurable) {
        console.warn('DWR Cookie Security: Unable to intercept document.cookie - descriptor not configurable');
        return;
    }

    // Override document.cookie setter to intercept DWRSESSIONID
    Object.defineProperty(document, 'cookie', {
        get: function() {
            return cookieDesc.get.call(document);
        },
        set: function(val) {
            // Intercept DWRSESSIONID cookie setting
            if (val && typeof val === 'string' && val.indexOf('DWRSESSIONID=') === 0) {
                var attributesAdded = [];

                // Add Secure attribute if enabled and not already present
                if (enableSecure && val.indexOf('Secure') === -1) {
                    val += '; Secure';
                    attributesAdded.push('Secure');
                }

                // Add SameSite attribute if configured and not already present
                if (sameSiteValue && val.indexOf('SameSite') === -1) {
                    val += '; SameSite=' + sameSiteValue;
                    attributesAdded.push('SameSite=' + sameSiteValue);
                }

                if (console && console.info && attributesAdded.length > 0) {
                    console.info('DWR Cookie Security: Enhanced DWRSESSIONID cookie with: ' + attributesAdded.join(', '));
                }
            }
            return cookieDesc.set.call(document, val);
        },
        enumerable: true,
        configurable: true
    });

    if (console && console.info) {
        console.info('DWR Cookie Security: Interceptor initialized (Secure=' + enableSecure + ', SameSite=' + sameSiteValue + ')');
    }
})();
