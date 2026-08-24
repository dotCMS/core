import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';

import { take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRoleView, DotUsersService } from '../../../services/dot-users.service';

/**
 * Internal shape the shuttle renders. Keeps the visual code decoupled
 * from `DotRoleView` so the tree walker/filter logic never has to
 * worry about optional API fields.
 */
interface RoleOption {
    id: string;
    roleKey: string;
    name: string;
    description: string;
    parent?: string;
    /**
     * Mirrors `Role.editUsers`. `false` means the backend refuses to
     * link users to this role — `RoleAPIImpl.addRoleToUser` throws
     * `Cannot alter users on this role`, which rolls back the whole
     * transactional PUT (including any profile edits). Treated as
     * "not grantable" in the shuttle so no checkbox is ever shown.
     */
    editUsers: boolean;
}

interface RoleTreeNode {
    role: RoleOption;
    level: number;
    children: RoleTreeNode[];
    hasVisibleDescendant: boolean;
}

/**
 * Roles tab (shuttle variant). Two-column picker with an "Available"
 * tree on the left and a "Granted" list on the right, arrows in the
 * middle to move selection across.
 *
 * The shell owns the source-of-truth `roles` list on the outbound
 * save payload — this component takes the initial granted keys as
 * an input and emits every change through `grantedChange`.
 */
@Component({
    selector: 'dot-users-roles-tab',
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
    host: { class: 'flex flex-col gap-4' }
})
export class DotUsersRolesTabComponent {
    readonly #usersService = inject(DotUsersService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Role KEYS the user currently holds — sourced from the parent
     * dialog's `getUserRoles` fetch. `granted` is seeded from this
     * value on the first non-empty change; subsequent parent
     * mutations don't clobber in-flight user edits.
     */
    readonly initialGrantedKeys = input<string[]>([]);

    /**
     * Emits the full set of currently granted role KEYS every time
     * the user grants or revokes anything. The shell listens and
     * plugs the value into the save payload.
     */
    readonly grantedChange = output<string[]>();

    readonly #$allRoles = signal<RoleOption[]>([]);
    readonly #$rolesByKey = computed(() => {
        const map = new Map<string, RoleOption>();
        for (const role of this.#$allRoles()) {
            map.set(this.grantIdentifier(role), role);
        }

