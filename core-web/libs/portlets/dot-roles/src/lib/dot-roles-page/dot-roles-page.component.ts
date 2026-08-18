import { Component, OnInit, inject } from '@angular/core';

import { TabsModule } from 'primeng/tabs';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolePermissionsIframeComponent } from './components/dot-role-permissions-iframe/dot-role-permissions-iframe.component';
import { DotRoleToolsIframeComponent } from './components/dot-role-tools-iframe/dot-role-tools-iframe.component';
import { DotRoleUsersTabComponent } from './components/dot-role-users-tab/dot-role-users-tab.component';
import { DotRolesDetailHeaderComponent } from './components/dot-roles-detail-header/dot-roles-detail-header.component';
import { DotRolesTreeComponent } from './components/dot-roles-tree/dot-roles-tree.component';
import { DotRolesStore } from './store/dot-roles.store';

import { DotRoleTab } from '../models/dot-roles.models';
import { DotRolesPortletService } from '../services/dot-roles-portlet.service';

@Component({
    selector: 'dot-roles-page',
    standalone: true,
    imports: [
        TabsModule,
        DotMessagePipe,
        DotRolesTreeComponent,
        DotRolesDetailHeaderComponent,
        DotRoleUsersTabComponent,
        DotRolePermissionsIframeComponent,
        DotRoleToolsIframeComponent
    ],
    providers: [DotRolesPortletService, DotRolesStore],
    templateUrl: './dot-roles-page.component.html',
    styleUrl: './dot-roles-page.component.scss',
    host: { class: 'flex flex-1 min-h-0 block' }
})
export class DotRolesPageComponent implements OnInit {
    protected readonly store = inject(DotRolesStore);

    /**
     * PassThrough to strip PrimeNG's default padding off the tab panel
     * wrappers so the Users tab's `p-table` reaches the section edges
     * to align with the panel divider. The full-height flex chain is
     * enforced in `dot-roles-page.component.scss` — see that file for
     * why we don't do it here via `pt`.
     */
    protected readonly tabPanelsPt = { root: { class: 'p-0!' } };
    protected readonly tabPanelPt = { root: { class: 'p-0!' } };

    ngOnInit(): void {
        this.store.loadRootRoles();
    }

    protected onTabChange(tab: string | number): void {
        this.store.setActiveTab(String(tab) as DotRoleTab);
    }
}
