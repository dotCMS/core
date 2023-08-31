import { DOCUMENT } from '@angular/common';
import { Component, Inject, Input } from '@angular/core';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-page-info',
    templateUrl: './dot-edit-page-info.component.html',
    styleUrls: ['./dot-edit-page-info.component.scss']
})
export class DotEditPageInfoComponent {
    @Input() title: string;
    @Input() url: string;
    @Input() apiLink: string;
    baseUrl: string;

    constructor(@Inject(DOCUMENT) private document: Document) {
        this.baseUrl = document.defaultView.location.href.includes('edit-page')
            ? document.defaultView.location.origin
            : '';
    }
    previewURl() {
        const frontEndUrl = `${this.apiLink.replace('api/v1/page/render', '')}`;

        return `${frontEndUrl}${
            frontEndUrl.indexOf('?') != -1 ? '&' : '?'
        }disabledNavigateMode=true`;
    }
}
