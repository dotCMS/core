import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { AngularFormBridge } from '../bridges/angular-form-bridge';
import { DojoFormBridge } from '../bridges/dojo-form-bridge';
import { FormBridge } from '../interfaces/form-bridge.interface';

/**
 * Configuration interface for Angular form bridge implementation.
 * @interface
 */
interface AngularConfig {
    type: 'angular';
    form: FormGroup;
    zone: NgZone;
    dialogService: DialogService;
    /**
     * Optional callback invoked when a field's visibility changes via show()/hide().
     * Used to propagate visibility state back to the Angular store.
     *
     * @param fieldVariable - The variable name of the field whose visibility changed.
     * @param visible - `true` if the field should be shown, `false` if hidden.
     */
    onFieldVisibilityChange?: (fieldVariable: string, visible: boolean) => void;
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
        return AngularFormBridge.getInstance(
            config.form,
            config.zone,
            config.dialogService,
            config.onFieldVisibilityChange
        );
    }

    return new DojoFormBridge();
}

/**
 * Saves the current form bridge singleton onto an internal stack
 * so a nested context (e.g. a dialog) can create its own bridge.
 * Call `popFormBridge()` when the nested context is destroyed.
 */
export function pushFormBridge(): void {
    AngularFormBridge.pushInstance();
}

/**
 * Destroys the current form bridge singleton and restores the
 * previous one from the stack (saved by `pushFormBridge()`).
 */
export function popFormBridge(): void {
    AngularFormBridge.popInstance();
}
