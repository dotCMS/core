import { ChangeDetectionStrategy, Component, Input, inject, DOCUMENT } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkComponent, DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { LOCATION_TOKEN } from '../../../../providers';
import { DotLinkComponent } from '../../../../view/components/dot-link/dot-link.component';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 */
@Component({
    selector: 'dot-edit-page-info',
    templateUrl: './dot-edit-page-info.component.html',
    styleUrls: ['./dot-edit-page-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ButtonModule,
        DotCopyButtonComponent,
        DotApiLinkComponent,
        DotLinkComponent,
        DotMessagePipe
    ],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoComponent {
    private document = inject<Document>(DOCUMENT);

    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    previewUrl: string;
    baseUrl: string;

    constructor() {
        const document = this.document;

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
