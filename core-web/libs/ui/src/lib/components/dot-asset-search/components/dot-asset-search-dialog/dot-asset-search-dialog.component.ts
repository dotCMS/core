import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';

import { DotAssetSearchComponent } from '../../dot-asset-search.component';

@Component({
    selector: 'dot-asset-search-dialog',
    imports: [DotAssetSearchComponent],
    templateUrl: './dot-asset-search-dialog.component.html',
    styleUrl: './dot-asset-search-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchDialogComponent {
    private readonly config = inject(DynamicDialogConfig);

    private readonly ref = inject(DynamicDialogRef);

    protected editorAssetType: EditorAssetTypes;

    constructor() {
        this.editorAssetType = this.config.data?.assetType;
    }

    onSelectAsset(asset: DotCMSContentlet): void {
        this.ref.close(asset);
    }
}
