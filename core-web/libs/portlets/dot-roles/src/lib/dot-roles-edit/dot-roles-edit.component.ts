import { Subject } from 'rxjs';

import { Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ConfirmationService, TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import { DotRoleDetail, DotRoleFormValue, DotRoleNode } from '../models/dot-roles.models';

/**
 * Edit Role dialog. Wired to PUT /v1/roles/{roleId} via the store (#36936).
 *
 * System and locked roles are read-only per the backend gate: the form is
 * still shown but Save is disabled with a tooltip explaining why. The
 * `Delete` action is deferred to task #36939 wiring (a follow-up PR).
 *
 * The parent picker is filtered so the currently-edited role and its
 * descendants aren't selectable — the backend would reject a cycle with 400,
 * but we surface the constraint visually.
 */
@Component({
    selector: 'dot-roles-edit',
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        CheckboxModule,
        TreeSelectModule,
        TooltipModule,
        ConfirmDialogModule,
        DotMessagePipe,
        DotFieldRequiredDirective,
        DotFieldValidationMessageComponent
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-roles-edit.component.html'
})
export class DotRolesEditComponent {
    readonly #store = inject(DotRolesStore);
    readonly #fb = inject(FormBuilder);
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Deep-search results, scoped to this dialog — see the Add dialog for why
     * this is not routed through the store's shared `searchResults`.
     */
    readonly #searchResults = signal<DotRoleNode[] | null>(null);

    /**
     * Open branches. PrimeNG records expansion by mutating `node.expanded`,
     * but our options come from a `computed` that hands it new objects on
     * every store change — see the Add dialog for the full rationale.
     */
    readonly #expandedKeys = signal(new Set<string>());

    // `protected`, not `#`: Angular rejects `viewChild` on an ES-private field.
    protected readonly treeSelect = viewChild(TreeSelect);
    protected readonly $searching = signal(false);
    readonly #filterInput$ = new Subject<string>();

    /**
     * Guards against an older search overwriting a newer one. The debounce
     * gates how often a search STARTS, not the order responses come back, and
     * these calls are plain promises with no switchMap to cancel the loser.
     */
    #searchToken = 0;

    protected readonly role: DotRoleDetail = this.#config.data?.role;

    protected readonly $submitting = signal(false);
    protected readonly $error = signal<string | null>(null);

    protected readonly readOnly = this.role?.system === true || this.role?.locked === true;

    protected readonly form = this.#fb.nonNullable.group({
        roleName: [this.role?.name ?? '', Validators.required],
        roleKey: [this.role?.roleKey ?? ''],
        parent: [null as TreeNode | null],
        canEditUsers: [this.role?.editUsers ?? true],
        canEditPermissions: [this.role?.editPermissions ?? true],
        canEditLayouts: [this.role?.editLayouts ?? true],
        description: [this.role?.description ?? '']
    });

    /**
     * Parent candidates as a tree. The role itself and every descendant are
     * excluded — reparenting under your own subtree is a cycle, which the BE
     * rejects with a 400 anyway.
     */
    protected readonly $parentTree = computed<TreeNode[]>(() => {
        // Defensive: the dialog can theoretically be opened without
        // `data.role` (misconfigured caller) — bail out with an empty tree
        // instead of crashing on `this.role.id`.
        if (!this.role) {
            return [];
        }
        const cached = this.#store.roleTree();
        const searchResults = this.#searchResults();
        const source = searchResults ?? cached;

        // Collect descendants from BOTH datasets, not just the one being
        // rendered. The cached tree holds only the branches that were expanded,
        // while search results carry full ancestor paths from a different
        // endpoint — so each can contain a descendant the other is missing.
        // Excluding from only one lets the search path offer a real descendant
        // as the new parent, which is the cycle this guard exists to prevent.
        const exclude = new Set<string>();
        this.#collectDescendantIds(this.#findInTree(cached, this.role.id), exclude);
        if (searchResults) {
            this.#collectDescendantIds(this.#findInTree(searchResults, this.role.id), exclude);
        }
        exclude.add(this.role.id);

        return this.#toTreeNodes(source, exclude, this.#expandedKeys());
    });

    constructor() {
        this.#filterInput$
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((query) => this.#runSearch(query));

        // Seed the picker from the role's current parent. A root role has a
        // self-referential `parent`, which `#normalizeParentId` maps to null.
        const currentParentId = this.#normalizeParentId(this.role);
        if (currentParentId) {
            // The fallback is what stops a silent reparent: a role reached
            // through the page's search can have its parent chain missing from
            // the cached tree, and leaving the picker empty would send
            // `parentRoleId: null` — moving the role to root on a Save the
            // admin only opened to rename it.
            this.form.controls.parent.setValue(
                this.#findNode(this.$parentTree(), currentParentId) ?? {
                    key: currentParentId,
                    label: this.#parentLabel(currentParentId)
                }
            );
        }

        if (this.readOnly) {
            this.form.disable();
        }
    }

    protected onSave(): void {
        if (this.form.invalid || this.$submitting() || this.readOnly) {
            return;
        }

        this.$submitting.set(true);
        this.$error.set(null);

        const { parent, ...rest } = this.form.getRawValue();
        // An empty picker means the role becomes a root.
        const value: DotRoleFormValue = {
            ...rest,
            parentRoleId: (parent?.key as string | undefined) ?? null
        };

        this.#store.updateRole(this.role.id, value).then((updated) => {
            this.$submitting.set(false);
            if (updated) {
                this.#ref.close(updated);
            } else {
                this.$error.set('roles.edit.error');
            }
        });
    }

    /**
     * The backend hydrates only two levels per request, so hydrate on expand
     * and let the admin drill as deep as the hierarchy goes.
     */
    protected onNodeExpand(event: { node: TreeNode }): void {
        const key = event.node?.key;
        if (!key) {
            return;
        }
        this.#expandedKeys.update((keys) => new Set(keys).add(key));
        this.#store.loadRoleChildren(key);
    }

    protected onNodeCollapse(event: { node: TreeNode }): void {
        const key = event.node?.key;
        if (!key) {
            return;
        }
        this.#expandedKeys.update((keys) => {
            const next = new Set(keys);
            next.delete(key);

            return next;
        });
    }

    protected onFilter(event: { filter: string }): void {
        this.#filterInput$.next(event.filter ?? '');
    }

    async #runSearch(query: string): Promise<void> {
        const token = ++this.#searchToken;

        if (query.trim().length < 3) {
            this.#searchResults.set(null);
            // Reset the widget's own filter too. `Tree.getRootNode()` keeps
            // serving its cached `filteredNodes` once the filter has run, so
            // reverting the options alone would leave the last search's rows
            // on screen after the admin backspaces out of the query.
            this.treeSelect()?.treeViewChild?._filter('');

            return;
        }

        this.$searching.set(true);
        const results = await this.#store.searchRoleTree(query);

        // A newer query already landed — drop this one rather than replacing
        // fresh results with stale ones.
        if (token !== this.#searchToken) {
            return;
        }

        this.#searchResults.set(results);
        this.$searching.set(false);

        // `Tree.getRootNode()` returns its cached `filteredNodes` once the
        // client filter has run, ignoring `value` — so the new options only
        // render if the filter is re-applied over them.
        this.treeSelect()?.treeViewChild?._filter(query);
    }

    protected onCancel(): void {
        this.#ref.close();
    }

    /**
     * Delete flow: confirms first, then delegates to the store. The BE
     * cascades, so the confirm copy names the blast radius when we already
     * know it (the store may enrich it via `usersAffected` post-delete via a
     * follow-up toast, but the confirm itself is intentionally conservative
     * — always call out that the deletion is permanent).
     *
     * System/locked roles never reach this button; it's disabled at the
     * template level. The BE also rejects them with 403.
     */
    protected onDelete(): void {
        if (this.readOnly) {
            return;
        }

        this.#confirmationService.confirm({
            message: this.#messageService.get('roles.confirm.delete.message', this.role.name),
            header: this.#messageService.get('roles.confirm.delete.header'),
            // Plain "Delete" (not "Delete Role") — the header already names the object.
            acceptLabel: this.#messageService.get('roles.action.delete'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            // Default (primary) styling — no red — per UX guidance for this
            // confirm even though the action is destructive.
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => {
                this.$submitting.set(true);
                this.#store.deleteRole(this.role.id).then((result) => {
                    this.$submitting.set(false);
                    if (result?.deleted) {
                        this.#ref.close({ deleted: true, ...result });
                    } else if (result && result.deleted === false) {
                        // Server-side rejection with a 200 (e.g. hierarchy
                        // constraint) — no toast was fired, surface the
                        // inline banner as the only feedback.
                        this.$error.set('roles.delete.error');
                    }
                    // result === null → HTTP error already surfaced by
                    // `DotHttpErrorManagerService.handle` inside the store.
                    // Don't double up.
                });
            }
        });
    }

    /**
     * Roots come back from the BE with `parent === id` (self-referential).
     * Normalize to `null` so the select shows "None (top level)".
     */
    #normalizeParentId(role: DotRoleDetail | undefined): string | null {
        if (!role) {
            return null;
        }
        if (!role.parent || role.parent === role.id) {
            return null;
        }

        return role.parent;
    }

    #toTreeNodes(
        nodes: DotRoleNode[],
        exclude: Set<string>,
        expandedKeys: Set<string>
    ): TreeNode[] {
        return nodes.reduce<TreeNode[]>((acc, node) => {
            if (exclude.has(node.id)) {
                return acc;
            }
            acc.push({
                key: node.id,
                label: node.name,
                expanded: expandedKeys.has(node.id),
                // `leaf: false` gives PrimeNG a toggler for a node whose
                // children have not been fetched yet — see the Add dialog.
                leaf:
                    node.childCount !== undefined
                        ? node.childCount === 0
                        : (node.roleChildren?.length ?? 0) === 0,
                children: this.#toTreeNodes(node.roleChildren ?? [], exclude, expandedKeys)
            });

            return acc;
        }, []);
    }

    /** Best-effort display name for a parent that is not in the loaded tree. */
    #parentLabel(parentId: string): string {
        return this.#findInTree(this.#store.roleTree(), parentId)?.name ?? parentId;
    }

    #findNode(nodes: TreeNode[], key: string): TreeNode | null {
        for (const node of nodes) {
            if (node.key === key) {
                return node;
            }
            const found = this.#findNode(node.children ?? [], key);
            if (found) {
                return found;
            }
        }

        return null;
    }

    #findInTree(nodes: DotRoleNode[], id: string): DotRoleNode | null {
        for (const node of nodes) {
            if (node.id === id) {
                return node;
            }
            if (node.roleChildren?.length) {
                const found = this.#findInTree(node.roleChildren, id);
                if (found) {
                    return found;
                }
            }
        }

        return null;
    }

    #collectDescendantIds(node: DotRoleNode | null, into: Set<string>): void {
        if (!node?.roleChildren?.length) {
            return;
        }
        for (const child of node.roleChildren) {
            into.add(child.id);
            this.#collectDescendantIds(child, into);
        }
    }
}
