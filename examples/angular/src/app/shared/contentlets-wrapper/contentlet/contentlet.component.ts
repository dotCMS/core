import { Component, input } from '@angular/core';

import { NgTemplateOutlet } from '@angular/common';
import { getUVEState } from '@dotcms/uve';
import { UVE_MODE, DotCMSBasicContentlet } from '@dotcms/types';

/**
 * Local component for rendering a single contentlet outside the DotCmsLayout.
 * This is useful when you want to render a contentlet in a different context than the DotCmsLayout, like in a modal, sidebar, footer, etc.
 * @export
 * @class ContentletComponent
 */
@Component({
    selector: 'app-contentlet',
    standalone: true,
    imports: [NgTemplateOutlet],
    template: `
        @if(isEditing) {
        <div
            data-dot-object="contentlet"
            [attr.data-dot-identifier]="contentlet().identifier"
            [attr.data-dot-basetype]="contentlet().baseType"
            [attr.data-dot-title]="contentlet().widgetTitle || contentlet().title"
            [attr.data-dot-inode]="contentlet().inode"
            [attr.data-dot-type]="contentlet().contentType"
            [attr.data-dot-on-number-of-pages]="contentlet().onNumberOfPages ?? 0">
            <ng-container *ngTemplateOutlet="contentletTemplate"></ng-container>
        </div>

        } @else {
        <ng-container *ngTemplateOutlet="contentletTemplate"></ng-container>
        }

        <ng-template #contentletTemplate> <ng-content /></ng-template>
    `
})
export class ContentletComponent {
    contentlet = input.required<DotCMSBasicContentlet>();

    protected readonly isEditing = getUVEState()?.mode === UVE_MODE.EDIT;
}
