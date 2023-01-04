import { Component, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DEFAULT_LANG_ID } from '@dotcms/block-editor';

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageTabviewFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() showSearch = true;
    @Input() onSelectImage: (payload: DotCMSContentlet | string) => void;
    activeTab = 0;

    constructor(private readonly cd: ChangeDetectorRef) {}

    changeActiveTab(value) {
        this.activeTab = value;
        this.cd.markForCheck();
    }

    toggelShowSearch(value) {
        this.showSearch = value;
        this.cd.markForCheck();
    }
}
