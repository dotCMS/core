import { Component, computed, effect, inject, untracked } from '@angular/core';

import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';

import {
    DotLicenseService,
    DotMessageService,
    DotUnlicensedPortletData
} from '@dotcms/data-access';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotNotLicenseComponent,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotNetworkNodeDetailComponent } from './components/dot-network-node-detail/dot-network-node-detail.component';
import { DotNetworkNodeListComponent } from './components/dot-network-node-list/dot-network-node-list.component';
import { DotNetworkStore } from './store/dot-network.store';

/** Upsell shown on installs whose license does not include cluster data (FR-046). */
const CLUSTERING_UPSELL: DotUnlicensedPortletData = {
    icon: 'server',
    titleKey: 'com.dotcms.repackage.javax.portlet.title.EXT_CLUSTERING_TOOL',
    url: '/c/network-beta'
};

/**
 * Network portlet: every node in the cluster on the left, the selected node's health on the
 * right. Replaces the Network tab of the Configuration portlet.
 */
@Component({
    selector: 'dot-network-page',
    imports: [
        MessageModule,
        SkeletonModule,
        DotEmptyContainerComponent,
        DotNotLicenseComponent,
        DotMessagePipe,
        DotNetworkNodeListComponent,
        DotNetworkNodeDetailComponent
    ],
    providers: [DotNetworkStore],
    templateUrl: './dot-network-page.component.html',
    host: { class: 'flex flex-1 min-h-0 bg-white' }
})
export class DotNetworkPageComponent {
    protected readonly store = inject(DotNetworkStore);
    readonly #messageService = inject(DotMessageService);
    readonly #licenseService = inject(DotLicenseService);

    protected readonly skeletonCards = [1, 2, 3];

    protected readonly $emptyConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('network.empty.title'),
        subtitle: this.#messageService.get('network.empty.subtitle'),
        icon: 'lan',
        iconStyle: 'material-symbols-rounded'
    }));

    protected readonly $errorConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('network.error.title'),
        subtitle: this.#messageService.get('network.error.subtitle'),
        icon: 'error',
        iconStyle: 'material-symbols-rounded'
    }));

    protected readonly $retryLabel = computed(() => this.#messageService.get('network.retry'));

    constructor() {
        // `dot-not-license` renders whatever `DotLicenseService.unlicenseData` holds. A component
        // effect runs before this view is refreshed, so the upsell gets the Clustering copy (the
        // old tab's unlicensed page used the same title) before it first renders. It is only
        // pushed when this install is unlicensed, to leave other portlets' upsell data alone.
        effect(() => {
            if (this.store.showUnlicensed()) {
                untracked(() => this.#licenseService.unlicenseData.next(CLUSTERING_UPSELL));
            }
        });
    }
}
