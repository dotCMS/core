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

    /*
     * PT configs replace the removed `dot-roles-page.component.scss`. Each rule
     * has a specific reason, called out below, and the `!` suffix (Tailwind v4
     * `!important`) is only used where the class must beat a PrimeNG default.
     *
     * `tabsPt` — full-height flex column that fills the parent section.
     * PrimeNG already gives `.p-tabs` `display: flex; flex-direction: column`,
     * so `flex!` / `flex-col!` are defensive; `flex-1! min-h-0!` is required
     * so the tabs box stretches to fill available height without letting
     * children push the parent taller than the viewport.
     *
     * `tabPanelsPt` — `block!` because custom-element hosts default to
     * `display: inline` and PrimeNG does not override that on the p-tabpanels
     * host itself; `flex-1! min-h-0!` claims the space left over after the
     * tablist takes its natural size. **`flex-1` (basis 0) is load-bearing** —
     * with the default `flex: 0 1 auto`, tabpanels' flex-basis is measured
     * from the users-tab's natural content height, and for roles with many
     * members the algorithm proportionally shrinks BOTH children (tablist +
     * tabpanels), squeezing the tablist from 49.6px to ~40px and clipping the
     * active-tab underline. `overflow-hidden!` bounds the users-tab so its
     * internal scroll (see `[scrollable]/scrollHeight="flex"` on the members
     * `p-table`) works; `p-0!` removes PrimeNG's default panel padding so the
     * tab content reaches the section edges.
     *
     * `tabPanelPt` — `block!` for the same custom-element reason;
     * `h-full!` propagates the tabpanels' bounded height down to the
     * users-tab (and to the permissions/tools iframes) so `[scrollable]`
     * inside can compute against a finite parent; `p-0!` mirrors the panel
     * padding removal on the container.
     */
    protected readonly tabsPt = { root: { class: 'flex! flex-col! flex-1! min-h-0!' } };
    protected readonly tabPanelsPt = {
        root: { class: 'block! flex-1! min-h-0! overflow-hidden! p-0!' }
    };
    protected readonly tabPanelPt = { root: { class: 'block! h-full! p-0!' } };

    ngOnInit(): void {
        this.store.loadRootRoles();
    }

    protected onTabChange(tab: string | number): void {
        this.store.setActiveTab(String(tab) as DotRoleTab);
    }
}
