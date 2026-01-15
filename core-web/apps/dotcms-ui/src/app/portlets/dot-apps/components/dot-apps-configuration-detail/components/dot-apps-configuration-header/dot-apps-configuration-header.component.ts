import { MarkdownComponent } from 'ngx-markdown';

import { NgClass } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotCopyLinkComponent } from '../../../../../../view/components/dot-copy-link/dot-copy-link.component';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    styleUrls: ['./dot-apps-configuration-header.component.scss'],
    imports: [
        NgClass,
        AvatarModule,
        MarkdownComponent,
        DotAvatarDirective,
        DotCopyLinkComponent,
        DotMessagePipe
    ]
})
export class DotAppsConfigurationHeaderComponent {
    private dotRouterService = inject(DotRouterService);

    showMore = signal(false);

    app = input<DotApp>();

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

    toggleShowMore(): void {
        this.showMore.update((value) => !value);
    }
}
