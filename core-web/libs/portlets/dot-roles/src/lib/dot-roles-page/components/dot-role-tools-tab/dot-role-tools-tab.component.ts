import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotRoleToolGroupRow } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

/**
 * How many portlet titles the Included Tools column spells out before
 * collapsing the rest into "and N more...". Four keeps the cell on one line at
 * the narrowest supported panel width.
 */
const INCLUDED_TOOLS_PREVIEW = 4;

/**
 * `getPorletTitlesFromLayout` resolves each portlet title through
 * `LanguageUtil.get(...)`, which returns the KEY itself when no translation
 * exists — so custom portlets come back as the raw
 * `com.dotcms.repackage.javax.portlet.title.c_Activities` rather than
 * "Activities". Recover a readable label from the key instead of printing it.
 */
const PORTLET_TITLE_KEY_PREFIX = 'com.dotcms.repackage.javax.portlet.title.';

function toReadableToolName(title: string): string {
    if (!title.startsWith(PORTLET_TITLE_KEY_PREFIX)) {
        return title;
    }

    // `c_Blog-Entries` -> `Blog Entries`. The `c_` marks a custom portlet and
    // carries no meaning for the reader.
    return title
        .slice(PORTLET_TITLE_KEY_PREFIX.length)
        .replace(/^c_/, '')
        .replace(/[-_]+/g, ' ')
        .trim();
}

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
    imports: [
        FormsModule,
        CheckboxModule,
        TableModule,
        TagModule,
        SkeletonModule,
        DotMessagePipe,
        DotEmptyContainerComponent
    ],
    templateUrl: './dot-role-tools-tab.component.html',
    host: { class: 'block h-full' }
})
export class DotRoleToolsTabComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #messageService = inject(DotMessageService);

    protected readonly $emptyToolGroupsConfig = computed<PrincipalConfiguration>(() => ({
        title: this.#messageService.get('roles.tools.empty'),
        icon: 'construction',
        iconStyle: 'material-symbols-rounded'
    }));

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
        const titles = (group.portletTitles ?? []).filter(Boolean).map(toReadableToolName);
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
