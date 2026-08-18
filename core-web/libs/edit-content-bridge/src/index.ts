export {
    createFormBridge,
    pushFormBridge,
    popFormBridge
} from './lib/factories/form-bridge.factory';

// `export type`, not `export`: these are interfaces, and consumers compiled with
// `isolatedModules` cannot tell that from a plain re-export.
export type { FormBridge } from './lib/interfaces/form-bridge.interface';
export type { DotCustomFieldApiWindow } from './lib/interfaces/dot-custom-field-api-window.interface';
