import { NgZone } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { AngularFormBridge } from '../bridges/angular-form-bridge';
import { DojoFormBridge } from '../bridges/dojo-form-bridge';
import { FormBridge } from '../interfaces/form-bridge.interface';

interface AngularConfig {
    type: 'angular';
    form: FormGroup;
    zone: NgZone;
}

interface DojoConfig {
    type: 'dojo';
}

type BridgeConfig = AngularConfig | DojoConfig;

export function createFormBridge(config: BridgeConfig): FormBridge {
    if (config.type === 'angular') {
        return new AngularFormBridge(config.form, config.zone);
    }

    return new DojoFormBridge();
}
