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

import { TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TreeModule } from 'primeng/tree';

import { switchMap, take } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotRolesService
} from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { flattenRoleHierarchy } from '../../../utils/dot-roles-hierarchy.utils';

/**
 * Internal shape the shuttle renders. Keeps the visual code decoupled
 * from `DotRole` so the tree walker/filter logic never has to
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
 * save payload — this component takes the initial granted role IDs
 * as an input and emits every change (also as IDs) through
 * `grantedChange`. Since #37218 the backend accepts both `roleKey`
 * and `id` on the users endpoint, so we send IDs unconditionally —
 * keyless custom roles that used to be dropped now round-trip
 * cleanly.
 */
@Component({
    selector: 'dot-users-roles-tab',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        TreeModule,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-users-roles-tab.component.html',
    styleUrl: './dot-users-roles-tab.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col gap-4' }
})
export class DotUsersRolesTabComponent {
    readonly #rolesService = inject(DotRolesService);
    readonly #messageService = inject(DotMessageService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Empty-state config for the granted panel. Uses the shared
     * `<dot-empty-container>` so the panel matches the app-wide empty
     * pattern (icon + title, centred) instead of a lone paragraph.
     * `security` material symbol reads as "roles/permissions" and
     * matches the tab's domain. Contact-us link stays off — this
     * empty state is a hint for the admin on the current dialog, not
     * a marketing surface.
     */
    protected readonly grantedEmptyConfig: PrincipalConfiguration = {
        title: this.#messageService.get('users.dialog.roles.granted.empty'),
        icon: 'shield_person',
        iconStyle: 'material-symbols-rounded'
    };

    /**
     * Role IDs the user currently holds — sourced from the parent
     * dialog's `getUserRoles` fetch. `granted` is seeded from this
     * value on the first non-empty change; subsequent parent
     * mutations don't clobber in-flight user edits.
     */
    readonly initialGrantedIds = input<string[]>([]);

    /**
     * Emits the full set of currently granted role IDs every time
     * the user grants or revokes anything. The shell listens and
     * plugs the value into the save payload.
     */
    readonly grantedChange = output<string[]>();

