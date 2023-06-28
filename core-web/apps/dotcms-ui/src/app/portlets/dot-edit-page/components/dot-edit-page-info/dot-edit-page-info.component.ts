import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject, Input } from '@angular/core';

/**
 * Basic page information for edit mode
 *
 * @export
 * @class DotEditPageInfoComponent
 */
@Component({
    selector: 'dot-edit-page-info',
    templateUrl: './dot-edit-page-info.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrls: ['./dot-edit-page-info.component.scss']
})
export class DotEditPageInfoComponent {
    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    previewUrl: string;
    baseUrl: string;

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
