import { Component, Input, inject, DOCUMENT } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkComponent, DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { LOCATION_TOKEN } from '../../../../../providers';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 */
@Component({
    selector: 'dot-edit-page-info-seo',
    templateUrl: './dot-edit-page-info-seo.component.html',
    styleUrls: ['./dot-edit-page-info-seo.component.scss'],
    imports: [ButtonModule, DotCopyButtonComponent, DotApiLinkComponent, DotMessagePipe],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoSeoComponent {
    private document = inject<Document>(DOCUMENT);

    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    baseUrl = '';
    seoImprovements: boolean;
    previewUrl: string;

    constructor() {
        this.baseUrl = this.document.defaultView.location.href.includes('edit-page')
            ? this.document.defaultView.location.origin
            : '';
    }

    @Input()
    set apiLink(value: string) {
        if (value) {
            const frontEndUrl = `${value.replace('api/v1/page/render', '')}`;

            this.previewUrl = `${frontEndUrl}${
                frontEndUrl.indexOf('?') != -1 ? '&' : '?'
            }disabledNavigateMode=true`;
        } else {
            this.previewUrl = value;
        }

        this.innerApiLink = value;
    }
}
