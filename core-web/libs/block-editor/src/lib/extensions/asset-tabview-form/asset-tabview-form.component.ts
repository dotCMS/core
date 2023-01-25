import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DEFAULT_LANG_ID } from '../bubble-menu/models/index';

@Component({
    selector: 'dot-asset-tabview-form',
    templateUrl: './asset-tabview-form.component.html',
    styleUrls: ['./asset-tabview-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetTabviewFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() assetType = 'asset';
    @Input() onSelectAsset: (payload: DotCMSContentlet | string) => void;
}
