import { Injectable } from '@angular/core';

import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { buildAssetPickerConfig, DotAssetPickerEntryOptions } from './asset-picker-config';
import { buildAssetPickerDialogConfig } from './asset-picker-dialog';
import { DotAssetPickerLauncher } from './asset-picker-launcher.token';
import { DotAssetPickerComponent } from './dot-asset-picker.component';

/**
 * Opens the new AssetPicker as a centered modal.
 *
 * Provided against `ASSET_PICKER_LAUNCHER` by the three Angular Edit Content hosts, which is what
 * makes the new picker the picker there. Stateless and dependency-free on purpose — see the token's
 * docs for why the caller hands over its own `DialogService` instead.
 *
 * This is also the one place that pairs `buildAssetPickerConfig` with
 * `buildAssetPickerDialogConfig`. Three entry points used to do it themselves, which is how the
 * Story Block and the File field drift into looking like different features.
 */
@Injectable()
export class AngularAssetPickerLauncher implements DotAssetPickerLauncher {
    open(
        dialogService: DialogService,
        options: DotAssetPickerEntryOptions,
        overrides?: Pick<DynamicDialogConfig, 'baseZIndex'>
    ): DynamicDialogRef {
        return dialogService.open(
            DotAssetPickerComponent,
            buildAssetPickerDialogConfig(buildAssetPickerConfig(options), overrides)
        );
    }
}
