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
    host: { class: 'flex flex-1 min-h-0 block' }
})
export class DotRolesPageComponent implements OnInit {
    protected readonly store = inject(DotRolesStore);

    ngOnInit(): void {
        this.store.loadRootRoles();
    }

    protected onTabChange(tab: string | number): void {
        this.store.setActiveTab(String(tab) as DotRoleTab);
    }
}
