import { CommonModule, DOCUMENT } from '@angular/common';
import { Component, Inject, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotLinkComponent } from '@components/dot-link/dot-link.component';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import {
    DotApiLinkComponent,
    DotCopyButtonComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';

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
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DotCopyButtonComponent,
        DotApiLinkComponent,
        DotSafeHtmlPipe,
        DotMessagePipe,
        DotLinkComponent
    ],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoSeoComponent {
    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    baseUrl: string;
    seoImprovements: boolean;
    previewUrl: string;

    constructor(@Inject(DOCUMENT) private document: Document) {
        this.baseUrl = document.defaultView.location.href.includes('edit-page')
            ? document.defaultView.location.origin
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
