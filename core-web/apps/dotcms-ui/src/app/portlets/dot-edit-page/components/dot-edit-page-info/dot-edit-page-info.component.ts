import { Component, Input } from '@angular/core';

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
    previewURl() {
        const frontEndUrl = `${this.apiLink.replace('api/v1/page/render', '')}`;

        return `${frontEndUrl}${
            frontEndUrl.indexOf('?') != -1 ? '&' : '?'
        }disabledNavigateMode=true`;
    }
}
