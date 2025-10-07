import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { DEFAULT_LANG_ID } from '../../shared/utils';

@Component({
    selector: 'dot-asset-form',
    templateUrl: './asset-form.component.html',
    styleUrls: ['./asset-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AssetFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() type: EditorAssetTypes;
    @Input() onSelectAsset: (payload: DotCMSContentlet | string) => void;
    @Input() preventClose: (value: boolean) => void;
    @Input() onHide: (value: boolean) => void;

    public disableTabs = false;

    public onPreventClose(value) {
        this.preventClose(value);
        this.disableTabs = value;
    }
}
