import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRoleToolGroupRow } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

/**
 * How many portlet titles the Included Tools column spells out before
 * collapsing the rest into "and N more...". Four keeps the cell on one line at
 * the narrowest supported panel width.
 */
const INCLUDED_TOOLS_PREVIEW = 4;

/**
 * Tools tab — grant / revoke tool groups on the selected role.
 *
 * Deliberately narrower than the legacy Dojo screen: it lists the tool groups
 * and toggles which ones the role gets. Creating, editing and deleting tool
 * groups is not here — those have no v1 endpoints (only `RoleAjax` DWR) and
 * belong to a dedicated Tools portlet.
 */
@Component({
    selector: 'dot-role-tools-tab',
    standalone: true,
    imports: [FormsModule, CheckboxModule, TableModule, TagModule, SkeletonModule, DotMessagePipe],
    templateUrl: './dot-role-tools-tab.component.html',
    host: { class: 'block h-full' },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRoleToolsTabComponent {
    protected readonly store = inject(DotRolesStore);

    /**
     * The role's own grants. This is what gets POSTed on every toggle, since
     * the endpoint is a full replace — inherited rows are excluded because
     * they belong to the ancestor.
     */
    protected readonly $directGrantIds = computed(() =>
        this.store
            .toolGroups()
            .filter((group) => this.isDirectGrant(group))
            .map((group) => group.id)
    );

    protected isDirectGrant(group: DotRoleToolGroupRow): boolean {
        return group.granted && group.grantedFromRoleId === this.store.selectedRoleId();
    }

    /**
     * An inherited row renders checked but locked: revoking it means editing
     * the ancestor named in its Granted From chip.
     */
    protected isLocked(group: DotRoleToolGroupRow): boolean {
        return (
            !this.store.canEditRoleLayouts() ||
            this.store.toolGroupsSaving() ||
            (group.granted && !this.isDirectGrant(group))
        );
    }

    protected includedTools(group: DotRoleToolGroupRow): string {
        const titles = (group.portletTitles ?? []).filter(Boolean);
        if (titles.length === 0) {
            return '';
        }
        if (titles.length <= INCLUDED_TOOLS_PREVIEW) {
            return titles.join(', ');
        }

        const shown = titles.slice(0, INCLUDED_TOOLS_PREVIEW).join(', ');

        return `${shown} and ${titles.length - INCLUDED_TOOLS_PREVIEW} more...`;
    }

    protected onToggle(group: DotRoleToolGroupRow, checked: boolean): void {
        const current = new Set(this.$directGrantIds());
        if (checked) {
            current.add(group.id);
        } else {
            current.delete(group.id);
        }

        this.store.saveToolGroups(Array.from(current));
    }
}
