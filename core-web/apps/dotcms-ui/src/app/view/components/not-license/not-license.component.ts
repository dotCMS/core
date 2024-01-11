import { Component } from '@angular/core';

import { DotNotLicenseComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-not-license-page',
    standalone: true,
    imports: [DotNotLicenseComponent],
    templateUrl: './not-license.component.html',
    styleUrl: './not-license.component.scss'
})
export class NotLicenseComponent {}