    readonly #$allRoles = signal<RoleOption[]>([]);
    readonly #$rolesById = computed(() => {
        const map = new Map<string, RoleOption>();
        for (const role of this.#$allRoles()) {
            map.set(role.id, role);
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
     */
    private isGrantableLeaf(role: RoleOption): boolean {
        if (this.#$rolesWithChildren().has(role.id)) {
            return false;
        }

        return role.editUsers !== false;
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
        const grantedIds = new Set(this.$granted());
        const grantableLeaves = this.#$grantableLeavesByRole();

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

            return leaves.some((leafId) => !grantedIds.has(leafId));
        };

        const pool = this.#$allRoles().filter((role) => {
            if (this.#$rolesWithChildren().has(role.id)) {
                return hasRemainingGrantable(role.id);
            }

            // Leaves are moved between panels: keep only when not yet
            // granted.
            return !grantedIds.has(role.id);
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

    /**
     * PrimeNG-shaped mirror of `$availableTree`. Rebuilt whenever the
     * pruned tree changes (filter typed, roles granted / revoked) so
     * `<p-tree>` stays declarative. Non-grantable rows (workflow-only
     * branches or `editUsers=false` leaves) get `selectable=false` so
     * the checkbox column stays blank on them — same rule the
     * hand-rolled tree enforced via `canSelectRole`.
     *
     * The expanded flag is seeded from `$collapsed` so a filter that
     * hides + restores a branch preserves the user's open/closed
     * choice across renders.
     */
    protected readonly $availableTreeNodes = computed<TreeNode[]>(() =>
        this.buildTreeNodes(this.$availableTree())
    );

    /**
     * PrimeNG selection is stored as a TreeNode array. We keep it as
     * the UI source of truth for the `<p-tree>` binding and derive
     * `$selectedAvailable` (the leaf-id source used by `grant()`) on
     * every change. Tests still reach `$selectedAvailable` directly so
     * the isFullyChecked / isPartiallyChecked assertions keep working
     * without any tree wiring.
     */
    protected readonly $selectedTreeNodes = signal<TreeNode[]>([]);

    protected readonly $grantedList = computed<RoleOption[]>(() => {
        const query = this.$grantedFilter().toLowerCase().trim();
        const map = this.#$rolesById();
        const list: RoleOption[] = [];
        for (const id of this.$granted()) {
            const role = map.get(id);
            if (role && (!query || role.name.toLowerCase().includes(query))) {
                list.push(role);
            }
        }

        return list;
    });

    /**
     * Flat forest of granted roles for `<p-tree>`. Same shape as the
     * available side (no children) so both panels render with identical
     * PrimeNG chrome — matching checkboxes, row padding, hover and
     * selection colours — instead of a hand-rolled row list that never
     * quite catches up with the theme.
     */
    protected readonly $grantedTreeNodes = computed<TreeNode[]>(() =>
        this.$grantedList().map((role) => ({
            key: role.id,
            label: role.name,
            data: role,
            selectable: true
        }))
    );

    /**
     * PrimeNG selection binding for the granted tree. Reconstructed
     * from `$selectedGranted` (id source of truth) whenever either the
     * selection or the visible node set changes — this lets `revoke()`
     * clear the visible checkboxes just by clearing the id set.
     */
    protected readonly $selectedGrantedTreeNodes = computed<TreeNode[]>(() => {
        const selected = new Set(this.$selectedGranted());

        return this.$grantedTreeNodes().filter((node) => selected.has(node.key ?? ''));
    });

    protected readonly $grantedCount = computed(() => this.$granted().length);
    protected readonly $canGrant = computed(() => this.$selectedAvailable().length > 0);
    protected readonly $canRevoke = computed(() => this.$selectedGranted().length > 0);

    constructor() {
        this.loadRoles();

        // Seed the local `granted` signal from the parent's
        // `initialGrantedIds` exactly once, the first time it delivers
        // a non-empty value. Later parent mutations don't clobber
        // in-flight user edits.
        let seeded = false;
        effect(() => {
            const ids = this.initialGrantedIds();
            if (seeded) {
                return;
            }
            if (ids.length > 0) {
                this.$granted.set([...ids]);
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

    /**
     * Records a node's expand/collapse state from `<p-tree>` events so
     * a filter that hides + restores a branch preserves what the user
     * had open. p-tree emits both `onNodeExpand` and `onNodeCollapse`.
     */
    protected onNodeExpand(event: { node: TreeNode }): void {
        const key = event.node.key;
        if (key) {
            this.$collapsed.update((state) => ({ ...state, [key]: false }));
        }
    }

    protected onNodeCollapse(event: { node: TreeNode }): void {
        const key = event.node.key;
        if (key) {
            this.$collapsed.update((state) => ({ ...state, [key]: true }));
        }
    }

    /**
     * PrimeNG `(selectionChange)` handler for the granted-side tree.
     * The granted forest is flat (no parents) so every emitted node is
     * a leaf — dropping any node with a nullish key is defensive; in
     * practice `$grantedTreeNodes` always sets one.
     */
    protected onGrantedTreeSelectionChange(selection: TreeNode | TreeNode[] | null): void {
        const nodes = Array.isArray(selection) ? selection : selection ? [selection] : [];
        this.$selectedGranted.set(nodes.map((node) => node.key ?? '').filter((key) => !!key));
    }

    /**
     * PrimeNG `(selectionChange)` handler. Stores the raw TreeNode
     * array for the `<p-tree>` binding and derives the leaf-id list
     * for `grant()`. A parent whose children are only partially
     * checked still appears in the array with `partialSelected=true`
     * — filtering by `isGrantableLeaf` drops those non-leaf entries
     * cleanly.
     */
    protected onTreeSelectionChange(selection: TreeNode | TreeNode[] | null): void {
        const nodes = Array.isArray(selection) ? selection : selection ? [selection] : [];
        this.$selectedTreeNodes.set(nodes);
        const leafIds = nodes
            .filter((node) => !!node.data && this.isGrantableLeaf(node.data as RoleOption))
            .map((node) => (node.data as RoleOption).id);
        this.$selectedAvailable.set(leafIds);
    }

    protected grant(): void {
        const selectedIds = new Set(this.$selectedAvailable());
        if (selectedIds.size === 0) {
            return;
        }
        const roles = this.#$allRoles();
        // `selectedAvailable` only ever holds grantable-leaf ids
        // (see onTreeSelectionChange); the isGrantableLeaf check
        // is kept as defence-in-depth in case anything slipped in
        // between renders (e.g. tree refetch demoted a role).
        const idsToAdd = roles
            .filter((role) => selectedIds.has(role.id) && this.isGrantableLeaf(role))
            .map((role) => role.id);
        this.$granted.update((current) => Array.from(new Set([...current, ...idsToAdd])));
        this.$selectedAvailable.set([]);
        this.$selectedTreeNodes.set([]);
        this.grantedChange.emit(this.$granted());
    }

    protected revoke(): void {
        const toRemove = new Set(this.$selectedGranted());
        if (toRemove.size === 0) {
            return;
        }
        this.$granted.update((current) => current.filter((id) => !toRemove.has(id)));
        this.$selectedGranted.set([]);
        this.grantedChange.emit(this.$granted());
    }

    /**
     * Walks the pruned `$availableTree` and produces the TreeNode
     * array `<p-tree>` consumes. Deliberately no icon on any node —
     * icons were dropped per the design pass.
     */
    private buildTreeNodes(nodes: RoleTreeNode[]): TreeNode[] {
        return nodes.map((node) => ({
            key: node.role.id,
            label: node.role.name,
            data: node.role,
            selectable: this.canSelectRole(node.role),
            expanded: !this.$collapsed()[node.role.id],
            children: this.buildTreeNodes(node.children)
        }));
    }

    private loadRoles(): void {
        this.$isLoading.set(true);
        this.#rolesService
            .getRoots(true)
            .pipe(
                switchMap((roots) =>
                    flattenRoleHierarchy(roots, (id) => this.#rolesService.getById(id, true))
                ),
                take(1),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe({
                next: (roles) => {
                    // Keep every role in the display tree — some
                    // organizational parents (e.g. `Categories`,
                    // `Publisher / Legal`) don't have a roleKey but
                    // still need to render so their children have a
                    // parent node. Roles without a key are just not
                    // grantable when the backend refuses via
                    // `editUsers=false` (see the `grant()` filter).
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

function toRoleOption(role: DotRole): RoleOption {
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
