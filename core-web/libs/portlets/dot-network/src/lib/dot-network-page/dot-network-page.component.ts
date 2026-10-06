import { Component, computed, inject } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, PrincipalConfiguration } from '@dotcms/ui';

import { DotNetworkNodeDetailComponent } from './components/dot-network-node-detail/dot-network-node-detail.component';
import { DotNetworkNodeListComponent } from './components/dot-network-node-list/dot-network-node-list.component';
import { DotNetworkStore } from './store/dot-network.store';

/**
 * Network portlet: every node in the cluster on the left, the selected node's health on the
 * right. Replaces the Network tab of the Configuration portlet.
 */
@Component({
    selector: 'dot-network-page',
    imports: [
        SkeletonModule,
        DotEmptyContainerComponent,
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

    protected readonly $unlicensedConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('network.unlicensed.title'),
        subtitle: this.#messageService.get('network.unlicensed.subtitle'),
        icon: 'lock',
        iconStyle: 'material-symbols-rounded'
    }));

    protected readonly $retryLabel = computed(() => this.#messageService.get('network.retry'));
}
