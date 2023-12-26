import { Component, Input } from '@angular/core';

import { DotRouterService } from '@dotcms/data-access';
import { DotApps } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    styleUrls: ['./dot-apps-configuration-header.component.scss']
})
export class DotAppsConfigurationHeaderComponent {
    showMore: boolean;

    @Input() app: DotApps;

    constructor(private dotRouterService: DotRouterService) {}

    /**
     * Redirects to app configuration listing page
     *
     * @param string key
     * @memberof DotAppsConfigurationDetailComponent
     */
    goToApps(key: string): void {
        this.dotRouterService.gotoPortlet(`/apps/${key}`);
        this.dotRouterService.goToAppsConfiguration(key);
    }
}
