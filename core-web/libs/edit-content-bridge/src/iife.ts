import { createFormBridge } from './lib/factories/form-bridge.factory';

import type { FormBridge } from './lib/interfaces/form-bridge.interface';

interface DotCustomFieldApiWindow extends Window {
    DotCustomFieldApi: FormBridge;
}

/**
 * Initializes and exposes the DojoFormBridge as an IIFE (Immediately Invoked Function Expression)
 * to be consumed by the Edit Content Dojo application.
 *
 * This code creates a bridge instance for Dojo and exposes its public API through the global
 * DotCustomFieldApi object on the window, making it accessible to custom fields in the legacy Dojo UI.
 */
if (typeof window !== 'undefined') {
    const bridge = createFormBridge({ type: 'dojo' });

    (window as unknown as DotCustomFieldApiWindow).DotCustomFieldApi = bridge;
}
