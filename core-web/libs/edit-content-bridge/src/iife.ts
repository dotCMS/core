import { DotFormBridge } from './lib/edit-content-bridge';
interface DotCustomFieldApiWindow extends Window {
    DotCustomFieldApi: ReturnType<DotFormBridge['createPublicApi']>;
}
/**
 * Initializes and exposes the DotFormBridge public API as an IIFE (Immediately Invoked Function Expression)
 * to be consumed by the Edit Content Dojo application.
 *
 * This code creates a bridge instance for Dojo and exposes its public API through the global
 * DotCustomFieldApi object on the window, making it accessible to custom fields in the legacy Dojo UI.
 */
if (typeof window !== 'undefined') {
    const bridge = new DotFormBridge({ type: 'dojo' });

    (window as unknown as DotCustomFieldApiWindow).DotCustomFieldApi = bridge.createPublicApi();
}
