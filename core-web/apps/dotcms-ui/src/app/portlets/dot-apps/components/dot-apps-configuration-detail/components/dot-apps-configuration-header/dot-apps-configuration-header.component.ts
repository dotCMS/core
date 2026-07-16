import { MarkdownComponent } from 'ngx-markdown';

import { Component, inject, input, signal } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotColorIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DotCopyLinkComponent } from '../../../../../../view/components/dot-copy-link/dot-copy-link.component';

@Component({
    selector: 'dot-apps-configuration-header',
    templateUrl: './dot-apps-configuration-header.component.html',
    host: {
        class: 'flex items-start gap-4 p-6 bg-white border-b border-gray-300 sticky top-0 z-10'
    },
    imports: [
        AvatarModule,
        MarkdownComponent,
        DotAvatarDirective,
        DotColorIconComponent,
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
