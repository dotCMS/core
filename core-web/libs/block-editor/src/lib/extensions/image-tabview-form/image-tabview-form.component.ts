import { Component, Input } from '@angular/core';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DEFAULT_LANG_ID } from '@dotcms/block-editor';
import { ChangeDetectorRef, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageTabviewFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() isVisible = false;
    @Input() onSelectImage: (payload: DotCMSContentlet | string) => void;

    constructor(private readonly cd: ChangeDetectorRef) {}

    toggleIsVisible(value) {
        this.isVisible = value;
        this.cd.markForCheck();
    }
}
