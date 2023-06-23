import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, Inject, Input, OnInit } from '@angular/core';

import { take } from 'rxjs/operators';

import { DotPropertiesService } from '@dotcms/data-access';
import { FeaturedFlags } from '@dotcms/dotcms-models';

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
    changeDetection: ChangeDetectionStrategy.Default
})
export class DotEditPageInfoComponent implements OnInit {
    @Input() title: string;
    @Input() url: string;
    innerApiLink: string;
    previewUrl: string;
    baseUrl: string;
    seoImprovements: boolean;

    constructor(
        @Inject(DOCUMENT) private document: Document,
        private dotPropertiesService: DotPropertiesService
    ) {
        this.baseUrl = document.defaultView.location.href.includes('edit-page')
            ? document.defaultView.location.origin
            : '';
    }

    ngOnInit() {
        this.dotPropertiesService
            .getKey(FeaturedFlags.FEATURE_FLAG_SEO_IMPROVEMENTS)
            .pipe(take(1))
            .subscribe((value) => {
                this.seoImprovements = value && value === 'true';
            });
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
