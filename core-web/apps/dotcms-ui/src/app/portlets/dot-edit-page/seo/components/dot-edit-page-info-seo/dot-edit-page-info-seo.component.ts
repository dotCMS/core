import { CommonModule, DOCUMENT } from '@angular/common';
import { Component, Inject, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotLinkComponent } from '@components/dot-link/dot-link.component';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
        DotCopyButtonModule,
        DotApiLinkModule,
        DotPipesModule,
        DotMessagePipe,
        DotLinkComponent
    ],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoSeoComponent {
    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    previewUrl: string;
    baseUrl: string;
    seoImprovements: boolean;

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
