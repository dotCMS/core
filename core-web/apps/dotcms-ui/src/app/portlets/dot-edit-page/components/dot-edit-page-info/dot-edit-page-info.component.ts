import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';

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
    standalone: false
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