        return map;
    });

    /**
     * IDs of every role that appears as another role's parent. Used
     * to distinguish leaf nodes from container nodes in selection
     * logic.
     */
    readonly #$rolesWithChildren = computed(() => {
        const set = new Set<string>();
        for (const role of this.#$allRoles()) {
            if (role.parent) {
                set.add(role.parent);
            }
        }

        return set;
    });

    /**
     * A role is individually grantable when it's a leaf AND its
     * backend `editUsers` flag isn't `false`. Parents (nodes with
     * children) are excluded because checking a parent is a shortcut
     * for granting every grantable leaf beneath it, not for granting
     * the parent's own role. `editUsers=false` roles are excluded
     * because `addRoleToUser` throws on them and rolls the whole
     * transactional PUT back.
     *
     * `roleKey` is intentionally NOT required: dotCMS auto-generates
     * keys on role creation but legacy or manually-imported roles
     * can end up keyless, and callers still expect them in the
     * shuttle. When emitting on save we use `roleKey` when present
     * and fall back to the role `id` — worst case the backend
     * `loadRoleByKey` call silently no-ops.
     */
    private isGrantableLeaf(role: RoleOption): boolean {
        if (this.#$rolesWithChildren().has(role.id)) {
            return false;
        }

        return role.editUsers !== false;
    }

    /**
     * The identifier we hand up on `grantedChange` for a given role.
     * `roleKey` when the backend has one; the role `id` otherwise
     * so the shell can still preserve/save the assignment.
     * Exposed to the template so click handlers and selection state
     * key off the same value the internal `granted` signal holds.
     */
    protected grantIdentifier(role: RoleOption): string {
        return role.roleKey || role.id;
    }

    /**
     * For every role in the tree, the list of grantable leaf IDs
     * that live under it (including itself when the role is a
     * grantable leaf). Precomputed once per `allRoles` change so
     * checkbox state and toggle logic are O(1) per row.
     *
     * Non-user-assignable roles (`editUsers=false`) contribute
     * nothing to the map — the entire Publisher/Legal subtree ends
     * up with zero grantable leaves, so no checkbox anywhere.
     */
    readonly #$grantableLeavesByRole = computed(() => {
        const roles = this.#$allRoles();
        const childrenByParent = new Map<string, RoleOption[]>();
        for (const role of roles) {
            const key = role.parent ?? '__root__';
            const bucket = childrenByParent.get(key) ?? [];
            bucket.push(role);
            childrenByParent.set(key, bucket);
        }

        const cache = new Map<string, string[]>();
        const collect = (role: RoleOption): string[] => {
            const cached = cache.get(role.id);
            if (cached) {
                return cached;
            }

            const kids = childrenByParent.get(role.id) ?? [];
            if (kids.length === 0) {
                const result = this.isGrantableLeaf(role) ? [role.id] : [];
                cache.set(role.id, result);

                return result;
            }

            const collected: string[] = [];
            for (const child of kids) {
                collected.push(...collect(child));
            }
            cache.set(role.id, collected);

            return collected;
        };

        for (const role of roles) {
            collect(role);
        }

        return cache;
    });

    /**
     * A row shows a checkbox iff its subtree contains at least one
     * grantable leaf. Roots and intermediate parents surface the
     * bulk-select affordance; workflow-only branches (Publisher /
     * Legal, etc.) get no checkbox anywhere.
     */
    protected canSelectRole(role: RoleOption): boolean {
        return (this.#$grantableLeavesByRole().get(role.id) ?? []).length > 0;
    }

    /**
     * Checkbox is checked when every grantable leaf under this row
     * is currently selected. For a leaf itself this reduces to the
     * usual "am I selected".
     */
    protected isFullyChecked(role: RoleOption): boolean {
        const leaves = this.#$grantableLeavesByRole().get(role.id) ?? [];
        if (leaves.length === 0) {
            return false;
        }
        const selected = new Set(this.$selectedAvailable());

        return leaves.every((id) => selected.has(id));
    }

    /**
     * Indeterminate = some but not all grantable descendants are
     * currently selected. Only meaningful for parent rows.
     */
    protected isPartiallyChecked(role: RoleOption): boolean {
        const leaves = this.#$grantableLeavesByRole().get(role.id) ?? [];
        if (leaves.length <= 1) {
            return false;
        }
        const selected = new Set(this.$selectedAvailable());
        let count = 0;
        for (const id of leaves) {
            if (selected.has(id)) {
                count++;
            }
        }

        return count > 0 && count < leaves.length;
    }

    protected readonly $granted = signal<string[]>([]);
    protected readonly $selectedAvailable = signal<string[]>([]);
    protected readonly $selectedGranted = signal<string[]>([]);
    protected readonly $availableFilter = signal('');
    protected readonly $grantedFilter = signal('');
    protected readonly $collapsed = signal<Record<string, boolean>>({});
    protected readonly $isLoading = signal(false);

    /**
     * Flat forest of root role trees. There's no artificial "System"
     * vs "Custom" grouping — the backend's root roles already act as
     * the top-level buckets (e.g. `System`, `Categories`, `Intranet`,
     * `Publisher / Legal`) and their `roleChildren` come nested from
     * the load-children endpoint the service consumes.
     */
    protected readonly $availableTree = computed<RoleTreeNode[]>(() => {
        const query = this.$availableFilter().toLowerCase().trim();
        const grantedKeys = new Set(this.$granted());
        const grantableLeaves = this.#$grantableLeavesByRole();

        // Map granted `roleKey`s back to leaf ids so we can ask "is
        // every grantable descendant of this parent already granted?"
        const grantedLeafIds = new Set<string>();
        for (const role of this.#$allRoles()) {
            if (grantedKeys.has(this.grantIdentifier(role))) {
                grantedLeafIds.add(role.id);
            }
        }

        // A parent stays in the tree if either:
        //   - it never had any grantable descendants (workflow-only
        //     branches such as Publisher / Legal — nothing to move,
        //     so nothing to drop), or
        //   - at least one of its grantable descendants hasn't been
        //     granted yet.
        // Dropping "empty" parents keeps the panel honest — a root
        // whose entire grantable subtree is already on the right
        // shouldn't linger on the left as an empty container.
        const hasRemainingGrantable = (roleId: string): boolean => {
            const leaves = grantableLeaves.get(roleId) ?? [];
            if (leaves.length === 0) {
                return true;
            }

            return leaves.some((leafId) => !grantedLeafIds.has(leafId));
        };

        const pool = this.#$allRoles().filter((role) => {
            if (this.#$rolesWithChildren().has(role.id)) {
                return hasRemainingGrantable(role.id);
            }

            // Leaves are moved between panels: keep only when not yet
            // granted.
            return !grantedKeys.has(this.grantIdentifier(role));
        });
        const byParent = new Map<string, RoleOption[]>();
        for (const role of pool) {
            const key = role.parent ?? '__root__';
            const bucket = byParent.get(key) ?? [];
            bucket.push(role);
            byParent.set(key, bucket);
        }

        const buildTree = (role: RoleOption, level: number): RoleTreeNode => {
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

        return (byParent.get('__root__') ?? [])
            .map((role) => buildTree(role, 0))
            .filter(
                (node) =>
                    !query ||
                    node.role.name.toLowerCase().includes(query) ||
                    node.hasVisibleDescendant
            );
    });

    protected readonly $grantedList = computed<RoleOption[]>(() => {
        const query = this.$grantedFilter().toLowerCase().trim();
        const map = this.#$rolesByKey();
        const list: RoleOption[] = [];
        for (const key of this.$granted()) {
            const role = map.get(key);
            if (role && (!query || role.name.toLowerCase().includes(query))) {
                list.push(role);
            }
        }

        return list;
    });

    protected readonly $grantedCount = computed(() => this.$granted().length);
    protected readonly $canGrant = computed(() => this.$selectedAvailable().length > 0);
    protected readonly $canRevoke = computed(() => this.$selectedGranted().length > 0);

    constructor() {
        this.loadRoles();

        // Seed the local `granted` signal from the parent's `initialGrantedKeys`
        // exactly once, the first time it delivers a non-empty value. Later
        // parent mutations don't clobber in-flight user edits.
        let seeded = false;
        effect(() => {
            const keys = this.initialGrantedKeys();
            if (seeded) {
                return;
            }
            if (keys.length > 0) {
                this.$granted.set([...keys]);
                seeded = true;
            }
        });
    }

    protected onAvailableFilter(value: string): void {
        this.$availableFilter.set(value);
    }

    protected onGrantedFilter(value: string): void {
        this.$grantedFilter.set(value);
    }

    protected toggleNode(id: string): void {
        this.$collapsed.update((state) => ({ ...state, [id]: !state[id] }));
    }

    protected isNodeOpen(id: string): boolean {
        return !this.$collapsed()[id];
    }

    protected toggleAvailableSelection(id: string): void {
        const role = this.#$allRoles().find((entry) => entry.id === id);
        if (!role) {
            return;
        }
        // Rows without any grantable descendants (workflow branches,
        // isolated organizational nodes) toggle their subtree instead
        // of getting checked — matches the legacy Dojo tree.
        if (!this.canSelectRole(role)) {
            this.toggleNode(id);

            return;
        }

        // Checking a row selects every grantable leaf under it in one
        // shot. Unchecking removes them all. For a leaf, `leaves` is
        // just `[role.id]`, so the behavior collapses to per-row
        // toggling.
        const leaves = this.#$grantableLeavesByRole().get(id) ?? [];
        const currentSet = new Set(this.$selectedAvailable());
        const allChecked = leaves.every((leafId) => currentSet.has(leafId));

        if (allChecked) {
            for (const leafId of leaves) {
                currentSet.delete(leafId);
            }
        } else {
            for (const leafId of leaves) {
                currentSet.add(leafId);
            }
        }

        this.$selectedAvailable.set(Array.from(currentSet));
    }

    protected toggleGrantedSelection(id: string): void {
        this.$selectedGranted.update((current) =>
            current.includes(id) ? current.filter((x) => x !== id) : [...current, id]
        );
    }

    protected grant(): void {
        const selectedIds = new Set(this.$selectedAvailable());
        if (selectedIds.size === 0) {
            return;
        }
        const roles = this.#$allRoles();
        // `selectedAvailable` only ever holds grantable-leaf ids
        // (see toggleAvailableSelection); the isGrantableLeaf check
        // is kept as defence-in-depth in case anything slipped in
        // between renders (e.g. tree refetch demoted a role).
        const keysToAdd = roles
            .filter((role) => selectedIds.has(role.id) && this.isGrantableLeaf(role))
            .map((role) => this.grantIdentifier(role));
        this.$granted.update((current) => Array.from(new Set([...current, ...keysToAdd])));
        this.$selectedAvailable.set([]);
        this.grantedChange.emit(this.$granted());
    }

    protected revoke(): void {
        const toRemove = new Set(this.$selectedGranted());
        if (toRemove.size === 0) {
            return;
        }
        this.$granted.update((current) => current.filter((key) => !toRemove.has(key)));
        this.$selectedGranted.set([]);
        this.grantedChange.emit(this.$granted());
    }

    protected isSelectedGranted(key: string): boolean {
        return this.$selectedGranted().includes(key);
    }

    private loadRoles(): void {
        this.$isLoading.set(true);
        this.#usersService
            .getAllRoles()
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe({
                next: (roles) => {
                    // Keep every role in the display tree — some
                    // organizational parents (e.g. `Categories`,
                    // `Publisher / Legal`) don't have a roleKey but
                    // still need to render so their children have a
                    // parent node. Roles without a key are just not
                    // grantable (see the `grant()` filter below).
                    this.#$allRoles.set(roles.map((role) => toRoleOption(role)));
                    this.$isLoading.set(false);
                },
                error: (error) => {
                    this.#httpErrorManager.handle(error);
                    this.$isLoading.set(false);
                }
            });
    }
}

function toRoleOption(role: DotRoleView): RoleOption {
    return {
        id: role.id,
        roleKey: (role.roleKey ?? '').trim(),
        name: role.name ?? '(unnamed role)',
        description: role.description ?? '',
        parent: role.parent && role.parent.length > 0 ? role.parent : undefined,
        // Backend returns `editUsers` on every role; treat missing as
        // permissive (true) so we don't accidentally hide grantable
        // roles when the API shape shifts.
        editUsers: role.editUsers !== false
    };
}
