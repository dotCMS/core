import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotAssetSearchComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-asset-search-dialog',
    standalone: true,
    imports: [CommonModule, DotAssetSearchComponent],
    templateUrl: './dot-asset-search-dialog.component.html',
    styleUrl: './dot-asset-search-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchDialogComponent {
    private readonly config = inject(DynamicDialogConfig);
    private readonly ref = inject(DynamicDialogRef);

    onSelectAsset(asset: DotCMSContentlet): void {
        this.ref.close(asset);
    }
}