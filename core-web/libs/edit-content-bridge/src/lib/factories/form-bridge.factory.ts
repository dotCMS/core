import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { AngularFormBridge } from '../bridges/angular-form-bridge';
import { DojoFormBridge } from '../bridges/dojo-form-bridge';
import { FormBridge } from '../interfaces/form-bridge.interface';

/**
 * Configuration interface for Angular form bridge implementation
 * @interface
 */
interface AngularConfig {
    type: 'angular';
    form: FormGroup;
    zone: NgZone;
}

/**
 * Configuration interface for Dojo form bridge implementation
 * @interface
 */
interface DojoConfig {
    type: 'dojo';
}

/**
 * Union type representing all possible bridge configurations
 * @type {AngularConfig | DojoConfig}
 */
type BridgeConfig = AngularConfig | DojoConfig;

/**
 * Factory function that creates a form bridge instance based on the provided configuration
 * @param config - The configuration object that determines which bridge implementation to create
 * @returns {FormBridge} A configured form bridge instance
 */
export function createFormBridge(config: BridgeConfig): FormBridge {
    if (config.type === 'angular') {
        return AngularFormBridge.getInstance(config.form, config.zone);
    }

    return new DojoFormBridge();
}
