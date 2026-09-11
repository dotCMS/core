import { Subject } from 'rxjs';

import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem, TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenuModule } from 'primeng/contextmenu';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import {
    TreeNodeCollapseEvent,
    TreeNodeContextMenuSelectEvent,
    TreeNodeExpandEvent,
    TreeNodeSelectEvent
} from 'primeng/types/tree';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotFolderTreeComponent, DotMessagePipe, DotTruncatedLabelComponent } from '@dotcms/ui';

import { DotRolesAddComponent } from '../../../dot-roles-add/dot-roles-add.component';
import { DotRolesEditComponent } from '../../../dot-roles-edit/dot-roles-edit.component';
import { DotRoleNode } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';
import { collectAncestorChain } from '../../store/dot-roles.tree-utils';

interface DotRolePrimeTreeNode extends TreeNode {
    data: DotRoleNode;
    key: string;
    label: string;
    children?: DotRolePrimeTreeNode[];
}

@Component({
    selector: 'dot-roles-tree',
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        DynamicDialogModule,
        ContextMenuModule,
        ConfirmDialogModule,
        DotFolderTreeComponent,
        DotTruncatedLabelComponent,
        DotMessagePipe
    ],
    providers: [DialogService, ConfirmationService],
    templateUrl: './dot-roles-tree.component.html',
    host: { class: 'flex flex-col flex-1 min-h-0 p-4 gap-3' }
})
export class DotRolesTreeComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #alertService = inject(DotAlertConfirmService);
    readonly #filterInput$ = new Subject<string>();

    /**
     * Node targeted by the most recent right-click. Kept in a signal so the
     * context-menu commands can act on it — PrimeNG's `MenuItem.command`
     * fires without an event argument, so it needs a shared reference.
     */
    readonly #contextNode = signal<DotRolePrimeTreeNode | null>(null);

    /**
     * Tree context menu: Edit + separator + Delete, no icons, per design.
     * `command` reads from `#contextNode` set by `onNodeContextMenuSelect`.
     * System / locked roles get a disabled menu — backend rejects them
     * anyway (403), so we surface the constraint at click time.
     */
    protected readonly $contextMenuItems = computed<MenuItem[]>(() => {
        const node = this.#contextNode()?.data;
        const isReadOnly = node?.system === true || node?.locked === true;

        return [
            {
                label: this.#messageService.get('roles.action.edit'),
                disabled: isReadOnly,
                command: () => this.#editContextNode()
            },
            { separator: true },
            {
                label: this.#messageService.get('roles.action.delete'),
                disabled: isReadOnly,
                command: () => this.#deleteContextNode()
            }
        ];
    });

    /**
     * Nodes that have been opened at least once — used for lazy-load
     * dedup (skip refetch on re-open) and leaf detection (a node that
     * has been opened but has zero children is a confirmed leaf).
     * Add-only: never removed on collapse, so a re-open of a
     * previously-loaded branch does NOT hit the backend again.
     */
    readonly #fetchedRoleIds = signal(new Set<string>());

    /**
     * Nodes currently open in the UI. Reflects live expand / collapse
     * events. Separate from `#fetchedRoleIds` so collapsing a node
     * doesn't invalidate the "we've already loaded these children"
     * signal. Empty on first load → tree renders fully collapsed.
     */
    readonly #openNodeIds = signal(new Set<string>());

    // The label's own layout — clipping, and the `flex-1` that keeps the `+` button at the
    // trailing edge — belongs to `dot-truncated-label` and the shared tree's stylesheet now
    // (#37363). That also fixes the depth problem this comment used to describe: `pt` only
    // reaches the top-level tree, so nested labels never received these rules.
    protected readonly treePt = {
        root: { class: 'w-full h-full border-none overflow-y-auto [--p-tree-padding:0]' }
    };

    protected readonly $filterInput = computed(() => this.store.filter());

    /**
     * Auto-expand every branch when the store is showing search results
     * so the match at the leaf level is visible without the admin
     * clicking through ancestors. When no search is active the `expanded`
     * state falls back to `#openNodeIds` (user's own open/close history).
     */
    protected readonly $treeNodes = computed<DotRolePrimeTreeNode[]>(() =>
        this.#toTreeNodes(this.store.filteredRoles(), this.store.isSearching())
    );

    protected readonly $selectedNode = computed<DotRolePrimeTreeNode | null>(() => {
        const id = this.store.selectedRoleId();
        if (!id) {
            return null;
        }

        return this.#findNode(this.$treeNodes(), id);
    });

    /**
     * Ancestor path of the selected role, as a comparable key.
     *
     * Keyed on the path rather than the tree so it changes when the role
     * MOVES and not merely when `roles` is rewritten — a member load patches
     * `userCount` into the tree on every selection, and reacting to that would
     * re-open branches the admin had just collapsed.
     */
    readonly #selectedAncestorPath = computed(() => {
        const id = this.store.selectedRoleId();
        if (!id) {
            return '';
        }

        return collectAncestorChain(this.store.roleTree(), { id })
            .map((node) => node.id)
            .join('/');
    });

    /** Path last opened by the reveal effect, so it only acts on a change. */
    #revealedPath = '';

    constructor() {
        this.#filterInput$
            .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setFilter(value));

        // Reparenting from the Edit dialog keeps the role selected but moves it
        // under a parent that is very often collapsed — the admin saves and the
        // role they were just editing disappears from the tree. Open the new
        // path so it stays where they can see it.
        //
        // Only the ancestors: expanding the role itself would unfold a branch
        // the admin never asked to open.
        effect(() => {
            const path = this.#selectedAncestorPath();
            if (!path || path === this.#revealedPath) {
                return;
            }
            this.#revealedPath = path;

            const ancestorIds = path.split('/').slice(1);
            if (ancestorIds.length === 0) {
                return;
            }

            untracked(() =>
                this.#openNodeIds.update((set) => {
                    const next = new Set(set);
                    ancestorIds.forEach((id) => next.add(id));

                    return next;
                })
            );
        });
    }

    protected onFilterChange(value: string): void {
        this.#filterInput$.next(value);
    }

    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        this.store.selectRole(node.data.id);
    }

    /**
     * PrimeNG lazy-load hook. The backend only returns 2 levels deep per
     * request, so any node whose children we haven't fetched yet triggers
     * a load on first expand.
     */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        const id = node.data.id;

        this.#openNodeIds.update((set) => new Set(set).add(id));

        const loadedChildren = node.data.roleChildren?.length ?? 0;

        // A branch can lose children it had already fetched — a reparent
        // replaces the moved role with a response that hydrates two levels at
        // most, and a lazy-load that failed marked the node fetched before it
        // resolved. The marker is add-only, so on its own it leaves that branch
        // permanently empty: expanded, showing a chevron, and rendering
        // nothing. `childCount` is independent of hydration and still says the
        // children are there, so it is what re-opens the gate.
        const lostChildren = loadedChildren === 0 && (node.data.childCount ?? 0) > 0;

        if (this.#fetchedRoleIds().has(id) && !lostChildren) {
            return;
        }

        this.#fetchedRoleIds.update((set) => new Set(set).add(id));

        // Only fetch when we don't already have children populated.
        if (loadedChildren === 0 && !node.data.user) {
            this.store.loadRoleChildren(id);
        }
    }

    protected onNodeCollapse(event: TreeNodeCollapseEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        this.#openNodeIds.update((set) => {
            const next = new Set(set);
            next.delete(node.data.id);

            return next;
        });
    }

    protected onAddRole(parentRoleId?: string): void {
        const ref = this.#dialogService.open(DotRolesAddComponent, {
            header: this.#messageService.get('roles.add.title'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { parentRoleId: parentRoleId ?? null }
        });

        ref.onClose
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((created?: DotRoleNode) => {
                if (created) {
                    this.#revealCreatedRole(created);
                }
            });
    }

    /**
     * Open the branch the new role landed in.
     *
     * The store already selects it, and `$selectedNode` follows — but selection
     * is invisible inside a collapsed parent, so the admin gets no feedback that
     * anything happened.
     */
    #revealCreatedRole(created: DotRoleNode): void {
        // A self-referential `parent` marks a root; those are visible already.
        const parentId = created.parent && created.parent !== created.id ? created.parent : null;
        if (!parentId) {
            return;
        }

        // Expand the whole ancestor path, not just the immediate parent — that
        // parent may itself sit inside a collapsed branch.
        const chain = collectAncestorChain(this.store.roleTree(), { id: parentId });
        this.#openNodeIds.update((set) => {
            const next = new Set(set);
            chain.forEach((node) => next.add(node.id));

            return next;
        });

        // If this parent's children were never fetched, the tree holds only the
        // role we just spliced in — expanding would show it alone and hide its
        // siblings, which reads as "the others were deleted". Fetch the real
        // set; the backend already has the new role, so nothing is lost.
        if (!this.#fetchedRoleIds().has(parentId)) {
            this.#fetchedRoleIds.update((set) => new Set(set).add(parentId));
            this.store.loadRoleChildren(parentId);
        }
    }

    /**
     * Bound to `<dot-folder-tree>`'s `onNodeContextMenuSelect`. Selects
     * the node in the store (so the detail area follows the right-click
     * target) and stashes the tree-node reference for the menu commands.
     */
    protected onNodeContextMenu(event: TreeNodeContextMenuSelectEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        this.#contextNode.set(node);
        this.store.selectRole(node.data.id);
    }

    // Fetch the full detail before opening — under active search the tree
    // node is a thin `{id, name, locked}` and PUT is a full replace, so
    // opening the dialog with the partial node would silently wipe
    // `parent / roleKey / description / editUsers / editPermissions /
    // editLayouts` on save.
    async #editContextNode(): Promise<void> {
        const node = this.#contextNode();
        if (!node) {
            return;
        }

        const detail = await this.store.fetchRoleDetail(node.data.id);
        if (!detail) {
            return;
        }

        this.#dialogService.open(DotRolesEditComponent, {
            header: this.#messageService.get('roles.edit.title'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { role: detail }
        });
    }

    #deleteContextNode(): void {
        const node = this.#contextNode();
        if (!node) {
            return;
        }

        this.#confirmationService.confirm({
            message: this.#messageService.get('roles.confirm.delete.message', node.data.name),
            header: this.#messageService.get('roles.confirm.delete.header'),
            acceptLabel: this.#messageService.get('roles.action.delete'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: async () => {
                const result = await this.store.deleteRole(node.data.id);
                // `result === null` → HTTP error already surfaced by the
                // shared error manager. `result.deleted === false` → the BE
                // accepted the request but refused to delete (hierarchy
                // constraint, workflow reference); surface an alert so the
                // user gets feedback instead of a silent no-op.
                if (result && result.deleted === false) {
                    this.#alertService.alert({
                        header: this.#messageService.get('roles.confirm.delete.header'),
                        message: this.#messageService.get('roles.delete.rejected')
                    });
                }
            }
        });
    }

    #toTreeNodes(nodes: DotRoleNode[], expandAll = false): DotRolePrimeTreeNode[] {
        return nodes.map((node) => {
            const children = this.#toTreeNodes(node.roleChildren ?? [], expandAll);
            const hasChildren = children.length > 0;
            // `childCount` (#37071) is authoritative and independent of whether
            // `roleChildren` was hydrated, so leaf-vs-chevron is correct at every
            // depth on first paint — no more chevrons that expand into nothing.
            //
            // Legacy search nodes (`/api/role/loadbyname`) don't carry it, so
            // `undefined` falls back to the old heuristic: a leaf is confirmed
            // only once we've fetched the children and got none back.
            const confirmedLeaf =
                node.user === true ||
                (node.childCount !== undefined
                    ? node.childCount === 0
                    : this.#fetchedRoleIds().has(node.id) && !hasChildren);

            return {
                key: node.id,
                label: node.name,
                data: node,
                children,
                leaf: confirmedLeaf,
                expanded: expandAll || this.#openNodeIds().has(node.id)
            };
        });
    }

    #findNode(nodes: DotRolePrimeTreeNode[], id: string): DotRolePrimeTreeNode | null {
        for (const node of nodes) {
            if (node.data.id === id) {
                return node;
            }
            const found = node.children ? this.#findNode(node.children, id) : null;
            if (found) {
                return found;
            }
        }

        return null;
    }
}
