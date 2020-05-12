import { Component, OnInit, Input } from '@angular/core';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';
import * as _ from 'lodash';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    styleUrls: ['./dot-apps-configuration-header.component.scss']
})
export class DotAppsConfigurationHeaderComponent implements OnInit {
    messagesKey: { [key: string]: string } = {};
    showMore: boolean;

    @Input() app: DotApps;

    constructor(
        private dotRouterService: DotRouterService,
        public dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'apps.configurations',
                'apps.no.configurations',
                'apps.key',
                'apps.confirmation.description.show.more',
                'apps.confirmation.description.show.less'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }

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
