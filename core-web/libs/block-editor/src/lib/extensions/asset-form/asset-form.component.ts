import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { DEFAULT_LANG_ID } from '../bubble-menu/models/index';

@Component({
    selector: 'dot-asset-form',
    templateUrl: './asset-form.component.html',
    styleUrls: ['./asset-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() type: EditorAssetTypes;
    @Input() onSelectAsset: (payload: DotCMSContentlet | string) => void;
}
