import { Component, ChangeDetectionStrategy } from '@angular/core';

import { DotNotLicenseComponent } from '@dotcms/ui';

@Component({
    changeDetection: ChangeDetectionStrategy.Eager,
    selector: 'dot-not-license-page',
    imports: [DotNotLicenseComponent],
    templateUrl: './not-license.component.html',
    styleUrl: './not-license.component.scss'
})
export class NotLicenseComponent {}
