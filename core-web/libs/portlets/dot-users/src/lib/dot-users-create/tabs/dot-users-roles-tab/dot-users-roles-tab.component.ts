import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

import {
    DOT_USERS_MOCK_ROLES,
    DOT_USERS_ROLE_GROUPS,
    DotUsersRoleGroup,
    DotUsersRoleOption
} from './dot-users-roles.data';

interface AvailableGroupView {
    group: DotUsersRoleGroup;
    roots: RoleTreeNode[];
}

interface RoleTreeNode {
    role: DotUsersRoleOption;
    level: number;
    children: RoleTreeNode[];
    hasVisibleDescendant: boolean;
}

/**
 * Roles tab (shuttle variant). Two-column picker with an "Available"
 * tree on the left and a "Granted" list on the right, arrows in the
 * middle to move selection across.
 *
 * State is local to the component while we iterate on visuals; the
 * eventual wiring will lift `granted` up to the shell so the shared
 * dialog form owns the save payload.
 */
@Component({
    selector: 'dot-users-roles-tab',
    standalone: true,
    imports: [
        NgTemplateOutlet,
        FormsModule,
        ButtonModule,
        CheckboxModule,
        InputTextModule,
        DotMessagePipe
    ],
    templateUrl: './dot-users-roles-tab.component.html',
    styleUrl: './dot-users-roles-tab.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col gap-4 block' }
})
export class DotUsersRolesTabComponent {
    protected readonly allRoles = DOT_USERS_MOCK_ROLES;
    protected readonly groups = DOT_USERS_ROLE_GROUPS;

    protected readonly granted = signal<string[]>([]);
    protected readonly selectedAvailable = signal<string[]>([]);
    protected readonly selectedGranted = signal<string[]>([]);
    protected readonly availableFilter = signal('');
    protected readonly grantedFilter = signal('');
    protected readonly collapsed = signal<Record<string, boolean>>({});

    protected readonly availableTree = computed<AvailableGroupView[]>(() => {
        const query = this.availableFilter().toLowerCase().trim();
        const grantedIds = new Set(this.granted());
        const pool = this.allRoles.filter((role) => !grantedIds.has(role.id));
        const byParent = new Map<string, DotUsersRoleOption[]>();
        for (const role of pool) {
            const key = role.parent ?? '__root__';
            const bucket = byParent.get(key) ?? [];
            bucket.push(role);
            byParent.set(key, bucket);
        }

        const buildTree = (role: DotUsersRoleOption, level: number): RoleTreeNode => {
            const rawChildren = byParent.get(role.id) ?? [];
            const childNodes = rawChildren.map((child) => buildTree(child, level + 1));
            const matchesSelf = !query || role.name.toLowerCase().includes(query);
            const hasVisibleDescendant = childNodes.some(
                (node) => node.hasVisibleDescendant || node.role.name.toLowerCase().includes(query)
            );

            return {
                role,
                level,
                children: childNodes,
                hasVisibleDescendant: matchesSelf || hasVisibleDescendant
            };
        };

        return this.groups
            .map((group) => {
                const roots = pool
                    .filter((role) => role.group === group && !role.parent)
                    .map((role) => buildTree(role, 1))
                    .filter(
                        (node) =>
                            !query ||
                            node.role.name.toLowerCase().includes(query) ||
                            node.hasVisibleDescendant
                    );

                return { group, roots };
            })
            .filter((view) => view.roots.length > 0);
    });

    protected readonly grantedList = computed<DotUsersRoleOption[]>(() => {
        const query = this.grantedFilter().toLowerCase().trim();
        const grantedIds = new Set(this.granted());

        return this.allRoles.filter(
            (role) => grantedIds.has(role.id) && (!query || role.name.toLowerCase().includes(query))
        );
    });

    protected readonly grantedCount = computed(() => this.granted().length);
    protected readonly canGrant = computed(() => this.selectedAvailable().length > 0);
    protected readonly canRevoke = computed(() => this.selectedGranted().length > 0);

    protected onAvailableFilter(value: string): void {
        this.availableFilter.set(value);
    }

    protected onGrantedFilter(value: string): void {
        this.grantedFilter.set(value);
    }

    protected toggleGroup(group: string): void {
        this.collapsed.update((state) => ({ ...state, [`g:${group}`]: !state[`g:${group}`] }));
    }

    protected toggleNode(id: string): void {
        this.collapsed.update((state) => ({ ...state, [id]: !state[id] }));
    }

    protected isNodeOpen(id: string): boolean {
        return !this.collapsed()[id];
    }

    protected isGroupOpen(group: string): boolean {
        return !this.collapsed()[`g:${group}`];
    }

    protected toggleAvailableSelection(id: string): void {
        this.selectedAvailable.update((current) =>
            current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
        );
    }

    protected toggleGrantedSelection(id: string): void {
        this.selectedGranted.update((current) =>
            current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
        );
    }

    protected grant(): void {
        const toAdd = this.selectedAvailable();
        if (toAdd.length === 0) {
            return;
        }
        this.granted.update((current) => [...current, ...toAdd]);
        this.selectedAvailable.set([]);
    }

    protected revoke(): void {
        const toRemove = new Set(this.selectedGranted());
        if (toRemove.size === 0) {
            return;
        }
        this.granted.update((current) => current.filter((id) => !toRemove.has(id)));
        this.selectedGranted.set([]);
    }

    protected isSelectedAvailable(id: string): boolean {
        return this.selectedAvailable().includes(id);
    }

    protected isSelectedGranted(id: string): boolean {
        return this.selectedGranted().includes(id);
    }
}
