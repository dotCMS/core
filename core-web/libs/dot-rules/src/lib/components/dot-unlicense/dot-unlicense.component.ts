import { Component, OnInit, inject } from '@angular/core';

import { I18nService } from '../../services/system/locale/I18n';

@Component({
    selector: 'dot-unlicense',
    templateUrl: './dot-unlicense.component.html',
    styleUrls: ['./dot-unlicense.component.scss'],
    standalone: false
})
export class DotUnlicenseComponent implements OnInit {
    private resources = inject(I18nService);

    rulesTitle: string;
    onlyEnterpriseLabel: string;
    learnMoreEnterpriseLabel: string;
    contactUsLabel: string;
    requestTrialLabel: string;

    ngOnInit() {
        this.resources.get('com.dotcms.repackage.javax.portlet.title.rules').subscribe((label) => {
            this.rulesTitle = label;
        });
        this.resources.get('only-available-in-enterprise').subscribe((label) => {
            this.onlyEnterpriseLabel = label;
        });
        this.resources.get('Learn-more-about-dotCMS-Enterprise').subscribe((label) => {
            this.learnMoreEnterpriseLabel = label;
        });
        this.resources.get('Contact-Us-for-more-Information').subscribe((label) => {
            this.contactUsLabel = label;
        });
        this.resources.get('request-trial-license').subscribe((label) => {
            this.requestTrialLabel = label;
        });
    }
}
