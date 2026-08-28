import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';

import { SplitterModule } from 'primeng/splitter';
import { TabsModule } from 'primeng/tabs';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotRolePermissionsIframeComponent } from './components/dot-role-permissions-iframe/dot-role-permissions-iframe.component';
import { DotRoleToolsTabComponent } from './components/dot-role-tools-tab/dot-role-tools-tab.component';
import { DotRoleUsersTabComponent } from './components/dot-role-users-tab/dot-role-users-tab.component';
import { DotRolesDetailHeaderComponent } from './components/dot-roles-detail-header/dot-roles-detail-header.component';
import { DotRolesTreeComponent } from './components/dot-roles-tree/dot-roles-tree.component';
import { DotRolesStore } from './store/dot-roles.store';

import { DotRoleTab } from '../models/dot-roles.models';

@Component({
    selector: 'dot-roles-page',
    standalone: true,
    imports: [
        SplitterModule,
        TabsModule,
        DotMessagePipe,
        DotEmptyContainerComponent,
        DotRolesTreeComponent,
        DotRolesDetailHeaderComponent,
        DotRoleUsersTabComponent,
        DotRolePermissionsIframeComponent,
        DotRoleToolsTabComponent
    ],
    providers: [DotRolesStore],
    templateUrl: './dot-roles-page.component.html',
    host: { class: 'flex flex-1 min-h-0 block' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRolesPageComponent implements OnInit {
    protected readonly store = inject(DotRolesStore);
    readonly #messageService = inject(DotMessageService);

    // PT overrides for the full-height flex chain from `<p-tabs>` down to
    // `<p-tabpanel>`. `flex-1` on `tabPanelsPt` (basis 0) is load-bearing:
    // the default `1 1 auto` measures the basis from content, which for
    // roles with many members squeezes the tablist and clips the active-tab
    // underline. See `git log` for the full rationale.
    protected readonly splitterPt = { root: { class: 'border-0! rounded-none!' } };
    protected readonly tabsPt = { root: { class: 'flex! flex-col! flex-1! min-h-0!' } };
    protected readonly tabPanelsPt = {
        root: { class: 'block! flex-1! min-h-0! overflow-hidden! p-0!' }
    };
    protected readonly tabPanelPt = { root: { class: 'block! h-full! p-0!' } };

    // `hideContactUsLink` on every empty state in this portlet: these are
    // internal admin screens, not a licensing or capability dead-end, so
    // "Contact Us" would be noise.
    protected readonly $noSelectionConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('roles.detail.empty'),
        icon: 'left_panel_open',
        iconStyle: 'material-symbols-rounded'
    }));

    ngOnInit(): void {
        this.store.loadRootRoles();
    }

    protected onTabChange(tab: string | number): void {
        this.store.setActiveTab(String(tab) as DotRoleTab);
    }
}
