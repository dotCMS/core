/**
 * Central export point for all DotCMS Analytics models
 */

import { ANALYTICS_WINDOWS_ACTIVE_KEY, ANALYTICS_WINDOWS_CLEANUP_KEY } from '../constants';

// Data models (Analytics data structures)
export * from './data.model';

// Event models (Event types and structures)
export * from './event.model';

// Request models (HTTP request/response structures)
export * from './request.model';

// Library models (Internal SDK structures)
export * from './library.model';

// Extend Window interface to include our custom properties
declare global {
    interface Window {
        [ANALYTICS_WINDOWS_CLEANUP_KEY]?: () => void;
        [ANALYTICS_WINDOWS_ACTIVE_KEY]?: boolean;
    }
}
