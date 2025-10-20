import { MarkdownComponent } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCopyLinkModule } from '../../../view/components/dot-copy-link/dot-copy-link.module';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    styleUrls: ['./dot-apps-configuration-header.component.scss'],
    imports: [
        CommonModule,
        AvatarModule,
        MarkdownComponent,
        DotAvatarDirective,
        DotCopyLinkModule,
        DotMessagePipe
    ]
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
