import { Component, Input, inject } from '@angular/core';

import { DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    styleUrls: ['./dot-apps-configuration-header.component.scss']
})
export class DotAppsConfigurationHeaderComponent {
    private dotRouterService = inject(DotRouterService);

    showMore: boolean;

    @Input() app: DotApp;

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
