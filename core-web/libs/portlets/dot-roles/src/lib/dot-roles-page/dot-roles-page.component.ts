import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';

import { SplitterModule } from 'primeng/splitter';
import { TabsModule } from 'primeng/tabs';
import { SplitterPassThrough } from 'primeng/types/splitter';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolePermissionsIframeComponent } from './components/dot-role-permissions-iframe/dot-role-permissions-iframe.component';
import { DotRoleToolsIframeComponent } from './components/dot-role-tools-iframe/dot-role-tools-iframe.component';
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
        DotRolesTreeComponent,
        DotRolesDetailHeaderComponent,
        DotRoleUsersTabComponent,
        DotRolePermissionsIframeComponent,
        DotRoleToolsIframeComponent
    ],
    providers: [DotRolesStore],
    templateUrl: './dot-roles-page.component.html',
    host: { class: 'flex flex-1 min-h-0 block' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRolesPageComponent implements OnInit {
    protected readonly store = inject(DotRolesStore);

    // PT overrides for the full-height flex chain from `<p-tabs>` down to
    // `<p-tabpanel>`. `flex-1` on `tabPanelsPt` (basis 0) is load-bearing:
    // the default `1 1 auto` measures the basis from content, which for
    // roles with many members squeezes the tablist and clips the active-tab
    // underline. See `git log` for the full rationale.
    // `panel` is required by SplitterPassThroughOptions while the rest are optional,
    // so this looks like an upstream oversight. An empty passthrough satisfies it
    // and applies nothing.
    protected readonly splitterPt: SplitterPassThrough = {
        root: { class: 'border-0! rounded-none!' },
        panel: {}
    };
    protected readonly tabsPt = { root: { class: 'flex! flex-col! flex-1! min-h-0!' } };
    protected readonly tabPanelsPt = {
        root: { class: 'block! flex-1! min-h-0! overflow-hidden! p-0!' }
    };
    protected readonly tabPanelPt = { root: { class: 'block! h-full! p-0!' } };

    ngOnInit(): void {
        this.store.loadRootRoles();
    }

    protected onTabChange(tab: string | number | undefined): void {
        if (tab === undefined) {
            return;
        }

        this.store.setActiveTab(String(tab) as DotRoleTab);
    }
}
