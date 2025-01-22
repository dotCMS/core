import { DotFormBridge } from './lib/edit-content-bridge';

if (typeof window !== 'undefined') {
    const bridge = new DotFormBridge({ type: 'dojo' });
    (window as any).DotCustomFieldApi = bridge.createPublicApi();
}
